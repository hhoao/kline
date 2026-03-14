package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ArrayUtils {
    /**
     * @param array 要搜索的源数组
     * @param predicate 谓词函数，对数组中的每个元素调用一次，按降序排列，直到找到一个返回 true 的元素
     * @return 满足条件的最后一个元素的索引，如果没有找到则返回 -1
     */
    public static <T> int findLastIndex(List<T> array, Predicate<T> predicate) {
        if (array == null || array.isEmpty()) {
            return -1;
        }
        for (int i = array.size() - 1; i >= 0; i--) {
            if (predicate.test(array.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param array 要搜索的源数组
     * @param predicate 谓词函数，对数组中的每个元素调用一次，按降序排列，直到找到一个返回 true 的元素
     * @return 满足条件的最后一个元素，如果没有找到则返回 null
     */
    public static <T> T findLast(List<T> array, Predicate<T> predicate) {
        int index = findLastIndex(array, predicate);
        return index == -1 ? null : array.get(index);
    }

    /**
     * parsePartialArrayString
     *
     * @param arrayString 数组的字符串表示，可能不完整
     * @return 从输入解析的字符串数组
     */
    public static List<String> parsePartialArrayString(String arrayString) {
        if (arrayString == null || arrayString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    arrayString,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            // 如果 JSON 解析失败，处理为部分字符串
            String trimmed = arrayString.trim();
            if (!trimmed.startsWith("[\"")) {
                return new ArrayList<>();
            }

            String content = trimmed.substring(2);
            content = content.replaceAll("\"]$", "");
            if (content.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> result = new ArrayList<>();
            String[] parts = content.split("\", \"");
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty()) {
                    result.add(trimmedPart);
                }
            }
            return result;
        }
    }
}
