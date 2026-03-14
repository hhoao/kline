package com.hhoa.kline.plugins.jdbc.dbfilemapping.enums;

/** 同步状态枚举 Represents the current state of the synchronization process */
public enum SyncState {
    /** 初始化中 - System is initializing */
    INITIALIZING,

    /** 同步中 - Actively synchronizing data */
    SYNCING,

    /** 空闲 - Idle, waiting for changes */
    IDLE,

    /** 错误 - Error state */
    ERROR,

    /** 已停止 - Stopped */
    STOPPED
}
