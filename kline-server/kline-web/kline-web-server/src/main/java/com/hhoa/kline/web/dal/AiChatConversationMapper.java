package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiChatConversationDO;
import com.hhoa.kline.web.dto.chat.conversation.AiChatConversationPageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatConversationMapper extends BaseMapperX<AiChatConversationDO> {

    default PageResult<AiChatConversationDO> selectPage(AiChatConversationPageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiChatConversationDO>()
                        .eqIfPresent(AiChatConversationDO::getUserId, reqVO.getUserId())
                        .likeIfPresent(AiChatConversationDO::getTitle, reqVO.getTitle())
                        .betweenIfPresent(
                                AiChatConversationDO::getCreateTime,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[0]
                                        : null,
                                reqVO.getCreateTime() != null && reqVO.getCreateTime().length >= 2
                                        ? reqVO.getCreateTime()[1]
                                        : null)
                        .orderByDesc(AiChatConversationDO::getCreateTime));
    }
}
