package com.hhoa.kline.core.core.locks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 锁记录，对应数据库 locks 表中的一行。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockRow {
    private long id;
    private String heldBy;
    private LockType lockType;

    /** 锁目标，具体含义取决于 lockType：文件路径 / 主机地址 / 文件夹路径 */
    private String lockTarget;

    private long lockedAt;
}
