package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.KNOWLEDGE_SEGMENT_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiKnowledgeSegmentMapper;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDO;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeSegmentDO;
import com.hhoa.kline.web.dto.knowledge.segment.AiKnowledgeSegmentPageReqVO;
import com.hhoa.kline.web.dto.knowledge.segment.AiKnowledgeSegmentProcessRespVO;
import com.hhoa.kline.web.dto.knowledge.segment.AiKnowledgeSegmentSaveReqVO;
import com.hhoa.kline.web.dto.knowledge.segment.AiKnowledgeSegmentUpdateStatusReqVO;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiKnowledgeSegmentService;
import com.hhoa.kline.web.service.AiKnowledgeService;
import com.hhoa.kline.web.service.AiModelService;
import com.hhoa.kline.web.service.bo.AiKnowledgeSegmentSearchReqBO;
import com.hhoa.kline.web.service.bo.AiKnowledgeSegmentSearchRespBO;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * AI 知识库分片 Service 实现类
 *
 * @author hhoa
 */
@Service
@Slf4j
public class AiKnowledgeSegmentServiceImpl implements AiKnowledgeSegmentService {

    private static final String VECTOR_STORE_METADATA_KNOWLEDGE_ID = "knowledgeId";
    private static final String VECTOR_STORE_METADATA_DOCUMENT_ID = "documentId";
    private static final String VECTOR_STORE_METADATA_SEGMENT_ID = "segmentId";

    private static final Map<String, Class<?>> VECTOR_STORE_METADATA_TYPES =
            Map.of(
                    VECTOR_STORE_METADATA_KNOWLEDGE_ID, String.class,
                    VECTOR_STORE_METADATA_DOCUMENT_ID, String.class,
                    VECTOR_STORE_METADATA_SEGMENT_ID, String.class);
    @Resource private AiKnowledgeSegmentMapper segmentMapper;
    @Resource @Lazy private AiKnowledgeService knowledgeService;
    @Resource @Lazy // 延迟加载，避免循环依赖
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource private AiModelService modelService;
    @Resource private TokenCountEstimator tokenCountEstimator;

    private static List<Document> splitContentByToken(String content, Integer segmentMaxTokens) {
        TextSplitter textSplitter = buildTokenTextSplitter(segmentMaxTokens);
        return textSplitter.apply(Collections.singletonList(new Document(content)));
    }

    private static TextSplitter buildTokenTextSplitter(Integer segmentMaxTokens) {
        return TokenTextSplitter.builder()
                .withChunkSize(segmentMaxTokens)
                .withMinChunkSizeChars(Integer.MAX_VALUE) // 忽略字符的截断
                .withMinChunkLengthToEmbed(1) // 允许的最小有效分段长度
                .withMaxNumChunks(Integer.MAX_VALUE)
                .withKeepSeparator(true) // 保留分隔符
                .build();
    }

    @Override
    public PageResult<AiKnowledgeSegmentDO> getKnowledgeSegmentPage(
            AiKnowledgeSegmentPageReqVO pageReqVO) {
        PageResult<AiKnowledgeSegmentDO> result = segmentMapper.selectPage(pageReqVO);
        return new PageResult<>(result.getList(), result.getTotal());
    }

