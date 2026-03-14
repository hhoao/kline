package com.hhoa.kline.core.core.hosts;

import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;

@Getter
public class HostProvider {
    private final DiffViewProvider diffViewProvider;
    private final HostProviderTypes.HostBridgeClientProvider hostBridge;
    private final LogToChannel logToChannel;
    private final Supplier<CompletableFuture<String>> getCallbackUrl;
    private final Function<String, CompletableFuture<String>> getBinaryLocation;

    public HostProvider(
            DiffViewProvider diffViewProvider,
            HostProviderTypes.HostBridgeClientProvider hostBridge,
            LogToChannel logToChannel,
            Supplier<CompletableFuture<String>> getCallbackUrl,
            Function<String, CompletableFuture<String>> getBinaryLocation) {
        this.diffViewProvider = diffViewProvider;
        this.hostBridge = hostBridge;
        this.logToChannel = logToChannel;
        this.getCallbackUrl = getCallbackUrl;
        this.getBinaryLocation = getBinaryLocation;
    }

    /**
     * 记录日志到通道
     *
     * @param message 日志消息
     */
    public void logToChannel(String message) {
        logToChannel.log(message);
    }

    /**
     * 获取回调 URL
     *
     * @return 回调 URL 的 CompletableFuture
     */
    public CompletableFuture<String> getCallbackUrl() {
        return getCallbackUrl.get();
    }

    @FunctionalInterface
    public interface WebviewProviderCreator {
        Object create();
    }

    @FunctionalInterface
    public interface DiffViewProviderCreator {
        DiffViewProvider create();
    }

    @FunctionalInterface
    public interface LogToChannel {
        void log(String message);
    }
}
