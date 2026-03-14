package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiToolDO;
import com.hhoa.kline.web.dto.model.tool.AiToolPageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiToolMapper extends BaseMapperX<AiToolDO> {

    default PageResult<AiToolDO> selectPage(AiToolPageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiToolDO>()
                        .likeIfPresent(AiToolDO::getName, reqVO.getName())
                        .likeIfPresent(AiToolDO::getDescription, reqVO.getDescription())
                        .eqIfPresent(AiToolDO::getStatus, reqVO.getStatus())
                        .betweenIfPresent(
                                AiToolDO::getCreateTime,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[0]
                                        : null,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[1]
                                        : null)
                        .orderByDesc(AiToolDO::getId));
    }
}
