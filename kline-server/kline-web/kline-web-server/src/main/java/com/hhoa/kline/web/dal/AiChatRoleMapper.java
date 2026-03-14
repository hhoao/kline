package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.dal.dataobject.AiChatRoleDO;
import com.hhoa.kline.web.dto.model.chatRole.AiChatRolePageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatRoleMapper extends BaseMapperX<AiChatRoleDO> {

    default com.hhoa.kline.web.common.pojo.PageResult<AiChatRoleDO> selectPage(
            AiChatRolePageReqVO reqVO) {
        return selectPage(
                reqVO,
                new com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX<AiChatRoleDO>()
                        .likeIfPresent(AiChatRoleDO::getName, reqVO.getName())
                        .eqIfPresent(AiChatRoleDO::getUserId, reqVO.getUserId())
                        .eqIfPresent(AiChatRoleDO::getStatus, reqVO.getStatus())
                        .orderByDesc(AiChatRoleDO::getId));
    }
}
