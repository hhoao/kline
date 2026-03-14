package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgePageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiKnowledgeMapper extends BaseMapperX<AiKnowledgeDO> {

    default PageResult<AiKnowledgeDO> selectPage(AiKnowledgePageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiKnowledgeDO>()
                        .likeIfPresent(AiKnowledgeDO::getName, reqVO.getName())
                        .eqIfPresent(AiKnowledgeDO::getStatus, reqVO.getStatus())
                        .betweenIfPresent(
                                AiKnowledgeDO::getCreateTime,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[0]
                                        : null,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[1]
                                        : null)
                        .orderByDesc(AiKnowledgeDO::getId));
    }
}
