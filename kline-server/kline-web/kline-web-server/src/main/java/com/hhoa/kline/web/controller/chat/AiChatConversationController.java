package com.hhoa.kline.web.controller.chat;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;
import static com.hhoa.kline.web.utils.LoginUserUtil.getLoginIdDefaultNull;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.hhoa.ai.kline.commons.utils.collection.CollectionUtils;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.object.BeanUtils;
import com.hhoa.kline.web.dal.dataobject.AiChatConversationDO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationCreateMyReqVO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationPageReqVO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationRespVO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationUpdateMyReqVO;
import com.hhoa.kline.web.service.AiChatConversationService;
import com.hhoa.kline.web.service.AiChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 聊天对话")
@RestController
@RequestMapping("/ai/chat/conversation")
@Validated
public class AiChatConversationController {

    @Resource private AiChatConversationService chatConversationService;
    @Resource private AiChatMessageService chatMessageService;

    @PostMapping("/create-my")
    @Operation(summary = "创建【我的】聊天对话")
    public CommonResult<Long> createChatConversationMy(
            @RequestBody @Valid AiChatConversationCreateMyReqVO createReqVO) {
        return success(
                chatConversationService.createChatConversationMy(
                        createReqVO, getLoginIdDefaultNull()));
    }

    @PutMapping("/update-my")
    @Operation(summary = "更新【我的】聊天对话")
    public CommonResult<Boolean> updateChatConversationMy(
            @RequestBody @Valid AiChatConversationUpdateMyReqVO updateReqVO) {
        chatConversationService.updateChatConversationMy(updateReqVO, getLoginIdDefaultNull());
        return success(true);
    }

    @GetMapping("/my-list")
    @Operation(summary = "获得【我的】聊天对话列表")
    public CommonResult<List<AiChatConversationRespVO>> getChatConversationMyList() {
        List<AiChatConversationDO> list =
                chatConversationService.getChatConversationListByUserId(getLoginIdDefaultNull());
        return success(BeanUtils.toBean(list, AiChatConversationRespVO.class));
    }

    @GetMapping("/get-my")
    @Operation(summary = "获得【我的】聊天对话")
    @Parameter(name = "id", required = true, description = "对话编号", example = "1024")
    public CommonResult<AiChatConversationRespVO> getChatConversationMy(
            @RequestParam("id") Long id) {
        AiChatConversationDO conversation = chatConversationService.getChatConversation(id);
        if (conversation != null
                && ObjUtil.notEqual(conversation.getUserId(), getLoginIdDefaultNull())) {
            conversation = null;
        }
        return success(BeanUtil.toBean(conversation, AiChatConversationRespVO.class));
    }

    @DeleteMapping("/delete-my")
    @Operation(summary = "删除聊天对话")
    @Parameter(name = "id", required = true, description = "对话编号", example = "1024")
    public CommonResult<Boolean> deleteChatConversationMy(@RequestParam("id") Long id) {
        chatConversationService.deleteChatConversationMy(id, getLoginIdDefaultNull());
        return success(true);
    }

    @DeleteMapping("/delete-by-unpinned")
    @Operation(summary = "删除未置顶的聊天对话")
    public CommonResult<Boolean> deleteChatConversationMyByUnpinned() {
        chatConversationService.deleteChatConversationMyByUnpinned(getLoginIdDefaultNull());
        return success(true);
    }

    // ========== 对话管理 ==========

    @GetMapping("/page")
    @Operation(summary = "获得对话分页", description = "用于【对话管理】菜单")
    public CommonResult<PageResult<AiChatConversationRespVO>> getChatConversationPage(
            AiChatConversationPageReqVO pageReqVO) {
        PageResult<AiChatConversationDO> pageResult =
                chatConversationService.getChatConversationPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }
        // 拼接关联数据
        Map<Long, Integer> messageCountMap =
                chatMessageService.getChatMessageCountMap(
                        CollectionUtils.convertList(
                                pageResult.getList(), AiChatConversationDO::getId));
        return success(
                BeanUtils.toPageResultBean(
                        pageResult,
                        AiChatConversationRespVO.class,
                        conversation ->
                                conversation.setMessageCount(
                                        messageCountMap.getOrDefault(conversation.getId(), 0))));
    }

    @Operation(summary = "管理员删除对话")
    @DeleteMapping("/delete-by-admin")
    @Parameter(name = "id", required = true, description = "对话编号", example = "1024")
    public CommonResult<Boolean> deleteChatConversationByAdmin(@RequestParam("id") Long id) {
        chatConversationService.deleteChatConversationByAdmin(id);
        return success(true);
    }
}
