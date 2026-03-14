package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.CHAT_CONVERSATION_MODEL_ERROR;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiChatConversationMapper;
import com.hhoa.kline.web.dal.dataobject.AiChatConversationDO;
import com.hhoa.kline.web.dal.dataobject.AiChatRoleDO;
import com.hhoa.kline.web.dal.dataobject.AiModelDO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationCreateMyReqVO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationPageReqVO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationUpdateMyReqVO;
import com.hhoa.kline.web.enums.AiModelTypeEnum;
import com.hhoa.kline.web.service.AiChatConversationService;
import com.hhoa.kline.web.service.AiChatRoleService;
import com.hhoa.kline.web.service.AiKnowledgeService;
import com.hhoa.kline.web.service.AiModelService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Slf4j
public class AiChatConversationServiceImpl implements AiChatConversationService {

    @Resource private AiChatConversationMapper chatConversationMapper;

    @Resource private AiModelService modalService;

    @Resource private AiChatRoleService chatRoleService;

    @Resource private AiKnowledgeService knowledgeService;

    @Override
    public Long createChatConversationMy(AiChatConversationCreateMyReqVO createReqVO, Long userId) {
        AiChatRoleDO role =
                createReqVO.getRoleId() != null
                        ? chatRoleService.validateChatRole(createReqVO.getRoleId())
                        : null;
        AiModelDO model =
                role != null && role.getModelId() != null
                        ? modalService.validateModel(role.getModelId())
                        : modalService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
        Assert.notNull(model, "必须找到默认模型");
        validateChatModel(model);

        if (Objects.nonNull(createReqVO.getKnowledgeId())) {
            knowledgeService.validateKnowledgeExists(createReqVO.getKnowledgeId());
        }

        AiChatConversationDO conversation =
                new AiChatConversationDO()
                        .setUserId(userId)
                        .setPinned(false)
                        .setModelId(model.getId())
                        .setModel(model.getModel())
                        .setTemperature(model.getTemperature())
                        .setMaxTokens(model.getMaxTokens())
                        .setMaxContexts(model.getMaxContexts());
        if (role != null) {
            conversation
                    .setTitle(role.getName())
                    .setRoleId(role.getId())
                    .setSystemMessage(role.getSystemMessage());
        } else {
            conversation.setTitle(AiChatConversationDO.TITLE_DEFAULT);
        }
        chatConversationMapper.insert(conversation);
        return conversation.getId();
    }

    @Override
    public void updateChatConversationMy(AiChatConversationUpdateMyReqVO updateReqVO, Long userId) {
        AiChatConversationDO conversation = validateChatConversationExists(updateReqVO.getId());
        if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        AiModelDO model = null;
        if (updateReqVO.getModelId() != null) {
            model = modalService.validateModel(updateReqVO.getModelId());
        }

        if (updateReqVO.getKnowledgeId() != null) {
            knowledgeService.validateKnowledgeExists(updateReqVO.getKnowledgeId());
        }

        AiChatConversationDO updateObj = BeanUtil.toBean(updateReqVO, AiChatConversationDO.class);
        if (Boolean.TRUE.equals(updateReqVO.getPinned())) {
            updateObj.setPinnedTime(LocalDateTime.now());
        }
        if (model != null) {
            updateObj.setModel(model.getModel());
        }
        chatConversationMapper.updateById(updateObj);
    }

    @Override
    public List<AiChatConversationDO> getChatConversationListByUserId(Long userId) {
        return chatConversationMapper.selectList(
                new LambdaQueryWrapperX<AiChatConversationDO>()
                        .eq(AiChatConversationDO::getUserId, userId)
                        .orderByDesc(AiChatConversationDO::getCreateTime));
    }

    @Override
    public AiChatConversationDO getChatConversation(Long id) {
        return chatConversationMapper.selectById(id);
    }

    @Override
    public void deleteChatConversationMy(Long id, Long userId) {
        AiChatConversationDO conversation = validateChatConversationExists(id);
        if (conversation == null || ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        chatConversationMapper.deleteById(id);
    }

    @Override
    public void deleteChatConversationByAdmin(Long id) {
        AiChatConversationDO conversation = validateChatConversationExists(id);
        if (conversation == null) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        chatConversationMapper.deleteById(id);
    }

    private void validateChatModel(AiModelDO model) {
        if (ObjectUtil.isAllNotEmpty(
                model.getTemperature(), model.getMaxTokens(), model.getMaxContexts())) {
            return;
        }
        Assert.equals(model.getType(), AiModelTypeEnum.CHAT.getType(), "模型类型不正确：" + model);
        throw exception(CHAT_CONVERSATION_MODEL_ERROR);
    }

    public AiChatConversationDO validateChatConversationExists(Long id) {
        AiChatConversationDO conversation = chatConversationMapper.selectById(id);
        if (conversation == null) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        return conversation;
    }

    @Override
    public void deleteChatConversationMyByUnpinned(Long userId) {
        List<AiChatConversationDO> list =
                chatConversationMapper.selectList(
                        new LambdaQueryWrapperX<AiChatConversationDO>()
                                .eq(AiChatConversationDO::getUserId, userId)
                                .eq(AiChatConversationDO::getPinned, false));
        if (CollUtil.isEmpty(list)) {
            return;
        }
        chatConversationMapper.deleteBatchIds(convertList(list, AiChatConversationDO::getId));
    }

    @Override
    public PageResult<AiChatConversationDO> getChatConversationPage(
            AiChatConversationPageReqVO pageReqVO) {
        PageResult<AiChatConversationDO> result = chatConversationMapper.selectPage(pageReqVO);
        return new PageResult<>(result.getList(), result.getTotal());
    }
}
