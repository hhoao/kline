package com.hhoa.kline.core.core.task.deps;

import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessResultPromise;
import lombok.Data;

@Data
public class ActiveBackgroundCommand {

    private TerminalProcessResultPromise process;
    private String command;
}
