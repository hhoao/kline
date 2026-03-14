package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;
import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertSet;
import static com.hhoa.kline.web.common.pojo.CommonResult.success;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.CHAT_MESSAGE_NOT_EXIST;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.hhoa.ai.kline.commons.exception.ServiceException;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.core.RequestContext;
import com.hhoa.kline.web.dal.AiChatMessageMapper;
import com.hhoa.kline.web.dal.dataobject.AiChatConversationDO;
import com.hhoa.kline.web.dal.dataobject.AiChatMessageDO;
import com.hhoa.kline.web.dal.dataobject.AiChatRoleDO;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dal.dataobject.AiModelDO;
import com.hhoa.kline.web.dal.dataobject.AiToolDO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessagePageReqVO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessageRespVO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessageSendReqVO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessageSendRespVO;
import com.hhoa.kline.web.enums.AIErrorCodeConstants;
import com.hhoa.kline.web.enums.AiPlatformEnum;
import com.hhoa.kline.web.service.AiChatConversationService;
import com.hhoa.kline.web.service.AiChatMessageService;
import com.hhoa.kline.web.service.AiChatRoleService;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiKnowledgeSegmentService;
import com.hhoa.kline.web.service.AiModelService;
import com.hhoa.kline.web.service.AiToolService;
import com.hhoa.kline.web.service.bo.AiKnowledgeSegmentSearchReqBO;
import com.hhoa.kline.web.service.bo.AiKnowledgeSegmentSearchRespBO;
import com.hhoa.kline.web.utils.AiUtils;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

/**
 * AI 聊天消息 Service 实现类
 *
 * @author hhoa
 */
@Service
@Slf4j
@AllArgsConstructor
public class AiChatMessageServiceImpl implements AiChatMessageService {
    /** 知识库转 {@link UserMessage} 的内容模版 */
    private static final String KNOWLEDGE_USER_MESSAGE_TEMPLATE =
            "使用 <Reference></Reference> 标记中的内容作为本次对话的参考:\n\n"
                    + "%s\n\n"
                    + // 多个 <Reference></Reference> 的拼接
                    "回答要求：\n- 避免提及你是从 <Reference></Reference> 获取的知识。";

    @Resource private AiChatMessageMapper chatMessageMapper;
    @Resource private AiChatConversationService chatConversationService;
    @Resource private AiChatRoleService chatRoleService;
    @Resource private AiModelService modalService;
    @Resource private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource private AiToolService toolService;
    @Autowired private ToolCallingManager toolCallingManager;

