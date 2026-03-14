package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 验证结果模型 Represents the result of validating file content or configuration */
@Data
@NoArgsConstructor
public class ValidationResult {

    /** 是否有效 */
    private boolean valid = true;

    /** 错误列表 */
    private List<String> errors = new ArrayList<>();

    /** 警告列表 */
    private List<String> warnings = new ArrayList<>();

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    // Static factory methods
    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    public static ValidationResult failure(String error) {
        ValidationResult result = new ValidationResult(false);
        result.addError(error);
        return result;
    }

    public static ValidationResult failure(List<String> errors) {
        ValidationResult result = new ValidationResult(false);
        result.setErrors(errors);
        return result;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
        if (errors != null && !errors.isEmpty()) {
            this.valid = false;
        }
    }

    /** 添加错误 */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false;
    }

    /** 添加警告 */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /** 检查是否有错误 */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /** 检查是否有警告 */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /** 获取错误数量 */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    /** 获取警告数量 */
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
}
