package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolExecutionBatchCoordinatorTest {
    @Test
    void concurrencySafeToolsExecuteInParallelAndReturnInInputOrder() {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        FakeToolHandler alpha = new FakeToolHandler("alpha", active, maxActive);
        FakeToolHandler beta = new FakeToolHandler("beta", active, maxActive);
        FakeToolExecutor executor = new FakeToolExecutor(alpha, beta);
        ToolExecutionBatchCoordinator coordinator = new ToolExecutionBatchCoordinator(executor);

        ToolExecutionBatchCoordinator.BatchResult result =
                coordinator.execute(
                        List.of(tool("alpha"), tool("beta")),
                        toolUse -> ToolContext.builder().toolState(new ToolState()).build());

        assertEquals(2, result.completed().size());
        assertEquals("alpha result", text(result.completed().get(0).result()));
        assertEquals("beta result", text(result.completed().get(1).result()));
        assertTrue(maxActive.get() > 1, "concurrency-safe tools should overlap");
    }

    @Test
    void pendingAskStopsLaterTools() {
        AtomicInteger executedLaterTools = new AtomicInteger();
        PendingToolHandler pending = new PendingToolHandler();
        CountingToolHandler later = new CountingToolHandler(executedLaterTools);
        FakeToolExecutor executor = new FakeToolExecutor(pending, later);
        ToolExecutionBatchCoordinator coordinator = new ToolExecutionBatchCoordinator(executor);

        ToolExecutionBatchCoordinator.BatchResult result =
                coordinator.execute(
                        List.of(tool("pending"), tool("later")),
                        toolUse -> ToolContext.builder().toolState(new ToolState()).build());

        assertEquals(1, result.completed().size());
        assertTrue(result.hasPendingAsk());
        assertEquals(0, executedLaterTools.get());
    }

    private static ToolUse tool(String name) {
        ToolUse toolUse = new ToolUse();
        toolUse.setName(name);
        toolUse.setParams(Map.of());
        toolUse.setPartial(false);
        return toolUse;
    }

    private static String text(ToolExecuteResult result) {
        ToolExecuteResult.Immediate immediate = (ToolExecuteResult.Immediate) result;
        UserContentBlock block = immediate.blocks().getFirst();
        return ((TextContentBlock) block).getText();
    }

    private static final class FakeToolExecutor implements ToolExecutor {
        private final Map<String, ToolHandler> handlers = new ConcurrentHashMap<>();

        private FakeToolExecutor(ToolHandler... handlers) {
            for (ToolHandler handler : handlers) {
                this.handlers.put(handler.getName(), handler);
            }
        }

        @Override
        public ToolRegistry getRegistry() {
            return new ToolRegistry() {
                @Override
                public ToolHandler getHandler(String toolName) {
                    return handlers.get(toolName);
                }

                @Override
                public boolean has(String toolName) {
                    return handlers.containsKey(toolName);
                }
            };
        }

        @Override
        public ToolExecuteResult executeTool(ToolUse block, ToolContext config) {
            return handlers.get(block.getName()).execute(config, block);
        }

        @Override
        public ToolExecuteResult resume(
                PendingAskToken askToken, AskResult askResult, ToolContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ToolState getOrCreateToolState(String name) {
            return new ToolState();
        }
    }

    private static final class FakeToolHandler implements ToolHandler {
        private final String name;
        private final AtomicInteger active;
        private final AtomicInteger maxActive;

        private FakeToolHandler(String name, AtomicInteger active, AtomicInteger maxActive) {
            this.name = name;
            this.active = active;
            this.maxActive = maxActive;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription(ToolUse block) {
            return name;
        }

        @Override
        public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {}

        @Override
        public ToolExecuteResult execute(ToolContext context, ToolUse block) {
            int running = active.incrementAndGet();
            maxActive.accumulateAndGet(running, Math::max);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
            }
            return new ToolExecuteResult.Immediate(List.of(new TextContentBlock(name + " result")));
        }

        @Override
        public ClineToolSpec getClineToolSpec() {
            return null;
        }

        @Override
        public boolean isConcurrencySafe(ToolUse block, ToolContext context) {
            return true;
        }
    }

    private static final class PendingToolHandler implements ToolHandler {
        @Override
        public String getName() {
            return "pending";
        }

        @Override
        public String getDescription(ToolUse block) {
            return "pending";
        }

        @Override
        public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {}

        @Override
        public ToolExecuteResult execute(ToolContext context, ToolUse block) {
            return new ToolExecuteResult.PendingAsk(
                    new PendingAskToken.ToolUsePendingAskToken(
                            "pending-id", "task", "pending", null, "", null, block));
        }

        @Override
        public ClineToolSpec getClineToolSpec() {
            return null;
        }
    }

    private static final class CountingToolHandler implements ToolHandler {
        private final AtomicInteger count;

        private CountingToolHandler(AtomicInteger count) {
            this.count = count;
        }

        @Override
        public String getName() {
            return "later";
        }

        @Override
        public String getDescription(ToolUse block) {
            return "later";
        }

        @Override
        public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {}

        @Override
        public ToolExecuteResult execute(ToolContext context, ToolUse block) {
            count.incrementAndGet();
            return new ToolExecuteResult.Immediate(List.of(new TextContentBlock("later result")));
        }

        @Override
        public ClineToolSpec getClineToolSpec() {
            return null;
        }
    }
}
