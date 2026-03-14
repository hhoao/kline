package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_DOCUMENT_FILE_DOWNLOAD_FAIL;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_DOCUMENT_FILE_EMPTY;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_DOCUMENT_FILE_READ_FAIL;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_DOCUMENT_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiKnowledgeDocumentMapper;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentCreateListReqVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentPageReqVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentUpdateReqVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentUpdateStatusReqVO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgeDocumentCreateReqVO;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiKnowledgeSegmentService;
import com.hhoa.kline.web.service.AiKnowledgeService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AiKnowledgeDocumentServiceImpl implements AiKnowledgeDocumentService {

    @Resource private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource private TokenCountEstimator tokenCountEstimator;

    @Resource private AiKnowledgeSegmentService knowledgeSegmentService;

    @Resource @Lazy private AiKnowledgeService knowledgeService;

    @Override
    public Long createKnowledgeDocument(AiKnowledgeDocumentCreateReqVO createReqVO) {
        knowledgeService.validateKnowledgeExists(createReqVO.getKnowledgeId());

        String content = readUrl(createReqVO.getUrl());

        AiKnowledgeDocumentDO documentDO =
                BeanUtil.toBean(createReqVO, AiKnowledgeDocumentDO.class)
                        .setContent(content)
                        .setContentLength(content.length())
                        .setTokens(tokenCountEstimator.estimate(content))
                        .setStatus(CommonStatusEnum.ENABLE.getStatus());
        knowledgeDocumentMapper.insert(documentDO);

        knowledgeSegmentService.createKnowledgeSegmentBySplitContentAsync(
                documentDO.getId(), content);
        return documentDO.getId();
    }

    @Override
    public List<Long> createKnowledgeDocumentList(
            AiKnowledgeDocumentCreateListReqVO createListReqVO) {
        knowledgeService.validateKnowledgeExists(createListReqVO.getKnowledgeId());

        List<String> contents =
                convertList(createListReqVO.getList(), document -> readUrl(document.getUrl()));

        List<AiKnowledgeDocumentDO> documentDOs = new ArrayList<>(createListReqVO.getList().size());
        for (int i = 0; i < createListReqVO.getList().size(); i++) {
            AiKnowledgeDocumentCreateListReqVO.Document documentVO =
                    createListReqVO.getList().get(i);
            String content = contents.get(i);
            AiKnowledgeDocumentDO doc =
                    BeanUtil.toBean(documentVO, AiKnowledgeDocumentDO.class)
                            .setKnowledgeId(createListReqVO.getKnowledgeId())
                            .setContent(content)
                            .setContentLength(content.length())
                            .setTokens(tokenCountEstimator.estimate(content))
                            .setSegmentMaxTokens(createListReqVO.getSegmentMaxTokens())
                            .setStatus(CommonStatusEnum.ENABLE.getStatus());
            knowledgeDocumentMapper.insert(doc);
            documentDOs.add(doc);
        }

        documentDOs.forEach(
                documentDO ->
                        knowledgeSegmentService.createKnowledgeSegmentBySplitContentAsync(
                                documentDO.getId(), documentDO.getContent()));
        return convertList(documentDOs, AiKnowledgeDocumentDO::getId);
    }

    @Override
    public PageResult<AiKnowledgeDocumentDO> getKnowledgeDocumentPage(
            AiKnowledgeDocumentPageReqVO pageReqVO) {
        PageResult<AiKnowledgeDocumentDO> result = knowledgeDocumentMapper.selectPage(pageReqVO);
        return new PageResult<>(result.getList(), result.getTotal());
    }

    @Override
    public AiKnowledgeDocumentDO getKnowledgeDocument(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    @Override
    public void updateKnowledgeDocument(AiKnowledgeDocumentUpdateReqVO reqVO) {
        AiKnowledgeDocumentDO oldDocument = validateKnowledgeDocumentExists(reqVO.getId());

        AiKnowledgeDocumentDO document = BeanUtil.toBean(reqVO, AiKnowledgeDocumentDO.class);
        knowledgeDocumentMapper.updateById(document);

        if (CommonStatusEnum.isEnable(oldDocument.getStatus())
                && reqVO.getSegmentMaxTokens() != null
                && ObjUtil.notEqual(
                        reqVO.getSegmentMaxTokens(), oldDocument.getSegmentMaxTokens())) {
            knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(reqVO.getId());
            knowledgeSegmentService.createKnowledgeSegmentBySplitContentAsync(
                    reqVO.getId(), oldDocument.getContent());
        }
    }

    @Override
    public void updateKnowledgeDocumentStatus(AiKnowledgeDocumentUpdateStatusReqVO reqVO) {
        AiKnowledgeDocumentDO document = validateKnowledgeDocumentExists(reqVO.getId());

        knowledgeDocumentMapper.updateById(
                new AiKnowledgeDocumentDO().setId(reqVO.getId()).setStatus(reqVO.getStatus()));

        if (CommonStatusEnum.isEnable(reqVO.getStatus())) {
            knowledgeSegmentService.createKnowledgeSegmentBySplitContentAsync(
                    reqVO.getId(), document.getContent());
        } else {
            knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(reqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeDocument(Long id) {
        validateKnowledgeDocumentExists(id);
        knowledgeDocumentMapper.deleteById(id);
        knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(id);
    }

    @Override
    public AiKnowledgeDocumentDO validateKnowledgeDocumentExists(Long id) {
        AiKnowledgeDocumentDO knowledgeDocument = knowledgeDocumentMapper.selectById(id);
        if (knowledgeDocument == null) {
            throw exception(KNOWLEDGE_DOCUMENT_NOT_EXISTS);
        }
        return knowledgeDocument;
    }

    @Override
    public String readUrl(String url) {
        ByteArrayResource resource;
        try {
            byte[] bytes = HttpUtil.downloadBytes(url);
            if (bytes.length == 0) {
                throw exception(KNOWLEDGE_DOCUMENT_FILE_EMPTY);
            }
            resource = new ByteArrayResource(bytes);
        } catch (Exception e) {
            log.error("[readUrl][url({}) 读取失败]", url, e);
            throw exception(KNOWLEDGE_DOCUMENT_FILE_DOWNLOAD_FAIL);
        }

        TikaDocumentReader loader = new TikaDocumentReader(resource);
        List<Document> documents = loader.get();
        Document document = CollUtil.getFirst(documents);
        if (document == null || StrUtil.isEmpty(document.getText())) {
            throw exception(KNOWLEDGE_DOCUMENT_FILE_READ_FAIL);
        }
        return document.getText();
    }

    @Override
    public List<AiKnowledgeDocumentDO> getKnowledgeDocumentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return knowledgeDocumentMapper.selectBatchIds(ids);
    }

    @Override
    public List<AiKnowledgeDocumentDO> getKnowledgeDocumentListByKnowledgeId(Long knowledgeId) {
        return knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapperX<AiKnowledgeDocumentDO>()
                        .eq(AiKnowledgeDocumentDO::getKnowledgeId, knowledgeId)
                        .orderByAsc(AiKnowledgeDocumentDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeDocumentByKnowledgeId(Long knowledgeId) {
        List<AiKnowledgeDocumentDO> documents =
                knowledgeDocumentMapper.selectList(
                        new LambdaQueryWrapperX<AiKnowledgeDocumentDO>()
                                .eq(AiKnowledgeDocumentDO::getKnowledgeId, knowledgeId));
        if (CollUtil.isEmpty(documents)) {
            return;
        }
        for (AiKnowledgeDocumentDO document : documents) {
            deleteKnowledgeDocument(document.getId());
        }
    }
}
