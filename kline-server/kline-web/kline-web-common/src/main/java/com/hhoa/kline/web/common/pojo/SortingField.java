package com.hhoa.kline.web.common.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排序字段 DTO
 *
 * <p>类名加了 ing 的原因是，避免和 ES SortField 重名。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortingField implements Serializable {

    /** 顺序 - 升序 */
    public static final String ORDER_ASC = "asc";

    /** 顺序 - 降序 */
    public static final String ORDER_DESC = "desc";

    /** 字段 */
    @JsonAlias("fieldName")
    private String field;

    /** 顺序 */
    @Schema(
            description = "排序顺序",
            allowableValues = {"asc", "desc"})
    @JsonAlias({"direction", "sortExp"})
    private String order;
}
