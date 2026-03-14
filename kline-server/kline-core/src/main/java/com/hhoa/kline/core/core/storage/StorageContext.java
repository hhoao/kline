package com.hhoa.kline.core.core.storage;

import java.util.List;

/** 存储上下文接口 用于替代VSCode的ExtensionContext，提供状态和密钥的持久化能力 */
public interface StorageContext {
    /**
     * 获取全局存储路径
     *
     * @return 全局存储目录路径
     */
    String getGlobalStoragePath();

    /**
     * 获取工作区根路径列表
     *
     * @return 工作区根路径列表
     */
    List<String> getWorkspaceRoots();
}
