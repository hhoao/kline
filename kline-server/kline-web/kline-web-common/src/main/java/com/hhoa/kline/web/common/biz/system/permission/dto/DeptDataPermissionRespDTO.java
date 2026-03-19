package com.hhoa.kline.web.common.biz.system.permission.dto;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;

/** 部门的数据权限 Response DTO */
@Data
public class DeptDataPermissionRespDTO {

    /** 是否可查看全部数据 */
    private Boolean all;

    /** 是否可查看自己的数据 */
    private Boolean self;

    /** 可查看的部门编号数组 */
    private Set<Long> deptIds;

    public DeptDataPermissionRespDTO() {
        this.all = false;
        this.self = false;
        this.deptIds = new HashSet<>();
    }
}
