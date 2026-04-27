package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Executes a group of finalized tool uses with Claude Code style queue semantics.
 *
 * <p>Concurrency-safe tools may run in parallel with each other. Non-safe tools keep exclusive,
 * ordered execution. Results are returned in input order.
 */
public class ToolExecutionBatchCoordinator {
    private final ToolExecutor toolExecutor;

    public ToolExecutionBatchCoordinator(ToolExecutor toolExecutor) {
        this.toolExecutor = toolExecutor;
    }

    public BatchResult execute(
            List<ToolUse> toolUses, Function<ToolUse, ToolContext> contextFactory) {
        if (toolUses == null || toolUses.isEmpty()) {
            return new BatchResult(List.of(), false);
        }

        List<ExecutionResult> completed = new ArrayList<>();
        boolean hasPendingAsk = false;
        int index = 0;
        PreparedTool carriedPreparedTool = null;
        while (index < toolUses.size()) {
            PreparedTool prepared =
                    carriedPreparedTool != null
                            ? carriedPreparedTool
                            : prepare(toolUses.get(index), contextFactory);
            carriedPreparedTool = null;
            if (!prepared.concurrencySafe()) {
                ExecutionResult result = executePrepared(prepared);
                completed.add(result);
                if (result.result() instanceof ToolExecuteResult.PendingAsk) {
                    hasPendingAsk = true;
                    break;
                }
                index++;
                continue;
            }

            List<PreparedTool> safeRun = new ArrayList<>();
            safeRun.add(prepared);
            index++;
            while (index < toolUses.size()) {
                PreparedTool candidate = prepare(toolUses.get(index), contextFactory);
                if (!candidate.concurrencySafe()) {
                    carriedPreparedTool = candidate;
                    break;
                }
                safeRun.add(candidate);
                index++;
            }
            List<ExecutionResult> results = executeConcurrentRun(safeRun);
            completed.addAll(results);
            if (results.stream()
                    .anyMatch(result -> result.result() instanceof ToolExecuteResult.PendingAsk)) {
                hasPendingAsk = true;
                break;
            }
        }

        return new BatchResult(completed, hasPendingAsk);
    }

    private PreparedTool prepare(ToolUse toolUse, Function<ToolUse, ToolContext> contextFactory) {
        ToolContext context = contextFactory.apply(toolUse);
        ToolHandler handler = toolExecutor.getHandler(toolUse.getName());
        boolean concurrencySafe = handler != null && handler.isConcurrencySafe(toolUse, context);
        String description =
                handler != null ? handler.getDescription(toolUse) : "[" + toolUse.getName() + "]";
        return new PreparedTool(toolUse, context, concurrencySafe, description);
    }

    private List<ExecutionResult> executeConcurrentRun(List<PreparedTool> tools) {
        List<CompletableFuture<ExecutionResult>> futures =
                tools.stream()
                        .map(tool -> CompletableFuture.supplyAsync(() -> executePrepared(tool)))
                        .toList();

        List<ExecutionResult> results = new ArrayList<>();
        for (CompletableFuture<ExecutionResult> future : futures) {
            results.add(future.join());
        }
        return results;
    }

    private ExecutionResult executePrepared(PreparedTool prepared) {
        ToolExecuteResult result = toolExecutor.executeTool(prepared.toolUse(), prepared.context());
        return new ExecutionResult(prepared.toolUse(), result, prepared.description());
    }

    private record PreparedTool(
            ToolUse toolUse, ToolContext context, boolean concurrencySafe, String description) {}

    public record ExecutionResult(
            ToolUse toolUse, ToolExecuteResult result, String toolDescription) {}

    public record BatchResult(List<ExecutionResult> completed, boolean hasPendingAsk) {}
}
