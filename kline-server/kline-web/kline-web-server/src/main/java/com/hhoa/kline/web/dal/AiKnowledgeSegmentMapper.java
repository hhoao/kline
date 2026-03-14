package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeSegmentDO;
import com.hhoa.kline.web.dto.knowledge.segment.AiKnowledgeSegmentPageReqVO;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiKnowledgeSegmentMapper extends BaseMapperX<AiKnowledgeSegmentDO> {

    default PageResult<AiKnowledgeSegmentDO> selectPage(AiKnowledgeSegmentPageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                        .eqIfPresent(AiKnowledgeSegmentDO::getDocumentId, reqVO.getDocumentId())
                        .eqIfPresent(AiKnowledgeSegmentDO::getStatus, reqVO.getStatus())
                        .orderByDesc(AiKnowledgeSegmentDO::getId));
    }

    default void updateRetrievalCountIncr(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            AiKnowledgeSegmentDO seg = selectById(id);
            if (seg != null) {
                Integer c = seg.getRetrievalCount();
                seg.setRetrievalCount(c == null ? 1 : c + 1);
                updateById(seg);
            }
        }
    }
}
