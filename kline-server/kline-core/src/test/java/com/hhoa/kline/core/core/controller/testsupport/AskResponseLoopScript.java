package com.hhoa.kline.core.core.controller.testsupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 多轮「追问 → 用户回复」脚本数据：与 {@link ScriptedConversationApiHandler#multiTurnRounds(String...)} 及 {@link
 * PendingAskResponseDriver} 配合使用。
 *
 * <p>结构：前 {@link #ASK_ROUND_COUNT} 轮 API 均输出 {@code ask_followup_question}，最后一轮输出 {@code
 * attempt_completion}；用户在每轮追问后通过 {@link com.hhoa.kline.core.core.controller.TaskManager} 提交文本回复，最后对
 * COMPLETION_RESULT 点「是」。
 */
public final class AskResponseLoopScript {

    /** 与 {@link #assistantApiRounds()} 中 ask_followup 数量一致。 */
    public static final int ASK_ROUND_COUNT = 6;

    public static final String INITIAL_TASK_TEXT = "请按脚本完成六轮追问与确认，随后结束任务。";

    private AskResponseLoopScript() {}

    /** 每轮追问后注入的用户文本（长度须等于 {@link #ASK_ROUND_COUNT}）。 */
    public static List<String> userRepliesAfterEachFollowup() {
        return List.of("回复-A：继续。", "回复-B：继续。", "回复-C：继续。", "回复-D：继续。", "回复-E：继续。", "回复-F：最后一轮。");
    }

    /**
     * 第 1..{@link #ASK_ROUND_COUNT} 轮为 {@code ask_followup_question}，接着一轮 {@code
     * attempt_completion}，再一轮无工具纯文本（满足完成工具后仍可能触发的一次 API）。
     *
     * <p>长度 = {@link #ASK_ROUND_COUNT} + 2。
     */
    public static List<String> assistantApiRounds() {
        List<String> rounds = new ArrayList<>();
        for (int i = 1; i <= ASK_ROUND_COUNT; i++) {
            rounds.add(ClineToolXmlFragments.askFollowupQuestion("第" + i + "轮：请确认是否继续？"));
        }
        rounds.add(ClineToolXmlFragments.attemptCompletionResultOnly("六轮追问已完成，结束任务。"));
        // attempt_completion 确认后仍会再走至少一轮 API；若脚本无后续轮次会退化为「空流」并触发
        // PROCESS_ASSISTANT_RESPONSE_FAILED。此处追加无工具纯文本收尾轮。
        rounds.add("已完成，无需更多操作。");
        return List.copyOf(rounds);
    }
}
