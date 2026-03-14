package com.hhoa.kline.web.dal;

import com.hhoa.kline.web.common.mybatis.core.mapper.BaseMapperX;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.mybatis.core.query.QueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiModelDO;
import com.hhoa.kline.web.dto.model.model.AiModelPageReqVO;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 模型 Mapper
 *
 * @author fansili
 */
@Mapper
public interface AiChatMapper extends BaseMapperX<AiModelDO> {

    default AiModelDO selectFirstByStatus(Integer type, Integer status) {
        return selectOne(
                new QueryWrapperX<AiModelDO>()
                        .eq("type", type)
                        .eq("status", status)
                        .limitN(1)
                        .orderByAsc("sort"));
    }

    default PageResult<AiModelDO> selectPage(AiModelPageReqVO reqVO) {
        return selectPage(
                reqVO,
                new LambdaQueryWrapperX<AiModelDO>()
                        .likeIfPresent(AiModelDO::getName, reqVO.getName())
                        .eqIfPresent(AiModelDO::getModel, reqVO.getModel())
                        .eqIfPresent(AiModelDO::getPlatform, reqVO.getPlatform())
                        .orderByAsc(AiModelDO::getSort));
    }

    default List<AiModelDO> selectListByStatusAndType(
            Integer status, Integer type, @Nullable String platform) {
        return selectList(
                new LambdaQueryWrapperX<AiModelDO>()
                        .eq(AiModelDO::getStatus, status)
                        .eq(AiModelDO::getType, type)
                        .eqIfPresent(AiModelDO::getPlatform, platform)
                        .orderByAsc(AiModelDO::getSort));
    }
}
