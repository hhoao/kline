package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.MODEL_DEFAULT_NOT_EXISTS;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.MODEL_DISABLE;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.MODEL_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiChatMapper;
import com.hhoa.kline.web.dal.dataobject.AiApiKeyDO;
import com.hhoa.kline.web.dal.dataobject.AiModelDO;
import com.hhoa.kline.web.dto.model.model.AiModelPageReqVO;
import com.hhoa.kline.web.dto.model.model.AiModelSaveReqVO;
import com.hhoa.kline.web.enums.AiPlatformEnum;
import com.hhoa.kline.web.model.AiModelFactory;
import com.hhoa.kline.web.model.midjourney.api.MidjourneyApi;
import com.hhoa.kline.web.service.AiApiKeyService;
import com.hhoa.kline.web.service.AiModelService;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AiModelServiceImpl implements AiModelService {

    @Resource private AiApiKeyService apiKeyService;

    @Resource private AiChatMapper modelMapper;

    @Resource private AiModelFactory modelFactory;

    @Override
    public Long createModel(AiModelSaveReqVO createReqVO) {
        AiPlatformEnum.validatePlatform(createReqVO.getPlatform());
        apiKeyService.validateApiKey(createReqVO.getKeyId());

        AiModelDO model = BeanUtil.toBean(createReqVO, AiModelDO.class);
        modelMapper.insert(model);
        return model.getId();
    }

    @Override
    public void updateModel(AiModelSaveReqVO updateReqVO) {
        validateModelExists(updateReqVO.getId());
        AiPlatformEnum.validatePlatform(updateReqVO.getPlatform());
        apiKeyService.validateApiKey(updateReqVO.getKeyId());

        AiModelDO updateObj = BeanUtil.toBean(updateReqVO, AiModelDO.class);
        modelMapper.updateById(updateObj);
    }

    @Override
    public void deleteModel(Long id) {
        validateModelExists(id);
        modelMapper.deleteById(id);
    }

    private AiModelDO validateModelExists(Long id) {
        AiModelDO model = modelMapper.selectById(id);
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        return model;
    }

    @Override
    public AiModelDO getModel(Long id) {
        return modelMapper.selectById(id);
    }

    @Override
    public AiModelDO getRequiredDefaultModel(Integer type) {
        AiModelDO model =
                modelMapper.selectFirstByStatus(type, CommonStatusEnum.ENABLE.getStatus());
        if (model == null) {
            throw exception(MODEL_DEFAULT_NOT_EXISTS);
        }
        return model;
    }

    @Override
    public PageResult<AiModelDO> getModelPage(AiModelPageReqVO pageReqVO) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiModelDO> wrapper =
                new LambdaQueryWrapperX<AiModelDO>()
                        .likeIfPresent(AiModelDO::getName, pageReqVO.getName())
                        .eqIfPresent(AiModelDO::getModel, pageReqVO.getModel())
                        .eqIfPresent(AiModelDO::getPlatform, pageReqVO.getPlatform())
                        .orderByAsc(AiModelDO::getSort);
        PageResult<AiModelDO> result = modelMapper.selectPage(pageReqVO, wrapper);
        return new PageResult<>(result.getList(), result.getTotal());
    }

    @Override
    public AiModelDO validateModel(Long id) {
        AiModelDO model = validateModelExists(id);
        if (CommonStatusEnum.isDisable(model.getStatus())) {
            throw exception(MODEL_DISABLE);
        }
        return model;
    }

    @Override
    public List<AiModelDO> getModelListByStatusAndType(
            Integer status, Integer type, String platform) {
        return modelMapper.selectList(
                new LambdaQueryWrapperX<AiModelDO>()
                        .eqIfPresent(AiModelDO::getStatus, status)
                        .eqIfPresent(AiModelDO::getType, type)
                        .eqIfPresent(AiModelDO::getPlatform, platform)
                        .orderByAsc(AiModelDO::getSort));
    }

    @Override
    public ChatModel getChatModel(Long id) {
        AiModelDO model = validateModel(id);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(apiKey.getPlatform());
        return modelFactory.getOrCreateChatModel(platform, apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public ImageModel getImageModel(Long id) {
        AiModelDO model = validateModel(id);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(apiKey.getPlatform());
        return modelFactory.getOrCreateImageModel(platform, apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public MidjourneyApi getMidjourneyApi(Long id) {
        AiModelDO model = validateModel(id);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        return modelFactory.getOrCreateMidjourneyApi(apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public VectorStore getOrCreateVectorStore(Long id, Map<String, Class<?>> metadataFields) {
        AiModelDO model = validateModel(id);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(apiKey.getPlatform());

        EmbeddingModel embeddingModel =
                modelFactory.getOrCreateEmbeddingModel(
                        platform, apiKey.getApiKey(), apiKey.getUrl(), model.getModel());

        return modelFactory.getOrCreateVectorStore(
                SimpleVectorStore.class, embeddingModel, metadataFields);
    }
}
