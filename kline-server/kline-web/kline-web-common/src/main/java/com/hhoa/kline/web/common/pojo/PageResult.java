package com.hhoa.kline.web.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Schema(description = "分页结果")
@Data
public final class PageResult<T> implements Serializable {

    @Schema(description = "数据", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> list;

    @Schema(description = "总量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long total;

    public PageResult() {}

    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }

    public PageResult(Long total) {
        this.list = new ArrayList<>();
        this.total = total;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L);
    }

    public static <T> PageResult<T> empty(Long total) {
        return new PageResult<>(total);
    }

    public static final <T, V> PageResult<T> convert(
            long total, List<V> items, PageResult.Converter<T, V> converter) {
        List<T> contents = items.stream().map(converter::convert).collect(Collectors.toList());
        PageResult<T> page = new PageResult<>(contents, total);
        return page;
    }

    public final <V> PageResult<V> map(PageResult.Converter<V, T> converter) {
        PageResult<V> page = new PageResult<>();
        page.setTotal(total);
        if (total == null) {
            page.setList(Collections.emptyList());
        } else {
            page.setList(list.stream().map(converter::convert).collect(Collectors.toList()));
        }
        return page;
    }

    @FunctionalInterface
    public interface Converter<T, V> {
        T convert(V v);
    }
}
