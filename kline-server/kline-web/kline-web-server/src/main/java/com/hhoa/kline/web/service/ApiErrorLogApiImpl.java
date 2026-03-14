package com.hhoa.kline.web.service;

import com.hhoa.kline.web.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.hhoa.kline.web.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * API 访问日志的 API 接口
 *
 */
@Service
@Validated
public class ApiErrorLogApiImpl implements ApiErrorLogCommonApi {

    @Resource private ApiErrorLogService apiErrorLogService;

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
        apiErrorLogService.createApiErrorLog(createDTO);
    }
}
