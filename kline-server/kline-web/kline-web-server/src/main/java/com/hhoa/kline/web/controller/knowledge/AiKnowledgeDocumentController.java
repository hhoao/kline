package com.hhoa.kline.web.controller.knowledge;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.object.BeanUtils;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentCreateListReqVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentPageReqVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentRespVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentUpdateReqVO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentUpdateStatusReqVO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgeDocumentCreateReqVO;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 知识库文档")
@RestController
@RequestMapping("/ai/knowledge/document")
@Validated
public class AiKnowledgeDocumentController {

    @Resource private AiKnowledgeDocumentService documentService;

    @GetMapping("/page")
    @Operation(summary = "获取文档分页")
    public CommonResult<PageResult<AiKnowledgeDocumentRespVO>> getKnowledgeDocumentPage(
            @Valid AiKnowledgeDocumentPageReqVO pageReqVO) {
        PageResult<AiKnowledgeDocumentDO> pageResult =
                documentService.getKnowledgeDocumentPage(pageReqVO);
        return success(BeanUtils.toPageResultBean(pageResult, AiKnowledgeDocumentRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取文档详情")
    public CommonResult<AiKnowledgeDocumentRespVO> getKnowledgeDocument(
            @RequestParam("id") Long id) {
        AiKnowledgeDocumentDO document = documentService.getKnowledgeDocument(id);
        return success(BeanUtil.toBean(document, AiKnowledgeDocumentRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新建文档（单个）")
    public CommonResult<Long> createKnowledgeDocument(
            @RequestBody @Valid AiKnowledgeDocumentCreateReqVO reqVO) {
        Long id = documentService.createKnowledgeDocument(reqVO);
        return success(id);
    }

    @PostMapping("/create-list")
    @Operation(summary = "新建文档（多个）")
    public CommonResult<List<Long>> createKnowledgeDocumentList(
            @RequestBody @Valid AiKnowledgeDocumentCreateListReqVO reqVO) {
        List<Long> ids = documentService.createKnowledgeDocumentList(reqVO);
        return success(ids);
    }

    @PutMapping("/update")
    @Operation(summary = "更新文档")
    public CommonResult<Boolean> updateKnowledgeDocument(
            @Valid @RequestBody AiKnowledgeDocumentUpdateReqVO reqVO) {
        documentService.updateKnowledgeDocument(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新文档状态")
    public CommonResult<Boolean> updateKnowledgeDocumentStatus(
            @Valid @RequestBody AiKnowledgeDocumentUpdateStatusReqVO reqVO) {
        documentService.updateKnowledgeDocumentStatus(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文档")
    public CommonResult<Boolean> deleteKnowledgeDocument(@RequestParam("id") Long id) {
        documentService.deleteKnowledgeDocument(id);
        return success(true);
    }
}
