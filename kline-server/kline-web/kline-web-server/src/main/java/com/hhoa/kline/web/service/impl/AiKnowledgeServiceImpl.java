package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiKnowledgeMapper;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDO;
import com.hhoa.kline.web.dal.dataobject.AiModelDO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgePageReqVO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgeSaveReqVO;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiKnowledgeSegmentService;
import com.hhoa.kline.web.service.AiKnowledgeService;
import com.hhoa.kline.web.service.AiModelService;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    @Resource private AiKnowledgeMapper knowledgeMapper;

    @Resource private AiModelService modelService;

    @Resource private AiKnowledgeSegmentService knowledgeSegmentService;

    @Resource private AiKnowledgeDocumentService knowledgeDocumentService;

    @Override
    public Long createKnowledge(AiKnowledgeSaveReqVO createReqVO) {
        AiModelDO model = modelService.validateModel(createReqVO.getEmbeddingModelId());

        AiKnowledgeDO knowledge =
                BeanUtil.toBean(createReqVO, AiKnowledgeDO.class)
                        .setEmbeddingModel(model.getModel());
        knowledgeMapper.insert(knowledge);
        return knowledge.getId();
    }

    @Override
    public void updateKnowledge(AiKnowledgeSaveReqVO updateReqVO) {
        AiKnowledgeDO oldKnowledge = validateKnowledgeExists(updateReqVO.getId());
        AiModelDO model = modelService.validateModel(updateReqVO.getEmbeddingModelId());

        AiKnowledgeDO updateObj =
                BeanUtil.toBean(updateReqVO, AiKnowledgeDO.class)
                        .setEmbeddingModel(model.getModel());
        knowledgeMapper.updateById(updateObj);

        if (ObjUtil.notEqual(
                oldKnowledge.getEmbeddingModelId(), updateReqVO.getEmbeddingModelId())) {
            knowledgeSegmentService.reindexByKnowledgeIdAsync(updateReqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledge(Long id) {
        validateKnowledgeExists(id);
        knowledgeDocumentService.deleteKnowledgeDocumentByKnowledgeId(id);
        knowledgeMapper.deleteById(id);
    }

    @Override
    public AiKnowledgeDO getKnowledge(Long id) {
        return knowledgeMapper.selectById(id);
    }

    @Override
    public AiKnowledgeDO validateKnowledgeExists(Long id) {
        AiKnowledgeDO knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
        return knowledge;
    }

    @Override
    public PageResult<AiKnowledgeDO> getKnowledgePage(AiKnowledgePageReqVO pageReqVO) {
        PageResult<AiKnowledgeDO> result = knowledgeMapper.selectPage(pageReqVO);
        return new PageResult<>(result.getList(), result.getTotal());
    }

    @Override
    public List<AiKnowledgeDO> getKnowledgeSimpleListByStatus(Integer status) {
        return knowledgeMapper.selectList(
                new LambdaQueryWrapperX<AiKnowledgeDO>()
                        .eq(AiKnowledgeDO::getStatus, status)
                        .orderByAsc(AiKnowledgeDO::getId));
    }
}
