package com.hhoa.kline.core.core.mentions;

import java.util.concurrent.CompletableFuture;

public interface UrlContentFetcher {
    /**
     * 启动浏览器（如果需要）
     *
     * @return CompletableFuture 异步操作
     */
    CompletableFuture<Void> launchBrowser();

    /**
     * 关闭浏览器
     *
     * @return CompletableFuture 异步操作
     */
    CompletableFuture<Void> closeBrowser();

    /**
     * 将 URL 内容转换为 Markdown 格式 必须在调用 launchBrowser() 之后调用
     *
     * @param url URL 地址
     * @return CompletableFuture 异步返回 Markdown 格式的内容
     */
    CompletableFuture<String> urlToMarkdown(String url);
}
