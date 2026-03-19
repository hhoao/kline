package com.hhoa.kline.web.controller.model;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;
import static com.hhoa.kline.web.utils.LoginUserUtil.getLoginIdDefaultNull;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.PageUtils;
import com.hhoa.kline.web.dal.dataobject.AiChatRoleDO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRolePageReqVO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRoleRespVO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRoleSaveMyReqVO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRoleSaveReqVO;
import com.hhoa.kline.web.service.AiChatRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 聊天角色")
@RestController
@RequestMapping("/ai/chat-role")
@Validated
public class AiChatRoleController {

    @Resource private AiChatRoleService chatRoleService;

    @GetMapping("/my-page")
    @Operation(summary = "获得【我的】聊天角色分页")
    public CommonResult<PageResult<AiChatRoleRespVO>> getChatRoleMyPage(
            @Valid AiChatRolePageReqVO pageReqVO) {
        PageResult<AiChatRoleDO> pageResult =
                chatRoleService.getChatRoleMyPage(pageReqVO, getLoginIdDefaultNull());
        return success(PageUtils.toPageResult(pageResult, AiChatRoleRespVO.class));
    }

    @GetMapping("/get-my")
    @Operation(summary = "获得【我的】聊天角色")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AiChatRoleRespVO> getChatRoleMy(@RequestParam("id") Long id) {
        AiChatRoleDO chatRole = chatRoleService.getChatRole(id);
        if (ObjUtil.notEqual(chatRole.getUserId(), getLoginIdDefaultNull())) {
            return success(null);
        }
        return success(BeanUtil.toBean(chatRole, AiChatRoleRespVO.class));
    }

    @PostMapping("/create-my")
    @Operation(summary = "创建【我的】聊天角色")
    public CommonResult<Long> createChatRoleMy(
            @Valid @RequestBody AiChatRoleSaveMyReqVO createReqVO) {
        return success(chatRoleService.createChatRoleMy(createReqVO, getLoginIdDefaultNull()));
    }

    @PutMapping("/update-my")
    @Operation(summary = "更新【我的】聊天角色")
    public CommonResult<Boolean> updateChatRoleMy(
            @Valid @RequestBody AiChatRoleSaveMyReqVO updateReqVO) {
        chatRoleService.updateChatRoleMy(updateReqVO, getLoginIdDefaultNull());
        return success(true);
    }

    @DeleteMapping("/delete-my")
    @Operation(summary = "删除【我的】聊天角色")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteChatRoleMy(@RequestParam("id") Long id) {
        chatRoleService.deleteChatRoleMy(id, getLoginIdDefaultNull());
        return success(true);
    }

    @GetMapping("/category-list")
    @Operation(summary = "获得聊天角色的分类列表")
    public CommonResult<List<String>> getChatRoleCategoryList() {
        return success(chatRoleService.getChatRoleCategoryList());
    }

    // ========== 角色管理 ==========

    @PostMapping("/create")
    @Operation(summary = "创建聊天角色")
    public CommonResult<Long> createChatRole(@Valid @RequestBody AiChatRoleSaveReqVO createReqVO) {
        return success(chatRoleService.createChatRole(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新聊天角色")
    public CommonResult<Boolean> updateChatRole(
            @Valid @RequestBody AiChatRoleSaveReqVO updateReqVO) {
        chatRoleService.updateChatRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除聊天角色")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteChatRole(@RequestParam("id") Long id) {
        chatRoleService.deleteChatRole(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得聊天角色")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AiChatRoleRespVO> getChatRole(@RequestParam("id") Long id) {
        AiChatRoleDO chatRole = chatRoleService.getChatRole(id);
        return success(BeanUtil.toBean(chatRole, AiChatRoleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得聊天角色分页")
    public CommonResult<PageResult<AiChatRoleRespVO>> getChatRolePage(
            @Valid AiChatRolePageReqVO pageReqVO) {
        PageResult<AiChatRoleDO> pageResult = chatRoleService.getChatRolePage(pageReqVO);
        return success(PageUtils.toPageResult(pageResult, AiChatRoleRespVO.class));
    }
}
