package com.hhoa.kline.web.controller.knowledge;

import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.PageUtils;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgePageReqVO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgeRespVO;
import com.hhoa.kline.web.dto.knowledge.knowledge.AiKnowledgeSaveReqVO;
import com.hhoa.kline.web.service.AiKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "AI 知识库")
@RestController
@RequestMapping("/ai/knowledge")
@Validated
public class AiKnowledgeController {

    @Resource private AiKnowledgeService knowledgeService;

    @GetMapping("/page")
    @Operation(summary = "获取知识库分页")
    public CommonResult<PageResult<AiKnowledgeRespVO>> getKnowledgePage(
            @Valid AiKnowledgePageReqVO pageReqVO) {
        PageResult<AiKnowledgeDO> pageResult = knowledgeService.getKnowledgePage(pageReqVO);
        return success(PageUtils.toPageResult(pageResult, AiKnowledgeRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AiKnowledgeRespVO> getKnowledge(@RequestParam("id") Long id) {
        AiKnowledgeDO knowledge = knowledgeService.getKnowledge(id);
        return success(BeanUtil.toBean(knowledge, AiKnowledgeRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    public CommonResult<Long> createKnowledge(
            @RequestBody @Valid AiKnowledgeSaveReqVO createReqVO) {
        return success(knowledgeService.createKnowledge(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    public CommonResult<Boolean> updateKnowledge(
            @RequestBody @Valid AiKnowledgeSaveReqVO updateReqVO) {
        knowledgeService.updateKnowledge(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<Boolean> deleteKnowledge(@RequestParam("id") Long id) {
        knowledgeService.deleteKnowledge(id);
        return success(true);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得知识库的精简列表")
    public CommonResult<List<AiKnowledgeRespVO>> getKnowledgeSimpleList() {
        List<AiKnowledgeDO> list =
                knowledgeService.getKnowledgeSimpleListByStatus(
                        CommonStatusEnum.ENABLE.getStatus());
        return success(
                convertList(
                        list,
                        knowledge ->
                                new AiKnowledgeRespVO()
                                        .setId(knowledge.getId())
                                        .setName(knowledge.getName())));
    }
}
