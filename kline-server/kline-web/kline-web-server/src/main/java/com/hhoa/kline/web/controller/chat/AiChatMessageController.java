package com.hhoa.kline.web.controller.chat;

import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertListByFlatMap;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertSet;
import static com.hhoa.kline.web.common.pojo.CommonResult.success;
import static com.hhoa.kline.web.utils.LoginUserUtil.getLoginIdDefaultNull;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.hhoa.ai.kline.commons.utils.collection.MapUtils;
import com.hhoa.ai.kline.commons.utils.object.BeanUtils;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.PageUtils;
import com.hhoa.kline.web.dal.dataobject.AiChatConversationDO;
import com.hhoa.kline.web.dal.dataobject.AiChatMessageDO;
import com.hhoa.kline.web.dal.dataobject.AiChatRoleDO;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeSegmentDO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessagePageReqVO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessageRespVO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessageSendReqVO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessageSendRespVO;
import com.hhoa.kline.web.service.AiChatConversationService;
import com.hhoa.kline.web.service.AiChatMessageService;
import com.hhoa.kline.web.service.AiChatRoleService;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiKnowledgeSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "AI 聊天消息")
@RestController
@RequestMapping("/ai/chat/message")
@Slf4j
public class AiChatMessageController {

    @Resource private AiChatMessageService chatMessageService;
    @Resource private AiChatConversationService chatConversationService;
    @Resource private AiChatRoleService chatRoleService;
    @Resource private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource private AiKnowledgeDocumentService knowledgeDocumentService;

    @Operation(summary = "发送消息（段式）", description = "一次性返回，响应较慢")
    @PostMapping("/send")
    public CommonResult<AiChatMessageSendRespVO> sendMessage(
            @Valid @RequestBody AiChatMessageSendReqVO sendReqVO) {
        return success(chatMessageService.sendMessage(sendReqVO, getLoginIdDefaultNull()));
    }

    @Operation(summary = "发送消息（流式）", description = "流式返回，响应较快")
    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CommonResult<AiChatMessageSendRespVO>> sendChatMessageStream(
            @Valid @RequestBody AiChatMessageSendReqVO sendReqVO) {
        return chatMessageService.sendChatMessageStream(sendReqVO, getLoginIdDefaultNull());
    }

    @Operation(summary = "获得指定对话的消息列表")
    @GetMapping("/list-by-conversation-id")
    @Parameter(name = "conversationId", required = true, description = "对话编号", example = "1024")
    public CommonResult<List<AiChatMessageRespVO>> getChatMessageListByConversationId(
            @RequestParam("conversationId") Long conversationId) {
        AiChatConversationDO conversation =
                chatConversationService.getChatConversation(conversationId);
        if (conversation == null
                || ObjUtil.notEqual(conversation.getUserId(), getLoginIdDefaultNull())) {
            return success(Collections.emptyList());
        }
        // 1. 获取消息列表
        List<AiChatMessageDO> messageList =
                chatMessageService.getChatMessageListByConversationId(conversationId);
        if (CollUtil.isEmpty(messageList)) {
            return success(Collections.emptyList());
        }

        // 2. 拼接数据，主要是知识库段落信息
        Map<Long, AiKnowledgeSegmentDO> segmentMap =
                knowledgeSegmentService.getKnowledgeSegmentMap(
                        convertListByFlatMap(
                                messageList,
                                message -> {
                                    List<Long> segIds = parseSegmentIds(message.getSegmentIds());
                                    return segIds.isEmpty() ? null : segIds.stream();
                                }));
        Map<Long, AiKnowledgeDocumentDO> documentMap =
                knowledgeDocumentService.getKnowledgeDocumentMap(
                        convertList(segmentMap.values(), AiKnowledgeSegmentDO::getDocumentId));
        List<AiChatMessageRespVO> messageVOList =
                BeanUtils.toBean(messageList, AiChatMessageRespVO.class);
        for (int i = 0; i < messageList.size(); i++) {
            AiChatMessageDO message = messageList.get(i);
            List<Long> segIds = parseSegmentIds(message.getSegmentIds());
            if (segIds.isEmpty()) {
                continue;
            }
            // 设置知识库段落信息
            messageVOList
                    .get(i)
                    .setSegments(
                            convertList(
                                    segIds,
                                    segmentId -> {
                                        AiKnowledgeSegmentDO segment = segmentMap.get(segmentId);
                                        if (segment == null) {
                                            return null;
                                        }
                                        AiKnowledgeDocumentDO document =
                                                documentMap.get(segment.getDocumentId());
                                        if (document == null) {
                                            return null;
                                        }
                                        return new AiChatMessageRespVO.KnowledgeSegment()
                                                .setId(segment.getId())
                                                .setContent(segment.getContent())
                                                .setDocumentId(segment.getDocumentId())
                                                .setDocumentName(document.getName());
                                    }));
        }
        return success(messageVOList);
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/delete")
    @Parameter(name = "id", required = true, description = "消息编号", example = "1024")
    public CommonResult<Boolean> deleteChatMessage(@RequestParam("id") Long id) {
        chatMessageService.deleteChatMessage(id, getLoginIdDefaultNull());
        return success(true);
    }

    @Operation(summary = "删除指定对话的消息")
    @DeleteMapping("/delete-by-conversation-id")
    @Parameter(name = "conversationId", required = true, description = "对话编号", example = "1024")
    public CommonResult<Boolean> deleteChatMessageByConversationId(
            @RequestParam("conversationId") Long conversationId) {
        chatMessageService.deleteChatMessageByConversationId(
                conversationId, getLoginIdDefaultNull());
        return success(true);
    }

    // ========== 对话管理 ==========

    @GetMapping("/page")
    @Operation(summary = "获得消息分页", description = "用于【对话管理】菜单")
    public CommonResult<PageResult<AiChatMessageRespVO>> getChatMessagePage(
            AiChatMessagePageReqVO pageReqVO) {
        PageResult<AiChatMessageDO> pageResult = chatMessageService.getChatMessagePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }
        // 拼接数据
        Map<Long, AiChatRoleDO> roleMap =
                chatRoleService.getChatRoleMap(
                        convertSet(pageResult.getList(), AiChatMessageDO::getRoleId));
        return success(
                PageUtils.toPageResult(
                        pageResult,
                        AiChatMessageRespVO.class,
                        respVO ->
                                MapUtils.findAndThen(
                                        roleMap,
                                        respVO.getRoleId(),
                                        role -> respVO.setRoleName(role.getName()))));
    }

    @Operation(summary = "管理员删除消息")
    @DeleteMapping("/delete-by-admin")
    @Parameter(name = "id", required = true, description = "消息编号", example = "1024")
    public CommonResult<Boolean> deleteChatMessageByAdmin(@RequestParam("id") Long id) {
        chatMessageService.deleteChatMessageByAdmin(id);
        return success(true);
    }

    private static List<Long> parseSegmentIds(String segmentIds) {
        if (StrUtil.isBlank(segmentIds)) {
            return Collections.emptyList();
        }
        return Arrays.stream(segmentIds.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
