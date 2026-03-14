package com.hhoa.kline.web.controller.model;

import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.object.BeanUtils;
import com.hhoa.kline.web.dal.dataobject.AiModelDO;
import com.hhoa.kline.web.dto.model.model.AiModelPageReqVO;
import com.hhoa.kline.web.dto.model.model.AiModelRespVO;
import com.hhoa.kline.web.dto.model.model.AiModelSaveReqVO;
import com.hhoa.kline.web.service.AiModelService;
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

@Tag(name = "AI 模型")
@RestController
@RequestMapping("/ai/model")
@Validated
public class AiModelController {

    @Resource private AiModelService modelService;

    @PostMapping("/create")
    @Operation(summary = "创建模型")
    public CommonResult<Long> createModel(@Valid @RequestBody AiModelSaveReqVO createReqVO) {
        return success(modelService.createModel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新模型")
    public CommonResult<Boolean> updateModel(@Valid @RequestBody AiModelSaveReqVO updateReqVO) {
        modelService.updateModel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模型")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteModel(@RequestParam("id") Long id) {
        modelService.deleteModel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得模型")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AiModelRespVO> getModel(@RequestParam("id") Long id) {
        AiModelDO model = modelService.getModel(id);
        return success(BeanUtil.toBean(model, AiModelRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得模型分页")
    public CommonResult<PageResult<AiModelRespVO>> getModelPage(@Valid AiModelPageReqVO pageReqVO) {
        PageResult<AiModelDO> pageResult = modelService.getModelPage(pageReqVO);
        return success(BeanUtils.toPageResultBean(pageResult, AiModelRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得模型列表")
    @Parameter(name = "type", description = "类型", required = true, example = "1")
    @Parameter(name = "platform", description = "平台", example = "midjourney")
    public CommonResult<List<AiModelRespVO>> getModelSimpleList(
            @RequestParam("type") Integer type,
            @RequestParam(value = "platform", required = false) String platform) {
        List<AiModelDO> list =
                modelService.getModelListByStatusAndType(
                        CommonStatusEnum.ENABLE.getStatus(), type, platform);
        return success(
                convertList(
                        list,
                        model ->
                                new AiModelRespVO()
                                        .setId(model.getId())
                                        .setName(model.getName())
                                        .setModel(model.getModel())
                                        .setPlatform(model.getPlatform())));
    }
}
