package com.hhoa.kline.web.service;

import com.hhoa.kline.web.common.pojo.PageResult;
import com.hhoa.kline.web.dal.dataobject.AiToolDO;
import com.hhoa.kline.web.dto.model.tool.AiToolPageReqVO;
import com.hhoa.kline.web.dto.model.tool.AiToolSaveReqVO;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/** AI 工具 Service 接口 */
public interface AiToolService {
    List<McpSchema.Tool> getMcpTools();

    String callTool(String toolName, String argsJson);

    List<ToolCallback> getCommonTools();

    /**
     * 创建工具
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTool(@Valid AiToolSaveReqVO createReqVO);

    /**
     * 更新工具
     *
     * @param updateReqVO 更新信息
     */
    void updateTool(@Valid AiToolSaveReqVO updateReqVO);

    /**
     * 删除工具
     *
     * @param id 编号
     */
    void deleteTool(Long id);

    /**
     * 校验工具是否存在
     *
     * @param id 编号
     */
    void validateToolExists(Long id);

    /**
     * 获得工具
     *
     * @param id 编号
     * @return 工具
     */
    AiToolDO getTool(Long id);

    /**
     * 获得工具列表
     *
     * @param ids 编号列表
     * @return 工具列表
     */
    List<AiToolDO> getToolList(Collection<Long> ids);

    /**
     * 获得工具分页
     *
     * @param pageReqVO 分页查询
     * @return 工具分页
     */
    PageResult<AiToolDO> getToolPage(AiToolPageReqVO pageReqVO);

    /**
     * 获得工具列表
     *
     * @param status 状态
     * @return 工具列表
     */
    List<AiToolDO> getToolListByStatus(Integer status);
}
