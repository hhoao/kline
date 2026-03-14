package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentPageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapperX<AiKnowledgeDocumentDO> {

    default PageResult<AiKnowledgeDocumentDO> selectPage(AiKnowledgeDocumentPageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiKnowledgeDocumentDO>()
                        .eqIfPresent(AiKnowledgeDocumentDO::getKnowledgeId, reqVO.getKnowledgeId())
                        .likeIfPresent(AiKnowledgeDocumentDO::getName, reqVO.getName())
                        .eqIfPresent(AiKnowledgeDocumentDO::getStatus, reqVO.getStatus())
                        .orderByDesc(AiKnowledgeDocumentDO::getCreateTime));
    }
}