    @Transactional(rollbackFor = Exception.class)
    public AiChatMessageSendRespVO sendMessage(AiChatMessageSendReqVO sendReqVO, Long userId) {
        // 1.1 校验对话存在
        AiChatConversationDO conversation =
                chatConversationService.validateChatConversationExists(
                        sendReqVO.getConversationId());
        if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        List<AiChatMessageDO> historyMessages =
                chatMessageMapper.selectList(
                        new LambdaQueryWrapperX<AiChatMessageDO>()
                                .eq(AiChatMessageDO::getConversationId, conversation.getId())
                                .orderByAsc(AiChatMessageDO::getId));
        // 1.2 校验模型
        AiModelDO model = modalService.validateModel(conversation.getModelId());
        ChatModel chatModel = modalService.getChatModel(model.getId());

        // 2. 知识库找回
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments =
                recallKnowledgeSegment(sendReqVO.getContent(), conversation);

        // 3. 插入 user 发送消息
        AiChatMessageDO userMessage =
                createChatMessage(
                        conversation.getId(),
                        null,
                        model.getModel(),
                        model.getId(),
                        userId,
                        conversation.getRoleId(),
                        MessageType.USER,
                        sendReqVO.getContent(),
                        sendReqVO.getUseContext(),
                        null);

        // 3.1 插入 assistant 接收消息
        AiChatMessageDO assistantMessage =
                createChatMessage(
                        conversation.getId(),
                        userMessage.getId(),
                        model.getModel(),
                        model.getId(),
                        userId,
                        conversation.getRoleId(),
                        MessageType.ASSISTANT,
                        "",
                        sendReqVO.getUseContext(),
                        knowledgeSegments);

        // 3.2 创建 chat 需要的 Prompt
        Prompt prompt =
                buildPrompt(conversation, historyMessages, knowledgeSegments, model, sendReqVO);
        ChatResponse chatResponse = chatModel.call(prompt);

        // 3.3 更新响应内容
        String newContent = chatResponse.getResult().getOutput().getText();
        chatMessageMapper.updateById(
                new AiChatMessageDO().setId(assistantMessage.getId()).setContent(newContent));
        // 3.4 响应结果
        Map<Long, AiKnowledgeDocumentDO> documentMap =
                knowledgeDocumentService.getKnowledgeDocumentMap(
                        convertSet(
                                knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getDocumentId));
        List<AiChatMessageRespVO.KnowledgeSegment> segments =
                knowledgeSegments.stream()
                        .map(
                                seg -> {
                                    AiChatMessageRespVO.KnowledgeSegment vo =
                                            BeanUtil.toBean(
                                                    seg,
                                                    AiChatMessageRespVO.KnowledgeSegment.class);
                                    AiKnowledgeDocumentDO document =
                                            documentMap.get(seg.getDocumentId());
                                    vo.setDocumentName(
                                            document != null ? document.getName() : null);
                                    return vo;
                                })
                        .toList();
        return new AiChatMessageSendRespVO()
                .setSend(BeanUtil.toBean(userMessage, AiChatMessageSendRespVO.Message.class))
                .setReceive(
                        BeanUtil.toBean(assistantMessage, AiChatMessageSendRespVO.Message.class)
                                .setContent(newContent)
                                .setSegments(segments));
    }

