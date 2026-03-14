package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.CHAT_ROLE_DISABLE;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.CHAT_ROLE_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiChatRoleMapper;
import com.hhoa.kline.web.dal.dataobject.AiChatRoleDO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRolePageReqVO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRoleSaveMyReqVO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRoleSaveReqVO;
import com.hhoa.kline.web.service.AiChatRoleService;
import com.hhoa.kline.web.service.AiKnowledgeService;
import com.hhoa.kline.web.service.AiToolService;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiChatRoleServiceImpl implements AiChatRoleService {

    @Resource private AiChatRoleMapper chatRoleMapper;

    @Resource private AiKnowledgeService knowledgeService;

    @Resource private AiToolService toolService;

    @Override
    public Long createChatRole(AiChatRoleSaveReqVO createReqVO) {
        validateDocuments(createReqVO.getKnowledgeIds());
        validateTools(createReqVO.getToolIds());

        AiChatRoleDO chatRole = BeanUtil.toBean(createReqVO, AiChatRoleDO.class);
        chatRole.setKnowledgeIds(JSONUtil.toJsonStr(createReqVO.getKnowledgeIds()));
        chatRole.setToolIds(JSONUtil.toJsonStr(createReqVO.getToolIds()));
        chatRoleMapper.insert(chatRole);
        return chatRole.getId();
    }

    @Override
    public Long createChatRoleMy(AiChatRoleSaveMyReqVO createReqVO, Long userId) {
        validateDocuments(createReqVO.getKnowledgeIds());
        validateTools(createReqVO.getToolIds());

        AiChatRoleDO chatRole = BeanUtil.toBean(createReqVO, AiChatRoleDO.class);
        chatRole.setKnowledgeIds(JSONUtil.toJsonStr(createReqVO.getKnowledgeIds()));
        chatRole.setToolIds(JSONUtil.toJsonStr(createReqVO.getToolIds()));
        chatRole.setUserId(userId);
        chatRole.setStatus(CommonStatusEnum.ENABLE.getStatus());
        chatRole.setPublicStatus(false);
        chatRoleMapper.insert(chatRole);
        return chatRole.getId();
    }

    @Override
    public void updateChatRole(AiChatRoleSaveReqVO updateReqVO) {
        validateChatRoleExists(updateReqVO.getId());
        validateDocuments(updateReqVO.getKnowledgeIds());
        validateTools(updateReqVO.getToolIds());

        AiChatRoleDO updateObj = BeanUtil.toBean(updateReqVO, AiChatRoleDO.class);
        updateObj.setKnowledgeIds(JSONUtil.toJsonStr(updateReqVO.getKnowledgeIds()));
        updateObj.setToolIds(JSONUtil.toJsonStr(updateReqVO.getToolIds()));
        chatRoleMapper.updateById(updateObj);
    }

    @Override
    public void updateChatRoleMy(AiChatRoleSaveMyReqVO updateReqVO, Long userId) {
        AiChatRoleDO chatRole = validateChatRoleExists(updateReqVO.getId());
        if (ObjectUtil.notEqual(chatRole.getUserId(), userId)) {
            throw exception(CHAT_ROLE_NOT_EXISTS);
        }
        validateDocuments(updateReqVO.getKnowledgeIds());
        validateTools(updateReqVO.getToolIds());

        AiChatRoleDO updateObj = BeanUtil.toBean(updateReqVO, AiChatRoleDO.class);
        updateObj.setKnowledgeIds(JSONUtil.toJsonStr(updateReqVO.getKnowledgeIds()));
        updateObj.setToolIds(JSONUtil.toJsonStr(updateReqVO.getToolIds()));
        chatRoleMapper.updateById(updateObj);
    }

    private void validateDocuments(List<Long> knowledgeIds) {
        if (CollUtil.isEmpty(knowledgeIds)) {
            return;
        }
        knowledgeIds.forEach(knowledgeService::validateKnowledgeExists);
    }

    private void validateTools(List<Long> toolIds) {
        if (CollUtil.isEmpty(toolIds)) {
            return;
        }
        toolIds.forEach(toolService::validateToolExists);
    }

    @Override
    public void deleteChatRole(Long id) {
        validateChatRoleExists(id);
        chatRoleMapper.deleteById(id);
    }

    @Override
    public void deleteChatRoleMy(Long id, Long userId) {
        AiChatRoleDO chatRole = validateChatRoleExists(id);
        if (ObjectUtil.notEqual(chatRole.getUserId(), userId)) {
            throw exception(CHAT_ROLE_NOT_EXISTS);
        }
        chatRoleMapper.deleteById(id);
    }

    private AiChatRoleDO validateChatRoleExists(Long id) {
        AiChatRoleDO chatRole = chatRoleMapper.selectById(id);
        if (chatRole == null) {
            throw exception(CHAT_ROLE_NOT_EXISTS);
        }
        return chatRole;
    }

    @Override
    public AiChatRoleDO getChatRole(Long id) {
        return chatRoleMapper.selectById(id);
    }

    @Override
    public List<AiChatRoleDO> getChatRoleList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return chatRoleMapper.selectBatchIds(ids);
    }

    @Override
    public AiChatRoleDO validateChatRole(Long id) {
        AiChatRoleDO chatRole = validateChatRoleExists(id);
        if (CommonStatusEnum.isDisable(chatRole.getStatus())) {
            throw exception(CHAT_ROLE_DISABLE, chatRole.getName());
        }
        return chatRole;
    }

    @Override
    public PageResult<AiChatRoleDO> getChatRolePage(AiChatRolePageReqVO pageReqVO) {
        LambdaQueryWrapper<AiChatRoleDO> wrapper =
                new LambdaQueryWrapperX<AiChatRoleDO>()
                        .likeIfPresent(AiChatRoleDO::getName, pageReqVO.getName())
                        .eqIfPresent(AiChatRoleDO::getCategory, pageReqVO.getCategory())
                        .eqIfPresent(AiChatRoleDO::getPublicStatus, pageReqVO.getPublicStatus())
                        .orderByAsc(AiChatRoleDO::getSort);
        PageResult<AiChatRoleDO> result = chatRoleMapper.selectPage(pageReqVO, wrapper);
        return new PageResult<>(result.getList(), result.getTotal());
    }

    @Override
    public PageResult<AiChatRoleDO> getChatRoleMyPage(AiChatRolePageReqVO pageReqVO, Long userId) {
        LambdaQueryWrapper<AiChatRoleDO> wrapper =
                new LambdaQueryWrapperX<AiChatRoleDO>()
                        .likeIfPresent(AiChatRoleDO::getName, pageReqVO.getName())
                        .eqIfPresent(AiChatRoleDO::getCategory, pageReqVO.getCategory())
                        .orderByAsc(AiChatRoleDO::getSort);
        if (pageReqVO.getPublicStatus() != null) {
            if (pageReqVO.getPublicStatus()) {
                wrapper.eq(AiChatRoleDO::getPublicStatus, true);
            } else {
                wrapper.eq(AiChatRoleDO::getUserId, userId)
                        .eq(AiChatRoleDO::getPublicStatus, false);
            }
        }
        PageResult<AiChatRoleDO> result = chatRoleMapper.selectPage(pageReqVO, wrapper);
        return new PageResult<>(result.getList(), result.getTotal());
    }

    @Override
    public List<String> getChatRoleCategoryList() {
        List<AiChatRoleDO> roles =
                chatRoleMapper.selectList(
                        new LambdaQueryWrapperX<AiChatRoleDO>()
                                .eq(AiChatRoleDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
        return roles.stream()
                .map(AiChatRoleDO::getCategory)
                .filter(category -> category != null && StrUtil.isNotBlank(category))
                .distinct()
                .toList();
    }

    @Override
    public List<AiChatRoleDO> getChatRoleListByName(String name) {
        return chatRoleMapper.selectList(
                new LambdaQueryWrapperX<AiChatRoleDO>()
                        .like(AiChatRoleDO::getName, name)
                        .orderByAsc(AiChatRoleDO::getSort));
    }
}
