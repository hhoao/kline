package com.hhoa.kline.core.core.locks;

/**
 * 锁管理器接口。
 *
 * <p>提供实例注册、文件夹锁注册/释放等能力。 在 Cline TS 版本中使用 SQLite 实现，Kline 中可以使用 内存/文件/数据库等不同后端实现。
 */
public interface LockManager {

    /** 注册当前实例 */
    void registerInstance(String hostAddress);

    /** 更新实例心跳 */
    void touchInstance();

    /** 注销当前实例 */
    void unregisterInstance();

    /** 查询指定端口的实例 */
    InstanceInfo getInstanceByPort(int port);

    /** 删除指定地址的实例 */
    void removeInstanceByAddress(String instanceAddress);

    /** 查询指定目标的文件夹锁 */
    LockRow getFolderLockByTarget(String lockTarget);

    /**
     * 注册文件夹锁。
     *
     * @return null 表示成功获取锁；否则返回冲突的 LockRow
     */
    LockRow registerFolderLock(String heldBy, String lockTarget);

    /** 释放文件夹锁 */
    void releaseFolderLockByTarget(String heldBy, String lockTarget);

    /** 清理孤立的文件夹锁（其所属实例已不存在） */
    void cleanupOrphanedFolderLocks();

    /** 关闭锁管理器 */
    void close();

    /** 实例信息 */
    record InstanceInfo(String instanceAddress, String hostAddress) {}
}