    @Override
    public Flux<CommonResult<AiChatMessageSendRespVO>> sendChatMessageStream(
            AiChatMessageSendReqVO sendReqVO, Long userId) {
        return Flux.defer(
                        () -> {
                            // 1.1 校验对话存在
                            AiChatConversationDO conversation =
                                    chatConversationService.validateChatConversationExists(
                                            sendReqVO.getConversationId());
                            if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
                                throw exception(CHAT_CONVERSATION_NOT_EXISTS);
                            }
                            List<AiChatMessageDO> historyMessages =
                                    chatMessageMapper.selectList(
                                            new LambdaQueryWrapperX<AiChatMessageDO>()
                                                    .eq(
                                                            AiChatMessageDO::getConversationId,
                                                            conversation.getId())
                                                    .orderByAsc(AiChatMessageDO::getId));
                            // 1.2 校验模型
                            AiModelDO model = modalService.validateModel(conversation.getModelId());
                            ChatModel chatModel = modalService.getChatModel(model.getId());

                            // 2. 知识库找回
                            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments =
                                    recallKnowledgeSegment(sendReqVO.getContent(), conversation);

                            // 3. 插入 user 发送消息
                            AiChatMessageDO userMessage =
                                    createChatMessage(
                                            conversation.getId(),
                                            null,
                                            model.getModel(),
                                            model.getId(),
                                            userId,
                                            conversation.getRoleId(),
                                            MessageType.USER,
                                            sendReqVO.getContent(),
                                            sendReqVO.getUseContext(),
                                            null);

                            // 4.1 插入 assistant 接收消息
                            AiChatMessageDO assistantMessage =
                                    createChatMessage(
                                            conversation.getId(),
                                            userMessage.getId(),
                                            model.getModel(),
                                            model.getId(),
                                            userId,
                                            conversation.getRoleId(),
                                            MessageType.ASSISTANT,
                                            "",
                                            sendReqVO.getUseContext(),
                                            knowledgeSegments);

                            // 4.2 构建 Prompt，并进行调用
                            Prompt prompt =
                                    buildPrompt(
                                            conversation,
                                            historyMessages,
                                            knowledgeSegments,
                                            model,
                                            sendReqVO);

                            ChatClient chatClient =
                                    ChatClient.builder(chatModel)
                                            //                        .defaultAdvisors(
                                            //
                                            // VectorStoreChatMemoryAdvisor.builder(orCreateVectorStore)
                                            //
                                            // .conversationId(
                                            //
                                            // assistantMessage.getConversationId().toString())
                                            //                                        .build())
                                            .build();

                            RequestContext context = new RequestContext();

                            return response(
                                    knowledgeSegments,
                                    prompt,
                                    userMessage,
                                    assistantMessage,
                                    chatClient,
                                    context);
                        })
                .onErrorResume(
                        ex -> {
                            log.error("[sendChatMessageStream]", ex);
                            if (ex instanceof ServiceException se) {
                                return Flux.just(CommonResult.error(se.getCode(), se.getMessage()));
                            }
                            return Flux.just(CommonResult.error(INTERNAL_SERVER_ERROR));
                        });
    }

    @NotNull
    private Flux<CommonResult<AiChatMessageSendRespVO>> response(
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            Prompt prompt,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            ChatClient chatClient,
            RequestContext context) {
        StringBuffer contentBuffer = new StringBuffer();
        return chatClient.prompt(prompt).stream()
                .chatResponse()
                .map(
                        response -> {
                            // 处理知识库的返回，只有首次才有
                            List<AiChatMessageRespVO.KnowledgeSegment> segments = null;
                            if (StrUtil.isEmpty(contentBuffer)) {
                                Map<Long, AiKnowledgeDocumentDO> documentMap =
                                        knowledgeDocumentService.getKnowledgeDocumentMap(
                                                convertSet(
                                                        knowledgeSegments,
                                                        AiKnowledgeSegmentSearchRespBO
                                                                ::getDocumentId));
                                segments =
                                        knowledgeSegments.stream()
                                                .map(
                                                        seg -> {
                                                            AiChatMessageRespVO.KnowledgeSegment
                                                                    vo =
                                                                            BeanUtil.toBean(
                                                                                    seg,
                                                                                    AiChatMessageRespVO
                                                                                            .KnowledgeSegment
                                                                                            .class);
                                                            AiKnowledgeDocumentDO document =
                                                                    documentMap.get(
                                                                            seg.getDocumentId());
                                                            vo.setDocumentName(
                                                                    document != null
                                                                            ? document.getName()
                                                                            : null);
                                                            return vo;
                                                        })
                                                .toList();
                            }

                            if (response.hasToolCalls()) {
                                context.run(
                                        () -> {
                                            AiChatMessageDO toUpdate =
                                                    chatMessageMapper.selectById(
                                                            assistantMessage.getId());
                                            toUpdate.setContent(
                                                    StrUtil.nullToEmpty(toUpdate.getContent())
                                                            + contentBuffer);
                                            chatMessageMapper.updateById(toUpdate);
                                        });
                                contentBuffer.delete(0, contentBuffer.length());
                                return executeToolCallWithNotifications(
                                        prompt,
                                        response,
                                        userMessage,
                                        assistantMessage,
                                        segments,
                                        knowledgeSegments,
                                        chatClient,
                                        context);
                            } else {
                                response.getResult();
                                String newContent = response.getResult().getOutput().getText();
                                newContent = StrUtil.nullToDefault(newContent, ""); // 避免 null 的 情况
                                contentBuffer.append(newContent);

                                return Flux.just(
                                        success(
                                                new AiChatMessageSendRespVO()
                                                        .setSend(
                                                                BeanUtil.toBean(
                                                                        userMessage,
                                                                        AiChatMessageSendRespVO
                                                                                .Message.class))
                                                        .setReceive(
                                                                BeanUtil.toBean(
                                                                                assistantMessage,
                                                                                AiChatMessageSendRespVO
                                                                                        .Message
                                                                                        .class)
                                                                        .setContent(newContent)
                                                                        .setSegments(segments))));
                            }
                        })
                .flatMap(response -> response)
                .doOnComplete(
                        () -> {
                            context.run(
                                    () -> {
                                        AiChatMessageDO toUpdate =
                                                chatMessageMapper.selectById(
                                                        assistantMessage.getId());
                                        toUpdate.setContent(
                                                StrUtil.nullToEmpty(toUpdate.getContent())
                                                        + contentBuffer.toString());
                                        chatMessageMapper.updateById(toUpdate);
                                    });
                        })
                .doOnError(
                        throwable -> {
                            log.error("Chat stream error", throwable);
                            String errorMsg = ExceptionUtil.getRootCause(throwable).getMessage();
                            chatMessageMapper.updateById(
                                    new AiChatMessageDO()
                                            .setId(assistantMessage.getId())
                                            .setContent(errorMsg));
                        })
                .onErrorResume(
                        error ->
                                Flux.just(
                                        CommonResult.error(
                                                AIErrorCodeConstants.CHAT_STREAM_ERROR)));
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> executeToolCallWithNotifications(
            Prompt prompt,
            ChatResponse response,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            List<AiChatMessageRespVO.KnowledgeSegment> segments,
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            ChatClient chatClient,
            RequestContext context) {
        return Flux.concat(
                beforeToolCall(assistantMessage, segments, response, knowledgeSegments, context),
                executeToolCall(
                        prompt,
                        response,
                        userMessage,
                        assistantMessage,
                        knowledgeSegments,
                        chatClient,
                        context));
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> beforeToolCall(
            AiChatMessageDO assistantMessage,
            List<AiChatMessageRespVO.KnowledgeSegment> segments,
            ChatResponse response,
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            RequestContext context) {
        Optional<Generation> toolCallGeneration =
                response.getResults().stream()
                        .filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
                        .findFirst();
        if (toolCallGeneration.isEmpty()) {
            throw new IllegalStateException("No tool call requested by the chat model");
        }
        Generation generation = toolCallGeneration.get();
        List<AssistantMessage.ToolCall> toolCalls = generation.getOutput().getToolCalls();
        StringBuilder contentBuffer = new StringBuilder();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            contentBuffer.append(
                    String.format("调用 %s 工具, 参数为 %s\n", toolCall.name(), toolCall.arguments()));
        }
        log.info("Tool call requested: {}", contentBuffer);

        AiChatMessageDO toolMessage =
                context.run(
                        () ->
                                createChatMessage(
                                        assistantMessage.getConversationId(),
                                        assistantMessage.getReplyId(),
                                        assistantMessage.getModel(),
                                        assistantMessage.getModelId(),
                                        assistantMessage.getUserId(),
                                        assistantMessage.getRoleId(),
                                        MessageType.TOOL,
                                        contentBuffer.toString(),
                                        assistantMessage.getUseContext(),
                                        knowledgeSegments));

        return Flux.just(
                success(
                        new AiChatMessageSendRespVO()
                                .setSend(
                                        BeanUtil.toBean(
                                                toolMessage, AiChatMessageSendRespVO.Message.class))
                                .setReceive(
                                        BeanUtil.toBean(
                                                        assistantMessage,
                                                        AiChatMessageSendRespVO.Message.class)
                                                .setContent(contentBuffer.toString())
                                                .setSegments(segments))));
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> executeToolCall(
            Prompt prompt,
            ChatResponse response,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            ChatClient chatClient,
            RequestContext context) {
        try {
            log.info("execute tools: {}", response);
            ToolExecutionResult toolExecutionResult =
                    context.run(() -> toolCallingManager.executeToolCalls(prompt, response));
            Prompt newPrompt =
                    new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions());
            return response(
                    knowledgeSegments,
                    newPrompt,
                    userMessage,
                    assistantMessage,
                    chatClient,
                    context);
        } catch (Exception e) {
            return reportAndAnalyzeToolCallErrorAndContinue(
                    knowledgeSegments,
                    prompt,
                    userMessage,
                    assistantMessage,
                    response,
                    e,
                    chatClient,
                    context);
        }
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> reportAndAnalyzeToolCallErrorAndContinue(
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            Prompt prompt,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            ChatResponse response,
            Exception exception,
            ChatClient chatClient,
            RequestContext context) {
        return Flux.concat(
                createErrorFlux(assistantMessage, ExceptionUtil.getRootCauseMessage(exception)),
                analyzeToolCallErrorAndContinue(
                        knowledgeSegments,
                        prompt,
                        userMessage,
                        assistantMessage,
                        response,
                        exception,
                        chatClient,
                        context));
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> analyzeToolCallErrorAndContinue(
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            Prompt prompt,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            ChatResponse response,
            Exception exception,
            ChatClient chatClient,
            RequestContext context) {
        String rootCauseMessage = ExceptionUtil.getRootCauseMessage(exception);

        ArrayList<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

        Optional<Generation> toolCallGeneration =
                response.getResults().stream()
                        .filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
                        .findFirst();
        if (toolCallGeneration.isEmpty()) {
            throw new IllegalStateException("No tool call requested by the chat model");
        }

        AssistantMessage msg = toolCallGeneration.get().getOutput();
        prompt.getInstructions().add(msg);
        String aiRetry = String.format("调用工具出现异常, 异常信息为: %s, 不调用工具分析一下这个错误", rootCauseMessage);

        for (AssistantMessage.ToolCall toolCall : msg.getToolCalls()) {
            ToolResponseMessage.ToolResponse toolResponse =
                    new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), aiRetry);
            toolResponses.add(toolResponse);
        }

        ToolResponseMessage toolResponseMessage =
                ToolResponseMessage.builder().responses(toolResponses).build();
        prompt.getInstructions().add(toolResponseMessage);

        AiChatMessageDO chatMessage =
                context.run(
                        () ->
                                createChatMessage(
                                        assistantMessage.getConversationId(),
                                        assistantMessage.getReplyId(),
                                        assistantMessage.getModel(),
                                        assistantMessage.getModelId(),
                                        assistantMessage.getUserId(),
                                        assistantMessage.getRoleId(),
                                        MessageType.ASSISTANT,
                                        "",
                                        assistantMessage.getUseContext(),
                                        knowledgeSegments));

        return Flux.concat(
                response(knowledgeSegments, prompt, userMessage, chatMessage, chatClient, context),
                Flux.defer(
                        () ->
                                createContinueFlux(
                                        knowledgeSegments,
                                        prompt,
                                        userMessage,
                                        chatMessage,
                                        chatClient,
                                        context)));
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> createContinueFlux(
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            Prompt prompt,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            ChatClient chatClient,
            RequestContext context) {
        UserMessage continueChatMessage =
                new UserMessage(
                        String.format("出现了错误，继续完成之前的任务: %s", assistantMessage.getContent()));
        prompt.getInstructions().add(continueChatMessage);

        AiChatMessageDO newAssistantMessage =
                createChatMessage(
                        assistantMessage.getConversationId(),
                        assistantMessage.getReplyId(),
                        assistantMessage.getModel(),
                        assistantMessage.getId(),
                        assistantMessage.getUserId(),
                        assistantMessage.getRoleId(),
                        MessageType.ASSISTANT,
                        "",
                        assistantMessage.getUseContext(),
                        knowledgeSegments);

        return response(
                knowledgeSegments, prompt, userMessage, newAssistantMessage, chatClient, context);
    }

    private Flux<CommonResult<AiChatMessageSendRespVO>> createErrorFlux(
            AiChatMessageDO assistantMessage, String errorMessage) {
        String formatted = String.format("请求调用工具出现异常, 异常信息为: %s", errorMessage);
        return Flux.just(
                success(
                        new AiChatMessageSendRespVO()
                                .setSend(
                                        BeanUtil.toBean(
                                                assistantMessage,
                                                AiChatMessageSendRespVO.Message.class))
                                .setReceive(
                                        BeanUtil.toBean(
                                                        assistantMessage,
                                                        AiChatMessageSendRespVO.Message.class)
                                                .setContent(formatted))));
    }

    private List<AiKnowledgeSegmentSearchRespBO> recallKnowledgeSegment(
            String content, AiChatConversationDO conversation) {
        // 1. 查询聊天角色
        if (conversation == null || conversation.getRoleId() == null) {
            return Collections.emptyList();
        }
        AiChatRoleDO role = chatRoleService.getChatRole(conversation.getRoleId());
        if (role == null || StrUtil.isBlank(role.getKnowledgeIds())) {
            return Collections.emptyList();
        }
        List<Long> knowledgeIds =
                cn.hutool.json.JSONUtil.toList(role.getKnowledgeIds(), Long.class);
        if (CollUtil.isEmpty(knowledgeIds)) {
            return Collections.emptyList();
        }

        // 2. 遍历找回
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = new ArrayList<>();
        for (Long knowledgeId : knowledgeIds) {
            knowledgeSegments.addAll(
                    knowledgeSegmentService.searchKnowledgeSegment(
                            new AiKnowledgeSegmentSearchReqBO()
                                    .setKnowledgeId(knowledgeId)
                                    .setContent(content)));
        }
        return knowledgeSegments;
    }

    private Prompt buildPrompt(
            AiChatConversationDO conversation,
            List<AiChatMessageDO> messages,
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            AiModelDO model,
            AiChatMessageSendReqVO sendReqVO) {
        List<Message> chatMessages = new ArrayList<>();
        // 1.1 System Context 角色设定
        StringBuilder systemPrompt = new StringBuilder();

        chatMessages.add(new SystemMessage(systemPrompt.toString()));
        //        if (StrUtil.isNotBlank(conversation.getSystemMessage())) {
        //            chatMessages.add(new SystemMessage(conversation.getSystemMessage()));
        //        }

        // 1.2 历史 history message 历史消息
        List<AiChatMessageDO> contextMessages =
                filterContextMessages(messages, conversation, sendReqVO);
        contextMessages.forEach(
                message ->
                        chatMessages.add(
                                AiUtils.buildMessage(message.getType(), message.getContent())));

        // 1.3 当前 user message 新发送消息
        chatMessages.add(new UserMessage(sendReqVO.getContent()));

        // 1.4 知识库，通过 UserMessage 实现
        if (CollUtil.isNotEmpty(knowledgeSegments)) {
            String reference =
                    knowledgeSegments.stream()
                            .map(segment -> "<Reference>" + segment.getContent() + "</Reference>")
                            .collect(Collectors.joining("\n\n"));
            chatMessages.add(
                    new UserMessage(String.format(KNOWLEDGE_USER_MESSAGE_TEMPLATE, reference)));
        }

        // 2.1 查询 tool 工具
        Set<String> toolNames = null;
        Map<String, Object> toolContext = Map.of();
        if (conversation.getRoleId() != null) {
            AiChatRoleDO chatRole = chatRoleService.getChatRole(conversation.getRoleId());
            if (chatRole != null && StrUtil.isNotBlank(chatRole.getToolIds())) {
                List<Long> toolIds =
                        cn.hutool.json.JSONUtil.toList(chatRole.getToolIds(), Long.class);
                toolNames = convertSet(toolService.getToolList(toolIds), AiToolDO::getName);
                toolContext = AiUtils.buildCommonToolContext();
            }
        }
        // 2.2 构建 ChatOptions 对象
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());

        List<org.springframework.ai.tool.ToolCallback> commonTools = toolService.getCommonTools();

        ChatOptions chatOptions =
                AiUtils.buildChatOptions(
                        platform,
                        model.getModel(),
                        conversation.getTemperature(),
                        conversation.getMaxTokens(),
                        toolNames,
                        commonTools,
                        toolContext);
        return new Prompt(chatMessages, chatOptions);
    }

    /**
     * 从历史消息中，获得倒序的 n 组消息作为消息上下文
     *
     * <p>n 组：指的是 user + assistant 形成一组
     *
     * @param messages 消息列表
     * @param conversation 对话
     * @param sendReqVO 发送请求
     * @return 消息上下文
     */
    private List<AiChatMessageDO> filterContextMessages(
            List<AiChatMessageDO> messages,
            AiChatConversationDO conversation,
            AiChatMessageSendReqVO sendReqVO) {
        if (conversation.getMaxContexts() == null
                || ObjUtil.notEqual(sendReqVO.getUseContext(), Boolean.TRUE)) {
            return Collections.emptyList();
        }
        List<AiChatMessageDO> contextMessages = new ArrayList<>(conversation.getMaxContexts() * 2);
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessageDO assistantMessage = CollUtil.get(messages, i);
            if (assistantMessage == null || assistantMessage.getReplyId() == null) {
                continue;
            }
            AiChatMessageDO userMessage = CollUtil.get(messages, i - 1);
            if (userMessage == null
                    || ObjUtil.notEqual(assistantMessage.getReplyId(), userMessage.getId())
                    || StrUtil.isEmpty(assistantMessage.getContent())) {
                continue;
            }
            // 由于后续要 reverse 反转，所以先添加 assistantMessage
            contextMessages.add(assistantMessage);
            contextMessages.add(userMessage);
            // 超过最大上下文，结束
            if (contextMessages.size() >= conversation.getMaxContexts() * 2) {
                break;
            }
        }
        Collections.reverse(contextMessages);
        return contextMessages;
    }

    private AiChatMessageDO createChatMessage(
            Long conversationId,
            Long replyId,
            String model,
            Long modelId,
            Long userId,
            Long roleId,
            MessageType messageType,
            String content,
            Boolean useContext,
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments) {
        AiChatMessageDO message =
                new AiChatMessageDO()
                        .setConversationId(conversationId)
                        .setReplyId(replyId)
                        .setModel(model)
                        .setModelId(modelId)
                        .setUserId(userId)
                        .setRoleId(roleId)
                        .setType(messageType.getValue())
                        .setContent(content)
                        .setUseContext(useContext)
                        .setSegmentIds(
                                cn.hutool.json.JSONUtil.toJsonStr(
                                        convertList(
                                                knowledgeSegments,
                                                AiKnowledgeSegmentSearchRespBO::getId)));
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public List<AiChatMessageDO> getChatMessageListByConversationId(Long conversationId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapperX<AiChatMessageDO>()
                        .eq(AiChatMessageDO::getConversationId, conversationId)
                        .orderByAsc(AiChatMessageDO::getId));
    }

    @Override
    public void deleteChatMessage(Long id, Long userId) {
        AiChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null || ObjUtil.notEqual(message.getUserId(), userId)) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        chatMessageMapper.deleteById(id);
    }

    @Override
    public void deleteChatMessageByConversationId(Long conversationId, Long userId) {
        List<AiChatMessageDO> messages =
                chatMessageMapper.selectList(
                        new LambdaQueryWrapperX<AiChatMessageDO>()
                                .eq(AiChatMessageDO::getConversationId, conversationId));
        if (CollUtil.isEmpty(messages)
                || ObjUtil.notEqual(messages.getFirst().getUserId(), userId)) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        chatMessageMapper.deleteBatchIds(convertList(messages, AiChatMessageDO::getId));
    }

    @Override
    public void deleteChatMessageByAdmin(Long id) {
        AiChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        chatMessageMapper.deleteById(id);
    }

    @Override
    public Map<Long, Integer> getChatMessageCountMap(Collection<Long> conversationIds) {
        List<AiChatMessageDO> messages =
                chatMessageMapper.selectList(
                        new LambdaQueryWrapperX<AiChatMessageDO>()
                                .in(AiChatMessageDO::getConversationId, conversationIds));

        return messages.stream()
                .collect(
                        Collectors.groupingBy(
                                AiChatMessageDO::getConversationId,
                                Collectors.collectingAndThen(
                                        Collectors.counting(), Math::toIntExact)));
    }

    @Override
    public PageResult<AiChatMessageDO> getChatMessagePage(AiChatMessagePageReqVO pageReqVO) {
        PageResult<AiChatMessageDO> result = chatMessageMapper.selectPage(pageReqVO);
        return new PageResult<>(result.getList(), result.getTotal());
    }
}
