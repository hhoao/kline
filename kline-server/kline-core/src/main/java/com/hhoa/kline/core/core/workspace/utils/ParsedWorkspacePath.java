package com.hhoa.kline.core.core.workspace.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示带有可选工作区提示的已解析工作区路径
 *
 * <p>此类保存解析可能包含工作区提示前缀的路径的结果。 工作区提示用于在多根环境中定位特定工作区。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedWorkspacePath {

    private String workspaceHint;

    private String relPath;
}
