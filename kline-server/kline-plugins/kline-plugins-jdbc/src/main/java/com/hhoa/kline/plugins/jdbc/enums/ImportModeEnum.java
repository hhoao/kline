package com.hhoa.kline.plugins.jdbc.enums;

import com.hhoa.ai.kline.commons.core.ArrayValuable;
import java.util.Arrays;
import lombok.Getter;

/**
 * 导入模式枚举
 *
 * @author hhoa
 * @date 2025/10/10
 */
@Getter
public enum ImportModeEnum implements ArrayValuable<Integer> {
    INSERT(0, "INSERT", "插入模式：直接插入新记录"),
    UPDATE(1, "UPDATE", "更新模式：根据标识字段更新现有记录"),
    UPSERT(2, "UPSERT", "插入或更新模式：先尝试插入，失败则更新");

    public static final Integer[] ARRAYS =
            Arrays.stream(values()).map(ImportModeEnum::getCode).toArray(Integer[]::new);

    /** 状态值 */
    private final Integer code;

    /** 模式名称 */
    private final String name;

    /** 描述 */
    private final String description;

    ImportModeEnum(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 根据名称获取导入模式枚举
     *
     * @param name 模式名称
     * @return 导入模式枚举，未找到时返回null
     */
    public static ImportModeEnum getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        name = name.trim().toUpperCase();
        for (ImportModeEnum mode : values()) {
            if (mode.getName().equals(name)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 根据代码获取导入模式枚举
     *
     * @param code 代码
     * @return 导入模式枚举，未找到时返回null
     */
    public static ImportModeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (ImportModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 获取默认导入模式
     *
     * @return 默认导入模式（INSERT）
     */
    public static ImportModeEnum getDefault() {
        return INSERT;
    }

    /**
     * 验证导入模式名称是否有效
     *
     * @param name 模式名称
     * @return 是否有效
     */
    public static boolean isValid(String name) {
        return getByName(name) != null;
    }
}
