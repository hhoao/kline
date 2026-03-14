package com.hhoa.kline.web.controller.model;

import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.object.BeanUtils;
import com.hhoa.kline.web.dal.dataobject.AiToolDO;
import com.hhoa.kline.web.dto.model.tool.AiToolPageReqVO;
import com.hhoa.kline.web.dto.model.tool.AiToolRespVO;
import com.hhoa.kline.web.dto.model.tool.AiToolSaveReqVO;
import com.hhoa.kline.web.service.AiToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 工具")
@RestController
@RequestMapping("/ai/tool")
@Validated
public class AiToolController {

    @Resource private AiToolService toolService;

    @PostMapping("/create")
    @Operation(summary = "创建工具")
    public CommonResult<Long> createTool(@Valid @RequestBody AiToolSaveReqVO createReqVO) {
        return success(toolService.createTool(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工具")
    public CommonResult<Boolean> updateTool(@Valid @RequestBody AiToolSaveReqVO updateReqVO) {
        toolService.updateTool(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工具")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteTool(@RequestParam("id") Long id) {
        toolService.deleteTool(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工具")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AiToolRespVO> getTool(@RequestParam("id") Long id) {
        AiToolDO tool = toolService.getTool(id);
        return success(BeanUtil.toBean(tool, AiToolRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工具分页")
    public CommonResult<PageResult<AiToolRespVO>> getToolPage(@Valid AiToolPageReqVO pageReqVO) {
        PageResult<AiToolDO> pageResult = toolService.getToolPage(pageReqVO);
        return success(BeanUtils.toPageResultBean(pageResult, AiToolRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得工具列表")
    public CommonResult<List<AiToolRespVO>> getToolSimpleList() {
        List<AiToolDO> list = toolService.getToolListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(
                convertList(
                        list,
                        tool -> new AiToolRespVO().setId(tool.getId()).setName(tool.getName())));
    }
}
