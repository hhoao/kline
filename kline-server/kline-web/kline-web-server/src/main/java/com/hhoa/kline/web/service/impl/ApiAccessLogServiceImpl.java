package com.hhoa.kline.web.service.impl;

import static com.hhoa.kline.web.dal.dataobject.ApiAccessLogDO.REQUEST_PARAMS_MAX_LENGTH;
import static com.hhoa.kline.web.dal.dataobject.ApiAccessLogDO.RESULT_MSG_MAX_LENGTH;

import com.hhoa.ai.kline.commons.utils.object.BeanUtils;
import com.hhoa.kline.web.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.string.StrUtils;
import com.hhoa.kline.web.dal.ApiAccessLogMapper;
import com.hhoa.kline.web.dal.dataobject.ApiAccessLogDO;
import com.hhoa.kline.web.dto.ApiAccessLogPageReqVO;
import com.hhoa.kline.web.service.ApiAccessLogService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** API 访问日志 Service 实现类 */
@Slf4j
@Service
@Validated
public class ApiAccessLogServiceImpl implements ApiAccessLogService {

    @Resource private ApiAccessLogMapper apiAccessLogMapper;

    @Override
    public void createApiAccessLog(ApiAccessLogCreateReqDTO createDTO) {
        ApiAccessLogDO apiAccessLog = BeanUtils.toBean(createDTO, ApiAccessLogDO.class);
        apiAccessLog.setRequestParams(
                StrUtils.maxLength(apiAccessLog.getRequestParams(), REQUEST_PARAMS_MAX_LENGTH));
        apiAccessLog.setResultMsg(
                StrUtils.maxLength(apiAccessLog.getResultMsg(), RESULT_MSG_MAX_LENGTH));
        apiAccessLogMapper.insert(apiAccessLog);
    }

    @Override
    public PageResult<ApiAccessLogDO> getApiAccessLogPage(ApiAccessLogPageReqVO pageReqVO) {
        return apiAccessLogMapper.selectPage(pageReqVO);
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Integer cleanAccessLog(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = apiAccessLogMapper.deleteByCreateTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }
}
