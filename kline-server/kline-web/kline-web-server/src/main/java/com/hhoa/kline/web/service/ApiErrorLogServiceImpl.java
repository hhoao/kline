package com.hhoa.kline.web.service;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.kline.web.constants.ErrorCodeConstants.API_ERROR_LOG_NOT_FOUND;
import static com.hhoa.kline.web.constants.ErrorCodeConstants.API_ERROR_LOG_PROCESSED;
import static com.hhoa.kline.web.dal.dataobject.ApiErrorLogDO.REQUEST_PARAMS_MAX_LENGTH;

import com.hhoa.ai.kline.commons.utils.object.BeanUtils;
import com.hhoa.kline.web.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.string.StrUtils;
import com.hhoa.kline.web.dal.dataobject.ApiErrorLogDO;
import com.hhoa.kline.web.dto.ApiErrorLogPageReqVO;
import com.hhoa.kline.web.enums.ApiErrorLogProcessStatusEnum;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** API 错误日志 Service 实现类 */
@Service
@Validated
@Slf4j
public class ApiErrorLogServiceImpl implements ApiErrorLogService {

    @Resource private ApiErrorLogMapper apiErrorLogMapper;

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
        ApiErrorLogDO apiErrorLog =
                BeanUtils.toBean(createDTO, ApiErrorLogDO.class)
                        .setProcessStatus(ApiErrorLogProcessStatusEnum.INIT.getStatus());
        apiErrorLog.setRequestParams(
                StrUtils.maxLength(apiErrorLog.getRequestParams(), REQUEST_PARAMS_MAX_LENGTH));
        apiErrorLogMapper.insert(apiErrorLog);
    }

    @Override
    public PageResult<ApiErrorLogDO> getApiErrorLogPage(ApiErrorLogPageReqVO pageReqVO) {
        return apiErrorLogMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateApiErrorLogProcess(Long id, Integer processStatus, Long processUserId) {
        ApiErrorLogDO errorLog = apiErrorLogMapper.selectById(id);
        if (errorLog == null) {
            throw exception(API_ERROR_LOG_NOT_FOUND);
        }
        if (!ApiErrorLogProcessStatusEnum.INIT.getStatus().equals(errorLog.getProcessStatus())) {
            throw exception(API_ERROR_LOG_PROCESSED);
        }
        // 标记处理
        apiErrorLogMapper.updateById(
                ApiErrorLogDO.builder()
                        .id(id)
                        .processStatus(processStatus)
                        .processUserId(processUserId)
                        .processTime(LocalDateTime.now())
                        .build());
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Integer cleanErrorLog(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = apiErrorLogMapper.deleteByCreateTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }
}
