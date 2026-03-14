package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.service.AiFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Cline 文件服务")
@RestController
@RequestMapping("/api/cline/file")
@RequiredArgsConstructor
public class AiFileController {

    private final AiFileService aiFileService;

    @Operation(summary = "复制到剪贴板")
    @PostMapping("/copy-to-clipboard")
    public CommonResult<Void> copyToClipboard(@Valid @RequestBody String request) {
        aiFileService.copyToClipboard(request);
        return success(null);
    }

    @Operation(summary = "打开文件")
    @PostMapping("/open")
    public CommonResult<Void> openFile(@Valid @RequestBody String request) {
        aiFileService.openFile(request);
        return success(null);
    }

    @Operation(summary = "打开图片")
    @PostMapping("/open-image")
    public CommonResult<Void> openImage(@Valid @RequestBody String request) {
        aiFileService.openImage(request);
        return success(null);
    }

    @Operation(summary = "打开提及")
    @PostMapping("/open-mention")
    public CommonResult<Void> openMention(@Valid @RequestBody String request) {
        aiFileService.openMention(request);
        return success(null);
    }

    @Operation(summary = "删除规则文件")
    @PostMapping("/delete-rule-file")
    public CommonResult<String> deleteRuleFile() {
        String response = aiFileService.deleteRuleFile();
        return success(response);
    }

    @Operation(summary = "创建规则文件")
    @PostMapping("/create-rule-file")
    public CommonResult<String> createRuleFile() {
        String response = aiFileService.createRuleFile();
        return success(response);
    }

    @Operation(summary = "搜索提交")
    @PostMapping("/search-commits")
    public CommonResult<String> searchCommits(@Valid @RequestBody String request) {
        String response = aiFileService.searchCommits(request);
        return success(response);
    }

    @Operation(summary = "选择文件")
    @PostMapping("/select")
    public CommonResult<List<String>> selectFiles(@Valid @RequestBody Boolean request) {
        List<String> response = aiFileService.selectFiles(request);
        return success(response);
    }

    @Operation(summary = "获取相对路径")
    @GetMapping("/relative-paths")
    public CommonResult<String> getRelativePaths() {
        String response = aiFileService.getRelativePaths();
        return success(response);
    }

    @Operation(summary = "搜索文件")
    @PostMapping("/search")
    public CommonResult<String> searchFiles() {
        String response = aiFileService.searchFiles();
        return success(response);
    }

    @Operation(summary = "切换 Cline 规则")
    @PostMapping("/toggle-cline-rule")
    public CommonResult<String> toggleClineRule() {
        String response = aiFileService.toggleClineRule();
        return success(response);
    }

    @Operation(summary = "切换 Cursor 规则")
    @PostMapping("/toggle-cursor-rule")
    public CommonResult<String> toggleCursorRule() {
        String response = aiFileService.toggleCursorRule();
        return success(response);
    }

    @Operation(summary = "切换 Windsurf 规则")
    @PostMapping("/toggle-windsurf-rule")
    public CommonResult<String> toggleWindsurfRule() {
        String response = aiFileService.toggleWindsurfRule();
        return success(response);
    }

    @Operation(summary = "刷新规则")
    @PostMapping("/refresh-rules")
    public CommonResult<String> refreshRules() {
        String response = aiFileService.refreshRules();
        return success(response);
    }

    @Operation(summary = "打开磁盘对话历史")
    @PostMapping("/open-disk-conversation-history")
    public CommonResult<Void> openDiskConversationHistory(@Valid @RequestBody String request) {
        aiFileService.openDiskConversationHistory(request);
        return success(null);
    }

    @Operation(summary = "切换工作流")
    @PostMapping("/toggle-workflow")
    public CommonResult<String> toggleWorkflow() {
        String response = aiFileService.toggleWorkflow();
        return success(response);
    }

    @Operation(summary = "检查文件是否存在（相对路径）")
    @PostMapping("/exists")
    public CommonResult<Boolean> ifFileExistsRelativePath(@Valid @RequestBody String request) {
        Boolean response = aiFileService.ifFileExistsRelativePath(request);
        return success(response);
    }

    @Operation(summary = "打开文件（相对路径）")
    @PostMapping("/open-relative")
    public CommonResult<Void> openFileRelativePath(@Valid @RequestBody String request) {
        aiFileService.openFileRelativePath(request);
        return success(null);
    }

    @Operation(summary = "打开焦点链文件")
    @PostMapping("/open-focus-chain")
    public CommonResult<Void> openFocusChainFile(@Valid @RequestBody String request) {
        aiFileService.openFocusChainFile(request);
        return success(null);
    }
}
