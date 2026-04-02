package com.hhoa.kline.core.core.hooks;

/** Hook 输出流式回调 */
@FunctionalInterface
public interface HookStreamCallback {

    /**
     * 每当 hook 脚本输出一行时调用
     *
     * @param line 输出行
     * @param stream 流名称（"stdout" 或 "stderr"）
     * @param source hook 来源（"global" 或 "workspace"）
     * @param scriptPath hook 脚本路径
     */
    void onLine(String line, String stream, String source, String scriptPath);
}
