package com.hhoa.kline.core.core.tools.types;

import lombok.Getter;
import lombok.Setter;

/** 工具执行状态基类。每个需要跨 ask 保存状态的 ToolHandler 应创建对应的子类。 phase 表示当前执行阶段。 */
@Getter
@Setter
public class ToolState {
    private String name;
    private int phase;
}
