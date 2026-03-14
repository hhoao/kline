package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiChatMessageDO;
import com.hhoa.kline.web.dto.chat.message.AiChatMessagePageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapperX<AiChatMessageDO> {

    default PageResult<AiChatMessageDO> selectPage(AiChatMessagePageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiChatMessageDO>()
                        .eqIfPresent(AiChatMessageDO::getConversationId, reqVO.getConversationId())
                        .eqIfPresent(AiChatMessageDO::getUserId, reqVO.getUserId())
                        .likeIfPresent(AiChatMessageDO::getContent, reqVO.getContent())
                        .betweenIfPresent(
                                AiChatMessageDO::getCreateTime,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[0]
                                        : null,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[1]
                                        : null)
                        .orderByDesc(AiChatMessageDO::getCreateTime));
    }
}
