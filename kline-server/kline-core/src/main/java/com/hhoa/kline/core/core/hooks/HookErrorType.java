package com.hhoa.kline.core.core.hooks;

/** Hook 执行过程中可能出现的错误类型 */
public enum HookErrorType {
    /** Hook 执行超时 */
    TIMEOUT("timeout"),
    /** Hook 输出 JSON 验证失败 */
    VALIDATION("validation"),
    /** Hook 脚本执行失败（非零退出码、崩溃等） */
    EXECUTION("execution"),
    /** Hook 被用户取消 */
    CANCELLATION("cancellation");

    private final String value;

    HookErrorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
