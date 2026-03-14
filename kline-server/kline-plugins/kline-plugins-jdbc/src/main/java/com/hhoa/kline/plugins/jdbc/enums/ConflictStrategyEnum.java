package com.hhoa.kline.plugins.jdbc.enums;

import com.hhoa.ai.kline.commons.core.ArrayValuable;
import java.util.Arrays;
import lombok.Getter;

/**
 * 冲突处理策略枚举
 *
 * @author hhoa
 * @date 2025/01/15
 */
@Getter
public enum ConflictStrategyEnum implements ArrayValuable<String> {
    SKIP("SKIP", "跳过冲突记录，不进行任何操作"),
    OVERWRITE("OVERWRITE", "覆盖冲突记录，使用新数据替换现有数据"),
    ERROR("ERROR", "遇到冲突时抛出错误，停止处理");

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(ConflictStrategyEnum::getCode).toArray(String[]::new);

    /** 策略代码 */
    private final String code;

    /** 描述 */
    private final String description;

    ConflictStrategyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

    /**
     * 根据代码获取冲突策略枚举
     *
     * @param code 策略代码
     * @return 冲突策略枚举，未找到时返回null
     */
    public static ConflictStrategyEnum getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        code = code.trim().toUpperCase();
        for (ConflictStrategyEnum strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * 获取默认冲突策略
     *
     * @return 默认冲突策略（SKIP）
     */
    public static ConflictStrategyEnum getDefault() {
        return SKIP;
    }

    /**
     * 验证冲突策略代码是否有效
     *
     * @param code 策略代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}
