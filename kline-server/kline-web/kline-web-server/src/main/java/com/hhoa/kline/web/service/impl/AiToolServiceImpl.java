package com.hhoa.kline.web.service.impl;

import static com.hhoa.ai.kline.commons.exception.util.ServiceExceptionUtil.exception;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.TOOL_NAME_NOT_EXISTS;
import static com.hhoa.kline.web.enums.AIErrorCodeConstants.TOOL_NOT_EXISTS;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.hhoa.kline.core.common.tool.common.CommonTool;
import com.hhoa.kline.web.common.mybatis.core.query.LambdaQueryWrapperX;
import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.AiToolMapper;
import com.hhoa.kline.web.dal.dataobject.AiToolDO;
import com.hhoa.kline.web.dto.model.tool.AiToolPageReqVO;
import com.hhoa.kline.web.dto.model.tool.AiToolSaveReqVO;
import com.hhoa.kline.web.service.AiToolService;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AiToolServiceImpl implements AiToolService {
    private final List<CommonTool> commonTools;
    private List<ToolCallback> commonToolCallbacks;

    public AiToolServiceImpl(List<CommonTool> commonTools) {
        this.commonTools = commonTools;
    }

    @Override
    public List<McpSchema.Tool> getMcpTools() {
        return McpToolUtils.toSyncToolSpecification(getCommonTools()).stream()
                .map(spec -> spec.tool())
                .toList();
    }

    @Override
    public List<ToolCallback> getCommonTools() {
        if (commonToolCallbacks == null) {
            ToolCallback[] from = ToolCallbacks.from(commonTools.toArray());
            commonToolCallbacks = List.of(from);
        }
        return commonToolCallbacks;
    }

    @Override
    public String callTool(String toolName, String argsJson) {
        ToolCallback cb =
                getCommonTools().stream()
                        .filter(c -> toolName.equals(c.getToolDefinition().name()))
                        .findFirst()
                        .orElse(null);
        if (cb == null) {
            return null;
        }
        return cb.call(argsJson, null);
    }

    @Resource private AiToolMapper toolMapper;

    @Override
    public Long createTool(AiToolSaveReqVO createReqVO) {
        validateToolNameExists(createReqVO.getName());
        AiToolDO tool = BeanUtil.toBean(createReqVO, AiToolDO.class);
        toolMapper.insert(tool);
        return tool.getId();
    }

    @Override
    public void updateTool(AiToolSaveReqVO updateReqVO) {
        validateToolExists(updateReqVO.getId());
        validateToolNameExists(updateReqVO.getName());
        AiToolDO updateObj = BeanUtil.toBean(updateReqVO, AiToolDO.class);
        toolMapper.updateById(updateObj);
    }

    @Override
    public void deleteTool(Long id) {
        validateToolExists(id);
        toolMapper.deleteById(id);
    }

    @Override
    public void validateToolExists(Long id) {
        if (toolMapper.selectById(id) == null) {
            throw exception(TOOL_NOT_EXISTS);
        }
    }

    private void validateToolNameExists(String name) {
        try {
            SpringUtil.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {
            throw exception(TOOL_NAME_NOT_EXISTS, name);
        }
    }

    @Override
    public AiToolDO getTool(Long id) {
        return toolMapper.selectById(id);
    }

    @Override
    public List<AiToolDO> getToolList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return toolMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<AiToolDO> getToolPage(AiToolPageReqVO pageReqVO) {
        var pr = toolMapper.selectPage(pageReqVO);
        return new PageResult<>(pr.getList(), pr.getTotal());
    }

    @Override
    public List<AiToolDO> getToolListByStatus(Integer status) {
        return toolMapper.selectList(
                new LambdaQueryWrapperX<AiToolDO>()
                        .eq(AiToolDO::getStatus, status)
                        .orderByAsc(AiToolDO::getId));
    }
}
