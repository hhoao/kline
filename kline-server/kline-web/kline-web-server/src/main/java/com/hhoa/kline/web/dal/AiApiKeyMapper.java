package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.mybatis.core.query.QueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiApiKeyDO;
import com.hhoa.kline.web.dto.AiApiKeyPageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI API 密钥 Mapper
 *
 */
@Mapper
public interface AiApiKeyMapper extends BaseMapperX<AiApiKeyDO> {

    default PageResult<AiApiKeyDO> selectPage(AiApiKeyPageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiApiKeyDO>()
                        .likeIfPresent(AiApiKeyDO::getName, reqVO.getName())
                        .eqIfPresent(AiApiKeyDO::getPlatform, reqVO.getPlatform())
                        .eqIfPresent(AiApiKeyDO::getStatus, reqVO.getStatus())
                        .orderByDesc(AiApiKeyDO::getId));
    }

    default AiApiKeyDO selectFirstByPlatformAndStatus(String platform, Integer status) {
        return selectOne(
                new QueryWrapperX<AiApiKeyDO>()
                        .eq("platform", platform)
                        .eq("status", status)
                        .limitN(1)
                        .orderByAsc("id"));
    }
}
