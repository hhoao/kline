package com.hhoa.kline.core.core.controller.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.hhoa.kline.core.core.api.TaskContextHolder;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.MessageRole;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUseContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.systemprompt.DefaultSystemPromptServiceFactory;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.TaskStatus;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.message.PartialMessage;
import com.hhoa.kline.core.subscription.message.StateMessage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.Disposable;

/**
 * 本地任务集成测试基类：固定 {@link TaskContextHolder}、订阅 {@link PartialMessage} 并合并流式分片、 提供常用断言与 {@link TaskV2}
 * 等待工具。
 *
 * <p>与 {@link FixedTaskContext} 的用户 id 一致，默认向 {@link DefaultSubscriptionManager} 用户 {@value
 * #DEFAULT_SUBSCRIPTION_USER_ID} 建立订阅。
 */
public abstract class AbstractLocalTaskIntegrationTest {

    /** 与 {@link FixedTaskContext} 构造函数中的 userId 一致。 */
    public static final String DEFAULT_SUBSCRIPTION_USER_ID = "1";

    /** 等待任务进入 {@link TaskStatus#TASK_COMPLETE} 的默认超时（毫秒）。 */
    protected static final long DEFAULT_TASK_AWAIT_MS = 60_000L;

    /** 订阅收到的每一条 {@link PartialMessage}（含 increment 为 null 的事件）。 */
    protected final List<PartialMessage> partialMessages = new ArrayList<>();

    /**
     * 按「逻辑消息」合并 increment：{@code isUpdatingPreviousPartial == false}
     * 表示新气泡开始，此前缓冲内容作为一整条加入本列表并清空；同一条内的增量继续 append。
     */
    protected final List<String> mergedIncrementSeries = new ArrayList<>();

    private final StringBuffer currentMergeBuffer = new StringBuffer();

    /** {@link StateMessage} 推送次数，用于粗粒度验证任务侧是否在推进状态。 */
    protected final AtomicInteger statePushCount = new AtomicInteger();

    @TempDir protected Path baseDir;

    protected SystemPromptService systemPromptService;

    private Disposable subscriptionDisposable;

    private void recordPartialMessage(PartialMessage pm) {
        partialMessages.add(pm);
        if (!Boolean.TRUE.equals(pm.getIsUpdatingPreviousPartial())) {
            if (currentMergeBuffer.length() > 0) {
                mergedIncrementSeries.add(currentMergeBuffer.toString());
                currentMergeBuffer.setLength(0);
            }
        }
        String inc = pm.getIncrementContent();
        if (inc != null) {
            currentMergeBuffer.append(inc);
        }
    }

    /** 将最后一段仍在缓冲区的合并文本推入 {@link #mergedIncrementSeries}。 */
    protected final void flushPendingMergedIncrements() {
        if (currentMergeBuffer.length() > 0) {
            mergedIncrementSeries.add(currentMergeBuffer.toString());
            currentMergeBuffer.setLength(0);
        }
    }

    /**
     * 校验首条 USER 消息中 {@code <task>...</task>} 内文本与期望值完全一致（忽略标签内首尾空白）。
     *
     * <p>首条 user 消息通常还包含环境块等，故不能对 {@link #plainText(MessageParam)} 整段做 equals。
     */
    protected static void assertFirstUserTaskContentEquals(TaskV2 task, String expectedTaskText) {
        MessageParam firstUser =
                task.getMessageStateHandler().getApiConversationHistory().stream()
                        .filter(p -> p.getRole() == MessageRole.USER)
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("api conversation has no USER message"));
        assertThat(extractTaskInnerText(plainText(firstUser))).isEqualTo(expectedTaskText);
    }

    protected static String extractTaskInnerText(String plain) {
        String open = "<task>";
        String close = "</task>";
        int i = plain.indexOf(open);
        if (i < 0) {
            return plain.trim();
        }
        int start = i + open.length();
        int j = plain.indexOf(close, start);
        if (j < 0) {
            return plain.trim();
        }
        return plain.substring(start, j).trim();
    }

    private static String plainText(MessageParam p) {
        if (p == null || p.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (UserContentBlock b : p.getContent()) {
            if (b instanceof TextContentBlock tb && tb.getText() != null) {
                sb.append(tb.getText());
            } else if (b instanceof ToolUseContentBlock tu && tu.getInput() != null) {
                sb.append(tu.getInput().toString());
            } else if (b != null) {
                sb.append(b.toString());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 集成测试常用：关闭 checkpoint、可选 yolo、保证 browser 设置非空。 */
    protected static void applyIntegrationSettings(StateManager stateManager, boolean yolo) {
        stateManager.getSettings().setEnableCheckpointsSetting(false);
        stateManager.getSettings().setYoloModeToggled(yolo);
        if (stateManager.getSettings().getBrowserSettings() == null) {
            stateManager.getSettings().setBrowserSettings(new BrowserSettings());
        }
    }

    @BeforeEach
    void setUpLocalTaskIntegrationBase() {
        TaskContextHolder.clear();
        TaskContextHolder.set(new FixedTaskContext(1L));
        partialMessages.clear();
        mergedIncrementSeries.clear();
        currentMergeBuffer.setLength(0);
        statePushCount.set(0);

        SubscriptionManager subscriptionManager = DefaultSubscriptionManager.getInstance();
        subscriptionDisposable =
                subscriptionManager
                        .subscribe(DEFAULT_SUBSCRIPTION_USER_ID)
                        .handle(
                                (message, sink) -> {
                                    if (message instanceof StateMessage) {
                                        statePushCount.incrementAndGet();
                                    } else if (message instanceof PartialMessage pm) {
                                        recordPartialMessage(pm);
                                    }
                                    sink.next(message);
                                })
                        .subscribe();

        systemPromptService = DefaultSystemPromptServiceFactory.createSystemPromptService();
    }

    @AfterEach
    void tearDownLocalTaskIntegrationBase() {
        if (subscriptionDisposable != null) {
            subscriptionDisposable.dispose();
            subscriptionDisposable = null;
        }
        DefaultSubscriptionManager.getInstance().shutdown(DEFAULT_SUBSCRIPTION_USER_ID);
        TaskContextHolder.clear();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("BusyWait")
    protected final void awaitTaskComplete(TaskV2 task, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            TaskStatus s = task.getState();
            if (s == TaskStatus.TASK_COMPLETE) {
                return;
            }
            if (s == TaskStatus.ABORT) {
                fail("expected TASK_COMPLETE, got ABORT");
            }
            Thread.sleep(20L);
        }
        assertThat(task.getState())
                .as("timeout after %d ms waiting for TASK_COMPLETE", timeoutMs)
                .isEqualTo(TaskStatus.TASK_COMPLETE);
    }
}
