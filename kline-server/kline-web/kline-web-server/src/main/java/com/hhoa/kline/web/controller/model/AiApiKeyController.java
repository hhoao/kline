package com.hhoa.kline.web.controller.model;

import static com.hhoa.ai.kline.commons.utils.collection.CollectionUtils.convertList;
import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.common.utils.PageUtils;
import com.hhoa.kline.web.dal.dataobject.AiApiKeyDO;
import com.hhoa.kline.web.dto.AiApiKeyPageReqVO;
import com.hhoa.kline.web.dto.AiApiKeyRespVO;
import com.hhoa.kline.web.dto.AiApiKeySaveReqVO;
import com.hhoa.kline.web.dto.model.model.AiModelRespVO;
import com.hhoa.kline.web.service.AiApiKeyService;
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

@Tag(name = "AI API 密钥")
@RestController
@RequestMapping("/ai/api-key")
@Validated
public class AiApiKeyController {

    @Resource private AiApiKeyService apiKeyService;

    @PostMapping("/create")
    @Operation(summary = "创建 API 密钥")
    public CommonResult<Long> createApiKey(@Valid @RequestBody AiApiKeySaveReqVO createReqVO) {
        return success(apiKeyService.createApiKey(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 API 密钥")
    public CommonResult<Boolean> updateApiKey(@Valid @RequestBody AiApiKeySaveReqVO updateReqVO) {
        apiKeyService.updateApiKey(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 API 密钥")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteApiKey(@RequestParam("id") Long id) {
        apiKeyService.deleteApiKey(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 API 密钥")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AiApiKeyRespVO> getApiKey(@RequestParam("id") Long id) {
        AiApiKeyDO apiKey = apiKeyService.getApiKey(id);
        return success(BeanUtil.toBean(apiKey, AiApiKeyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 API 密钥分页")
    public CommonResult<PageResult<AiApiKeyRespVO>> getApiKeyPage(
            @Valid AiApiKeyPageReqVO pageReqVO) {
        PageResult<AiApiKeyDO> pageResult = apiKeyService.getApiKeyPage(pageReqVO);
        return success(PageUtils.toPageResult(pageResult, AiApiKeyRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得 API 密钥分页列表")
    public CommonResult<List<AiModelRespVO>> getApiKeySimpleList() {
        List<AiApiKeyDO> list = apiKeyService.getApiKeyList();
        return success(
                convertList(
                        list,
                        key -> new AiModelRespVO().setId(key.getId()).setName(key.getName())));
    }
}
