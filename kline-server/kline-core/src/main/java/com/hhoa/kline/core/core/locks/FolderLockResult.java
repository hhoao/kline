package com.hhoa.kline.core.core.locks;

import lombok.Builder;
import lombok.Data;

/** 单次文件夹锁请求结果。 */
@Data
@Builder
public class FolderLockResult {
    /** 是否成功获取锁 */
    private boolean acquired;

    /** 冲突的锁记录（获取失败时可用） */
    private LockRow conflictingLock;
}
