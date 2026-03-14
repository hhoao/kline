package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TerminalExecutionMode {
    VSCODE_TERMINAL("vscodeTerminal"),
    BACKGROUND_EXEC("backgroundExec");

    private final String value;

    TerminalExecutionMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static TerminalExecutionMode fromValue(String value) {
        if (value == null) {
            return VSCODE_TERMINAL;
        }

        for (TerminalExecutionMode mode : TerminalExecutionMode.values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }

        return VSCODE_TERMINAL;
    }
}
