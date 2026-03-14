package com.hhoa.kline.core.core.hosts;

import com.hhoa.kline.core.core.shared.proto.host.CloseAllDiffsRequest;
import com.hhoa.kline.core.core.shared.proto.host.CloseAllDiffsResponse;
import com.hhoa.kline.core.core.shared.proto.host.ExecuteCommandInTerminalRequest;
import com.hhoa.kline.core.core.shared.proto.host.ExecuteCommandInTerminalResponse;
import com.hhoa.kline.core.core.shared.proto.host.GetActiveEditorRequest;
import com.hhoa.kline.core.core.shared.proto.host.GetActiveEditorResponse;
import com.hhoa.kline.core.core.shared.proto.host.GetDiagnosticsRequest;
import com.hhoa.kline.core.core.shared.proto.host.GetDiagnosticsResponse;
import com.hhoa.kline.core.core.shared.proto.host.GetDocumentTextRequest;
import com.hhoa.kline.core.core.shared.proto.host.GetDocumentTextResponse;
import com.hhoa.kline.core.core.shared.proto.host.GetOpenTabsRequest;
import com.hhoa.kline.core.core.shared.proto.host.GetOpenTabsResponse;
import com.hhoa.kline.core.core.shared.proto.host.GetVisibleTabsRequest;
import com.hhoa.kline.core.core.shared.proto.host.GetVisibleTabsResponse;
import com.hhoa.kline.core.core.shared.proto.host.GetWorkspacePathsRequest;
import com.hhoa.kline.core.core.shared.proto.host.GetWorkspacePathsResponse;
import com.hhoa.kline.core.core.shared.proto.host.OpenDiffRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenDiffResponse;
import com.hhoa.kline.core.core.shared.proto.host.OpenFileRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenFileResponse;
import com.hhoa.kline.core.core.shared.proto.host.OpenMultiFileDiffRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenMultiFileDiffResponse;
import com.hhoa.kline.core.core.shared.proto.host.OpenSettingsRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenSettingsResponse;
import com.hhoa.kline.core.core.shared.proto.host.OpenTerminalRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenTerminalResponse;
import com.hhoa.kline.core.core.shared.proto.host.ReplaceTextRequest;
import com.hhoa.kline.core.core.shared.proto.host.ReplaceTextResponse;
import com.hhoa.kline.core.core.shared.proto.host.SaveDocumentRequest;
import com.hhoa.kline.core.core.shared.proto.host.SaveDocumentResponse;
import com.hhoa.kline.core.core.shared.proto.host.SaveOpenDocumentIfDirtyRequest;
import com.hhoa.kline.core.core.shared.proto.host.SaveOpenDocumentIfDirtyResponse;
import com.hhoa.kline.core.core.shared.proto.host.ScrollDiffRequest;
import com.hhoa.kline.core.core.shared.proto.host.ScrollDiffResponse;
import com.hhoa.kline.core.core.shared.proto.host.SelectedResources;
import com.hhoa.kline.core.core.shared.proto.host.SelectedResponse;
import com.hhoa.kline.core.core.shared.proto.host.ShowInputBoxRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowInputBoxResponse;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowOpenDialogueRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowSaveDialogRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowSaveDialogResponse;
import com.hhoa.kline.core.core.shared.proto.host.ShowTextDocumentRequest;
import com.hhoa.kline.core.core.shared.proto.host.TextEditorInfo;
import com.hhoa.kline.core.core.shared.proto.host.TruncateDocumentRequest;
import com.hhoa.kline.core.core.shared.proto.host.TruncateDocumentResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 主机提供者类型定义
 *
 * <p>包含主机桥接客户端提供者接口和流式回调接口
 */
public class HostProviderTypes {

    /**
     * 主机桥接客户端提供者接口
     *
     * <p>用于提供主机桥接客户端服务的接口，包含工作区、环境、窗口和差异服务客户端
     */
    public interface HostBridgeClientProvider {
        /**
         * 获取工作区服务客户端
         *
         * @return 工作区服务客户端接口
         */
        WorkspaceServiceClientInterface getWorkspaceClient();

        /**
         * 获取窗口服务客户端
         *
         * @return 窗口服务客户端接口
         */
        WindowServiceClientInterface getWindowClient();

        /**
         * 获取差异服务客户端
         *
         * @return 差异服务客户端接口
         */
        DiffServiceClientInterface getDiffClient();
    }

    /**
     * 流式请求回调接口
     *
     * <p>用于处理流式请求的回调，包括响应、错误和完成回调
     *
     * @param <T> 响应类型
     */
    public interface StreamingCallbacks<T> {
        /**
         * 响应回调
         *
         * @param response 响应对象
         */
        void onResponse(T response);

        /**
         * 错误回调（可选）
         *
         * @param error 错误对象
         */
        default void onError(Throwable error) {
            // 默认实现为空，子类可以覆盖
        }

        /** 完成回调（可选） */
        default void onComplete() {
            // 默认实现为空，子类可以覆盖
        }
    }

    /**
     * 工作区服务客户端接口
     *
     * <p>提供工作区相关的服务方法
     */
    public interface WorkspaceServiceClientInterface {
        /**
         * 获取工作区路径列表
         *
         * @param request 请求对象
         * @return 工作区路径响应
         */
        CompletableFuture<GetWorkspacePathsResponse> getWorkspacePaths(
                GetWorkspacePathsRequest request);

