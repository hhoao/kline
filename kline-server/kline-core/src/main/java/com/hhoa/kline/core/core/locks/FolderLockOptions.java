package com.hhoa.kline.core.core.locks;

import lombok.Builder;
import lombok.Data;

/** 文件夹锁请求参数。 */
@Data
@Builder
public class FolderLockOptions {
    /** 要锁定的文件夹的 cwdHash */
    private String lockTarget;

    /** 持有锁的 taskId */
    private String heldBy;
}
