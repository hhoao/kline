package com.hhoa.kline.core.core.locks;

import lombok.Builder;
import lombok.Data;

/** 带重试的文件夹锁请求结果。 */
@Data
@Builder
public class FolderLockWithRetryResult {
    /** 是否成功获取锁 */
    private boolean acquired;

    /** 锁请求被跳过（锁管理器不可用时） */
    private boolean skipped;

    /** 冲突的锁记录（获取失败时可用） */
    private LockRow conflictingLock;
}
