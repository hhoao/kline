package com.hhoa.ai.kline.commons.utils.object;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.hhoa.ai.kline.commons.utils.collection.CollectionUtils;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bean 工具类
 *
 * <p>1. 默认使用 {@link BeanUtil} 作为实现类，虽然不同 bean 工具的性能有差别，但是对绝大多数同学的项目，不用在意这点性能 2. 针对复杂的对象转换，可以搜参考
 * AuthConvert 实现，通过 mapstruct + default 配合实现
 */
public class BeanUtils {

    public static <T> T toBean(Object source, Class<T> targetClass) {
        return BeanUtil.toBean(source, targetClass);
    }

    public static <T> T toBean(Object source, Class<T> targetClass, Consumer<T> peek) {
        T target = toBean(source, targetClass);
        if (target != null) {
            peek.accept(target);
        }
        return target;
    }

    public static <S, T> List<T> toBean(List<S> source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        return CollectionUtils.convertList(source, s -> toBean(s, targetType));
    }

    public static <S, T> List<T> toBean(List<S> source, Class<T> targetType, Consumer<T> peek) {
        List<T> list = toBean(source, targetType);
        if (list != null) {
            list.forEach(peek);
        }
        return list;
    }

    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.copyProperties(source, target, false);
    }

    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.copyProperties(source, target, CopyOptions.create().ignoreNullValue());
    }
}
