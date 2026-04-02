package com.hhoa.kline.core.core.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 组合多个 hook runner 并行执行，合并结果。
 *
 * <p>用于多根工作区场景，全局 hook 和工作区 hook 可以同时存在。
 *
 * <p>行为：
 *
 * <ul>
 *   <li>所有 hook 并发执行
 *   <li>任意一个 hook 返回 cancel: true，合并结果即为 cancel: true
 *   <li>所有 contextModification 以双换行连接
 *   <li>所有 errorMessage 以单换行连接
 * </ul>
 */
public class CombinedHookRunner extends HookRunner {

    private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool(
                    r -> {
                        Thread t = new Thread(r, "hook-combined-runner");
                        t.setDaemon(true);
                        return t;
                    });

    private final List<HookRunner> runners;

    public CombinedHookRunner(HookName hookName, List<HookRunner> runners) {
        super(hookName);
        this.runners = runners;
    }

    @Override
    public HookOutput execute(HookInput input) {
        // 并行执行所有 runner
        List<CompletableFuture<HookOutput>> futures =
                runners.stream()
                        .map(
                                runner ->
                                        CompletableFuture.supplyAsync(
                                                () -> runner.execute(input), EXECUTOR))
                        .collect(Collectors.toList());

        // 等待所有完成
        List<HookOutput> results = new ArrayList<>();
        for (CompletableFuture<HookOutput> future : futures) {
            try {
                results.add(future.join());
            } catch (Exception e) {
                // Fail-open: 单个 hook 失败不阻止整体
                if (e.getCause() instanceof HookExecutionError) {
                    throw (HookExecutionError) e.getCause();
                }
                results.add(HookOutput.success());
            }
        }

        // 合并结果
        boolean cancel = results.stream().anyMatch(HookOutput::isCancel);

        String contextModification =
                results.stream()
                        .map(HookOutput::getContextModification)
                        .filter(mod -> mod != null && !mod.trim().isEmpty())
                        .map(String::trim)
                        .collect(Collectors.joining("\n\n"));

        String errorMessage =
                results.stream()
                        .map(HookOutput::getErrorMessage)
                        .filter(msg -> msg != null && !msg.trim().isEmpty())
                        .map(String::trim)
                        .collect(Collectors.joining("\n"));

        return HookOutput.builder()
                .cancel(cancel)
                .contextModification(contextModification)
                .errorMessage(errorMessage)
                .build();
    }
}