    @Override
    public void createKnowledgeSegmentBySplitContent(Long documentId, String content) {
        // 1. 校验
        AiKnowledgeDocumentDO documentDO =
                knowledgeDocumentService.validateKnowledgeDocumentExists(documentId);
        AiKnowledgeDO knowledgeDO =
                knowledgeService.validateKnowledgeExists(documentDO.getKnowledgeId());
        VectorStore vectorStore = getVectorStoreById(knowledgeDO);

        // 2. 文档切片
        List<Document> documentSegments =
                splitContentByToken(content, documentDO.getSegmentMaxTokens());

        // 3.1 存储切片
        List<AiKnowledgeSegmentDO> segmentDOs =
                convertList(
                        documentSegments,
                        segment -> {
                            if (StrUtil.isEmpty(segment.getText())) {
                                return null;
                            }
                            return new AiKnowledgeSegmentDO()
                                    .setKnowledgeId(documentDO.getKnowledgeId())
                                    .setDocumentId(documentId)
                                    .setContent(segment.getText())
                                    .setContentLength(segment.getText().length())
                                    .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY)
                                    .setTokens(tokenCountEstimator.estimate(segment.getText()))
                                    .setStatus(CommonStatusEnum.ENABLE.getStatus());
                        });
        for (AiKnowledgeSegmentDO seg : segmentDOs) {
            segmentMapper.insert(seg);
        }
        // 3.2 切片向量化
        for (int i = 0; i < documentSegments.size(); i++) {
            Document segment = documentSegments.get(i);
            AiKnowledgeSegmentDO segmentDO = segmentDOs.get(i);
            writeVectorStore(vectorStore, segmentDO, segment);
        }
    }

    @Override
    public void updateKnowledgeSegment(AiKnowledgeSegmentSaveReqVO reqVO) {
        // 1. 校验
        AiKnowledgeSegmentDO oldSegment = validateKnowledgeSegmentExists(reqVO.getId());

        // 2. 删除向量
        VectorStore vectorStore = getVectorStoreById(oldSegment.getKnowledgeId());
        deleteVectorStore(vectorStore, oldSegment);

        // 3.1 更新切片
        AiKnowledgeSegmentDO newSegment = BeanUtil.toBean(reqVO, AiKnowledgeSegmentDO.class);
        segmentMapper.updateById(newSegment);
        // 3.2 重新向量化，必须开启状态
        if (CommonStatusEnum.isEnable(oldSegment.getStatus())) {
            newSegment
                    .setKnowledgeId(oldSegment.getKnowledgeId())
                    .setDocumentId(oldSegment.getDocumentId());
            writeVectorStore(vectorStore, newSegment, new Document(newSegment.getContent()));
        }
    }

    @Override
    public void deleteKnowledgeSegmentByDocumentId(Long documentId) {
        List<AiKnowledgeSegmentDO> segments =
                segmentMapper.selectList(
                        new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                                .eq(AiKnowledgeSegmentDO::getDocumentId, documentId));
        if (CollUtil.isEmpty(segments)) {
            return;
        }

        segmentMapper.deleteBatchIds(convertList(segments, AiKnowledgeSegmentDO::getId));

        // 3. 删除向量存储中的段落
        VectorStore vectorStore = getVectorStoreById(segments.get(0).getKnowledgeId());
        vectorStore.delete(convertList(segments, AiKnowledgeSegmentDO::getVectorId));
    }

    @Override
    public void updateKnowledgeSegmentStatus(AiKnowledgeSegmentUpdateStatusReqVO reqVO) {
        // 1. 校验
        AiKnowledgeSegmentDO segment = validateKnowledgeSegmentExists(reqVO.getId());

        // 2. 获取知识库向量实例
        VectorStore vectorStore = getVectorStoreById(segment.getKnowledgeId());

        // 3. 更新状态
        segmentMapper.updateById(
                new AiKnowledgeSegmentDO().setId(reqVO.getId()).setStatus(reqVO.getStatus()));

        // 4. 更新向量
        if (CommonStatusEnum.isEnable(reqVO.getStatus())) {
            writeVectorStore(vectorStore, segment, new Document(segment.getContent()));
        } else {
            deleteVectorStore(vectorStore, segment);
        }
    }

    @Override
    public void reindexKnowledgeSegmentByKnowledgeId(Long knowledgeId) {
        // 1.1 校验知识库存在
        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(knowledgeId);
        // 1.2 获取知识库向量实例
        VectorStore vectorStore = getVectorStoreById(knowledge);

        // 2.1 查询知识库下的所有启用状态的段落
        List<AiKnowledgeSegmentDO> segments =
                segmentMapper.selectList(
                        new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                                .eq(AiKnowledgeSegmentDO::getKnowledgeId, knowledgeId)
                                .eq(
                                        AiKnowledgeSegmentDO::getStatus,
                                        CommonStatusEnum.ENABLE.getStatus()));
        if (CollUtil.isEmpty(segments)) {
            return;
        }
        // 2.2 遍历所有段落，重新索引
        for (AiKnowledgeSegmentDO segment : segments) {
            // 删除旧的向量
            deleteVectorStore(vectorStore, segment);
            // 重新创建向量
            writeVectorStore(vectorStore, segment, new Document(segment.getContent()));
        }
        log.info(
                "[reindexKnowledgeSegmentByKnowledgeId][知识库({}) 重新索引完成，共处理 {} 个段落]",
                knowledgeId,
                segments.size());
    }

    private void writeVectorStore(
            VectorStore vectorStore, AiKnowledgeSegmentDO segmentDO, Document segment) {
        // 1. 向量存储
        // 为什么要 toString 呢？因为部分 VectorStore 实现，不支持 Long 类型，例如说 QdrantVectorStore
        segment.getMetadata()
                .put(VECTOR_STORE_METADATA_KNOWLEDGE_ID, segmentDO.getKnowledgeId().toString());
        segment.getMetadata()
                .put(VECTOR_STORE_METADATA_DOCUMENT_ID, segmentDO.getDocumentId().toString());
        segment.getMetadata().put(VECTOR_STORE_METADATA_SEGMENT_ID, segmentDO.getId().toString());
        vectorStore.add(List.of(segment));

        // 2. 更新向量 ID
        segmentDO.setVectorId(segment.getId());
        segmentMapper.updateById(segmentDO);
    }

    private void deleteVectorStore(VectorStore vectorStore, AiKnowledgeSegmentDO segmentDO) {
        // 1. 更新向量 ID
        if (StrUtil.isEmpty(segmentDO.getVectorId())) {
            return;
        }
        segmentDO.setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY);
        segmentMapper.updateById(segmentDO);

        // 2. 删除向量
        vectorStore.delete(List.of(segmentDO.getVectorId()));
    }

    @Override
    public List<AiKnowledgeSegmentSearchRespBO> searchKnowledgeSegment(
            AiKnowledgeSegmentSearchReqBO reqBO) {
        // 1. 校验
        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(reqBO.getKnowledgeId());

        // 2.1 向量检索
        VectorStore vectorStore = getVectorStoreById(knowledge);
        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(reqBO.getContent())
                                .topK(ObjUtil.defaultIfNull(reqBO.getTopK(), knowledge.getTopK()))
                                .similarityThreshold(
                                        ObjUtil.defaultIfNull(
                                                reqBO.getSimilarityThreshold(),
                                                knowledge.getSimilarityThreshold()))
                                .filterExpression(
                                        new FilterExpressionBuilder()
                                                .eq(
                                                        VECTOR_STORE_METADATA_KNOWLEDGE_ID,
                                                        reqBO.getKnowledgeId().toString())
                                                .build())
                                .build());
        if (CollUtil.isEmpty(documents)) {
            return ListUtil.empty();
        }
        // 2.2 段落召回
        List<String> vectorIds = convertList(documents, Document::getId);
        List<AiKnowledgeSegmentDO> segments =
                segmentMapper.selectList(
                        new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                                .in(AiKnowledgeSegmentDO::getVectorId, vectorIds));
        if (CollUtil.isEmpty(segments)) {
            return ListUtil.empty();
        }

        // 3. 增加召回次数
        segmentMapper.updateRetrievalCountIncr(convertList(segments, AiKnowledgeSegmentDO::getId));

        // 4. 构建结果
        List<AiKnowledgeSegmentSearchRespBO> result =
                convertList(
                        segments,
                        segment -> {
                            Document document =
                                    CollUtil.findOne(
                                            documents, // 找到对应的文档
                                            doc ->
                                                    Objects.equals(
                                                            doc.getId(), segment.getVectorId()));
                            if (document == null) {
                                return null;
                            }
                            return BeanUtil.toBean(segment, AiKnowledgeSegmentSearchRespBO.class)
                                    .setScore(document.getScore());
                        });
        result.sort((o1, o2) -> Double.compare(o2.getScore(), o1.getScore())); // 按照分数降序排序
        return result;
    }

    @Override
    public List<AiKnowledgeSegmentDO> splitContent(String url, Integer segmentMaxTokens) {
        // 1. 读取 URL 内容
        String content = knowledgeDocumentService.readUrl(url);

        // 2. 文档切片
        List<Document> documentSegments = splitContentByToken(content, segmentMaxTokens);

        // 3. 转换为段落对象
        return convertList(
                documentSegments,
                segment -> {
                    if (StrUtil.isEmpty(segment.getText())) {
                        return null;
                    }
                    return new AiKnowledgeSegmentDO()
                            .setContent(segment.getText())
                            .setContentLength(segment.getText().length())
                            .setTokens(tokenCountEstimator.estimate(segment.getText()));
                });
    }

    /**
     * 校验段落是否存在
     *
     * @param id 文档编号
     * @return 段落信息
     */
    private AiKnowledgeSegmentDO validateKnowledgeSegmentExists(Long id) {
        AiKnowledgeSegmentDO knowledgeSegment = segmentMapper.selectById(id);
        if (knowledgeSegment == null) {
            throw exception(KNOWLEDGE_SEGMENT_NOT_EXISTS);
        }
        return knowledgeSegment;
    }

    private VectorStore getVectorStoreById(AiKnowledgeDO knowledge) {
        return modelService.getOrCreateVectorStore(
                knowledge.getEmbeddingModelId(), VECTOR_STORE_METADATA_TYPES);
    }

    private VectorStore getVectorStoreById(Long knowledgeId) {
        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(knowledgeId);
        return getVectorStoreById(knowledge);
    }

    @Override
    public List<AiKnowledgeSegmentProcessRespVO> getKnowledgeSegmentProcessList(
            List<Long> documentIds) {
        if (CollUtil.isEmpty(documentIds)) {
            return Collections.emptyList();
        }
        List<AiKnowledgeSegmentDO> segments =
                segmentMapper.selectList(
                        new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                                .in(AiKnowledgeSegmentDO::getDocumentId, documentIds));
        return segments.stream()
                .collect(Collectors.groupingBy(AiKnowledgeSegmentDO::getDocumentId))
                .entrySet()
                .stream()
                .map(
                        e -> {
                            List<AiKnowledgeSegmentDO> list = e.getValue();
                            long embeddingCount =
                                    list.stream()
                                            .filter(
                                                    s ->
                                                            s.getVectorId() != null
                                                                    && !s.getVectorId().isEmpty())
                                            .count();
                            return new AiKnowledgeSegmentProcessRespVO(
                                    e.getKey(), (long) list.size(), embeddingCount);
                        })
                .toList();
    }

    @Override
    public Long createKnowledgeSegment(AiKnowledgeSegmentSaveReqVO createReqVO) {
        // 1.1 校验文档是否存在
        AiKnowledgeDocumentDO document =
                knowledgeDocumentService.validateKnowledgeDocumentExists(
                        createReqVO.getDocumentId());
        // 1.2 获取知识库信息
        AiKnowledgeDO knowledge =
                knowledgeService.validateKnowledgeExists(document.getKnowledgeId());
        // 1.3 校验 token 熟练
        Integer tokens = tokenCountEstimator.estimate(createReqVO.getContent());
        if (tokens > document.getSegmentMaxTokens()) {
            throw exception(
                    KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG, tokens, document.getSegmentMaxTokens());
        }

        // 2. 保存段落
        AiKnowledgeSegmentDO segment =
                BeanUtil.toBean(createReqVO, AiKnowledgeSegmentDO.class)
                        .setKnowledgeId(knowledge.getId())
                        .setDocumentId(document.getId())
                        .setContentLength(createReqVO.getContent().length())
                        .setTokens(tokens)
                        .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY)
                        .setRetrievalCount(0)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus());
        segmentMapper.insert(segment);

        // 3. 向量化
        writeVectorStore(
                getVectorStoreById(knowledge), segment, new Document(segment.getContent()));
        return segment.getId();
    }

    @Override
    public AiKnowledgeSegmentDO getKnowledgeSegment(Long id) {
        return segmentMapper.selectById(id);
    }

    @Override
    public List<AiKnowledgeSegmentDO> getKnowledgeSegmentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return segmentMapper.selectBatchIds(ids);
    }
}