        /**
         * 保存打开的文档（如果已修改）
         *
         * @param request 请求对象
         * @return 保存响应
         */
        CompletableFuture<SaveOpenDocumentIfDirtyResponse> saveOpenDocumentIfDirty(
                SaveOpenDocumentIfDirtyRequest request);

        /**
         * 获取诊断信息
         *
         * @param request 请求对象
         * @return 诊断响应
         */
        CompletableFuture<GetDiagnosticsResponse> getDiagnostics(GetDiagnosticsRequest request);

        /**
         * 打开终端面板
         *
         * @param request 请求对象
         * @return 打开终端响应
         */
        CompletableFuture<OpenTerminalResponse> openTerminalPanel(OpenTerminalRequest request);

        /**
         * 在终端中执行命令
         *
         * @param request 请求对象
         * @return 执行命令响应
         */
        CompletableFuture<ExecuteCommandInTerminalResponse> executeCommandInTerminal(
                ExecuteCommandInTerminalRequest request);
    }

    /**
     * 窗口服务客户端接口
     *
     * <p>提供窗口和编辑器相关的服务方法
     */
    public interface WindowServiceClientInterface {
        /**
         * 显示文本文档
         *
         * @param request 请求对象
         * @return 文本编辑器信息
         */
        CompletableFuture<TextEditorInfo> showTextDocument(ShowTextDocumentRequest request);

        /**
         * 显示打开对话框
         *
         * @param request 请求对象
         * @return 选中的资源
         */
        CompletableFuture<SelectedResources> showOpenDialogue(ShowOpenDialogueRequest request);

        /**
         * 显示消息
         *
         * @param request 请求对象
         * @return 选中的响应
         */
        CompletableFuture<SelectedResponse> showMessage(ShowMessageRequest request);

        /**
         * 显示输入框
         *
         * @param request 请求对象
         * @return 输入框响应
         */
        CompletableFuture<ShowInputBoxResponse> showInputBox(ShowInputBoxRequest request);

        /**
         * 显示保存对话框
         *
         * @param request 请求对象
         * @return 保存对话框响应
         */
        CompletableFuture<ShowSaveDialogResponse> showSaveDialog(ShowSaveDialogRequest request);

        /**
         * 打开文件
         *
         * @param request 请求对象
         * @return 打开文件响应
         */
        CompletableFuture<OpenFileResponse> openFile(OpenFileRequest request);

        /**
         * 打开设置
         *
         * @param request 请求对象
         * @return 打开设置响应
         */
        CompletableFuture<OpenSettingsResponse> openSettings(OpenSettingsRequest request);

        /**
         * 获取打开的标签页
         *
         * @param request 请求对象
         * @return 打开的标签页响应
         */
        CompletableFuture<GetOpenTabsResponse> getOpenTabs(GetOpenTabsRequest request);

        /**
         * 获取可见的标签页
         *
         * @param request 请求对象
         * @return 可见的标签页响应
         */
        CompletableFuture<GetVisibleTabsResponse> getVisibleTabs(GetVisibleTabsRequest request);

        /**
         * 获取活动编辑器
         *
         * @param request 请求对象
         * @return 活动编辑器响应
         */
        CompletableFuture<GetActiveEditorResponse> getActiveEditor(GetActiveEditorRequest request);
    }

    /**
     * 差异服务客户端接口
     *
     * <p>提供差异视图相关的服务方法
     */
    public interface DiffServiceClientInterface {
        /**
         * 打开差异视图
         *
         * @param request 请求对象
         * @return 打开差异响应
         */
        CompletableFuture<OpenDiffResponse> openDiff(OpenDiffRequest request);

        /**
         * 获取文档文本
         *
         * @param request 请求对象
         * @return 文档文本响应
         */
        CompletableFuture<GetDocumentTextResponse> getDocumentText(GetDocumentTextRequest request);

        /**
         * 替换文本
         *
         * @param request 请求对象
         * @return 替换文本响应
         */
        CompletableFuture<ReplaceTextResponse> replaceText(ReplaceTextRequest request);

        /**
         * 滚动差异视图
         *
         * @param request 请求对象
         * @return 滚动响应
         */
        CompletableFuture<ScrollDiffResponse> scrollDiff(ScrollDiffRequest request);

        /**
         * 截断文档
         *
         * @param request 请求对象
         * @return 截断文档响应
         */
        CompletableFuture<TruncateDocumentResponse> truncateDocument(
                TruncateDocumentRequest request);

        /**
         * 保存文档
         *
         * @param request 请求对象
         * @return 保存文档响应
         */
        CompletableFuture<SaveDocumentResponse> saveDocument(SaveDocumentRequest request);

        /**
         * 关闭所有差异视图
         *
         * @param request 请求对象
         * @return 关闭所有差异响应
         */
        CompletableFuture<CloseAllDiffsResponse> closeAllDiffs(CloseAllDiffsRequest request);

        /**
         * 打开多文件差异视图
         *
         * @param request 请求对象
         * @return 打开多文件差异响应
         */
        CompletableFuture<OpenMultiFileDiffResponse> openMultiFileDiff(
                OpenMultiFileDiffRequest request);
    }
}
