package com.hhoa.kline.web.constants;

import com.hhoa.ai.kline.commons.exception.ErrorCode;

/**
 * Infra 错误码枚举类
 *
 * <p>infra 系统，使用 1-001-000-000 段
 */
public interface ErrorCodeConstants {
    // ========== API 错误日志 1-001-002-000 ==========
    ErrorCode API_ERROR_LOG_NOT_FOUND = new ErrorCode(1_001_002_000, "API 错误日志不存在");
    ErrorCode API_ERROR_LOG_PROCESSED = new ErrorCode(1_001_002_001, "API 错误日志已处理");
}
