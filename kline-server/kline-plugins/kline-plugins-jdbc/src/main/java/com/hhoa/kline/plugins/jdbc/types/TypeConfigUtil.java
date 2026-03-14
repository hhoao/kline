package com.hhoa.kline.plugins.jdbc.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class TypeConfigUtil {
    // private static final Pattern COMPLEX_PATTERN =
    // Pattern.compile("(?<type1>\\D+)(\\(\\s*(?<precision1>\\d)\\s*\\))?(?<type2>\\D+)(\\(\\s*(?<scale1>\\d)\\s*\\))?(\\(\\s*(?<precision2>\\d)\\s*(,\\s*(?<scale2>\\d)\\s*)?\\))?");
    private static final Pattern COMPLEX_PATTERN =
            Pattern.compile(
                    "(?<prefixType>[A-Za-z0-9\\s]+)(\\(\\s*(?<precision>\\d+)\\s*\\))?(?<suffixType>[A-Za-z0-9\\s]+)(\\(\\s*(?<scale>\\d+)\\s*\\))?");

    private static final Pattern COMPLEX_PATTERN_2 =
            Pattern.compile(
                    "(?<prefixType>[A-Za-z0-9\\s]+)(\\(\\s*(?<precision>\\d+)\\s*(,\\s*(?<scale>\\d+)\\s*)?\\))(?<suffixType>[A-Za-z0-9\\s]+)");

    private static final Pattern NORMAL_PATTERN =
            Pattern.compile(
                    "(?<type>[_A-Za-z0-9]+)(\\(\\s*(?<precision>\\d+)\\s*(,\\s*(?<scale>\\d+)\\s*)?\\))?");

    private static final Pattern NORMAL_PATTERN_WITH_SPACE =
            Pattern.compile(
                    "(?<type>[_A-Za-z0-9\\s]+)(\\(\\s*(?<precision>\\d+)\\s*(,\\s*(?<scale>\\d+)\\s*)?\\))?$");

    private static final Pattern ALL_WORD_PATTERN = Pattern.compile("(?<type>[_A-Za-z0-9\\s]+)");
    private static final Pattern ALL_WORD_SPECIAL_PATTERN =
            Pattern.compile("(?<type>[_A-Za-z0-9]+)(\\(\\s*(?<specialStr>[_A-Za-z]+)\\))?");

    public static TypeConfig getTypeConf(String typeStr, Function<String, String> converter) {
        // 处理 PostgreSQL 数组格式（如 integer[]、text[]）转换为 ARRAY<...> 格式
        typeStr = convertPostgreSQLArrayFormat(typeStr);

        String removeUselessSpaceTypeStr = removeUselessSpace(typeStr);
        if (ColumnType.isComplexType(ColumnType.getType(removeUselessSpaceTypeStr))) {
            return buildCollectionTypeConfig(typeStr, converter);
        }
        TypeConfig typeConfig;
        typeConfig = getAllWordTypeConf(removeUselessSpaceTypeStr);
        if (typeConfig != null) {
            return typeConfig;
        }
        typeConfig = getNormalTypeConf(removeUselessSpaceTypeStr);
        if (typeConfig != null) {
            return typeConfig;
        }
        typeConfig = getNormalTypeConfWithSpace(removeUselessSpaceTypeStr);
        if (typeConfig != null) {
            return typeConfig;
        }
        typeConfig = getComplexTypeConf(removeUselessSpaceTypeStr);
        if (typeConfig != null) {
            return typeConfig;
        }
        typeConfig = getComplex2TypeConf(removeUselessSpaceTypeStr);
        if (typeConfig != null) {
            return typeConfig;
        }
        throw new RuntimeException("typeStr is not support: " + typeStr);
    }

    /**
     * 将 PostgreSQL 数组格式（如 integer[]、text[]）转换为 ARRAY<...> 格式 支持嵌套数组，如 integer[][] ->
     * ARRAY<ARRAY<INTEGER>>
     *
     * @param typeStr 原始类型字符串
     * @return 转换后的类型字符串
     */
    private static String convertPostgreSQLArrayFormat(String typeStr) {
        if (typeStr == null || !typeStr.contains("[]")) {
            return typeStr;
        }

        // 计算数组维度（[] 的数量）
        int arrayDimensions = 0;
        String baseType = typeStr;
        while (baseType.endsWith("[]")) {
            arrayDimensions++;
            baseType = baseType.substring(0, baseType.length() - 2);
        }
        baseType = baseType.trim();

        // 将基础类型转换为大写
        String upperBaseType = baseType.toUpperCase(Locale.ENGLISH);

        // 构建 ARRAY<...> 格式
        StringBuilder result = new StringBuilder("ARRAY");
        for (int i = 0; i < arrayDimensions; i++) {
            if (i == arrayDimensions - 1) {
                // 最内层，添加基础类型
                result.append("<").append(upperBaseType).append(">");
            } else {
                // 外层数组
                result.append("<ARRAY");
            }
        }
        // 闭合所有外层数组的尖括号
        for (int i = 0; i < arrayDimensions - 1; i++) {
            result.append(">");
        }

        return result.toString();
    }

    private static String removeUselessSpace(String typeStr) {
        typeStr = typeStr.toUpperCase(Locale.ENGLISH).trim();
        if (typeStr.endsWith("NOT NULL")) {
            typeStr = typeStr.substring(0, typeStr.length() - 8);
        }
        typeStr = typeStr.trim();
        // todo Handle type information in the '<>'
        int lessThanIndex = typeStr.indexOf(ConstantValue.LESS_THAN_SIGN);
        if (lessThanIndex != -1 && typeStr.contains(ConstantValue.GREATER_THAN_SIGN)) {
            typeStr = typeStr.substring(0, lessThanIndex);
        }
        return typeStr;
    }

    private static TypeConfig getComplexTypeConf(String typeStr) {
        Matcher matcher = COMPLEX_PATTERN.matcher(typeStr);
        if (matcher.matches()) {
            String prefixType = matcher.group("prefixType");
            String suffixType = matcher.group("suffixType");
            String precision = matcher.group("precision");
            String scale = matcher.group("scale");
            return new TypeConfig(
                    prefixType,
                    suffixType,
                    StringUtils.isEmpty(precision) ? null : Integer.parseInt(precision),
                    StringUtils.isEmpty(scale) ? null : Integer.parseInt(scale));
        }
        return null;
    }

    private static TypeConfig getComplex2TypeConf(String typeStr) {
        Matcher matcher = COMPLEX_PATTERN_2.matcher(typeStr);
        if (matcher.matches()) {
            String prefixType = matcher.group("prefixType");
            String suffixType = matcher.group("suffixType");
            String precision = matcher.group("precision");
            String scale = matcher.group("scale");
            return new TypeConfig(
                    prefixType,
                    suffixType,
                    StringUtils.isEmpty(precision) ? null : Integer.parseInt(precision),
                    StringUtils.isEmpty(scale) ? null : Integer.parseInt(scale));
        }
        return null;
    }

    private static TypeConfig getNormalTypeConf(String typeStr) {
        Matcher matcher = NORMAL_PATTERN.matcher(typeStr);
        if (matcher.matches()) {
            String type = matcher.group("type");
            String precision = matcher.group("precision");
            String scale = matcher.group("scale");
            return new TypeConfig(
                    type,
                    StringUtils.isEmpty(precision) ? null : Integer.parseInt(precision),
                    StringUtils.isEmpty(scale) ? null : Integer.parseInt(scale));
        }
        return null;
    }

    private static TypeConfig getNormalTypeConfWithSpace(String typeStr) {
        Matcher matcher = NORMAL_PATTERN_WITH_SPACE.matcher(typeStr);
        if (matcher.matches()) {
            String type = matcher.group("type");
            String precision = matcher.group("precision");
            String scale = matcher.group("scale");
            // 只处理带括号的类型（有 precision 或 scale），避免与 getAllWordTypeConf 重复
            if (precision != null || scale != null) {
                return new TypeConfig(
                        type.trim(),
                        StringUtils.isEmpty(precision) ? null : Integer.parseInt(precision),
                        StringUtils.isEmpty(scale) ? null : Integer.parseInt(scale));
            }
        }
        return null;
    }

    private static TypeConfig getAllWordTypeConf(String typeStr) {
        Matcher matcher = ALL_WORD_PATTERN.matcher(typeStr);
        String type = null;
        if (matcher.matches()) {
            type = matcher.group("type");
        }
        matcher = ALL_WORD_SPECIAL_PATTERN.matcher(typeStr);
        if (matcher.matches()) {
            type = matcher.group("type");
            String specialStr = matcher.group("specialStr");
            if (specialStr != null) {
                type = type + "(" + specialStr + ")";
            }
        }
        if (type != null) {
            return new TypeConfig(type, null, null);
        }
        return null;
    }

    // MAP<ARRAY<ARRAY<INT>>, MAP<INT, STRING>>
    private static TypeConfig buildCollectionTypeConfig(
            String typeStr, Function<String, String> converter) {
        if (!typeStr.startsWith("MAP") && !typeStr.startsWith("ARRAY")) {
            if (converter != null) {
                return new TypeConfig(converter.apply(typeStr), null);
            } else {
                return new TypeConfig(typeStr, null);
            }
        }
        int i1 = typeStr.indexOf('<');
        String rootType = typeStr.substring(0, i1);
        List<TypeConfig> types = new ArrayList<>();
        int z = 0;
        StringBuilder s = new StringBuilder();
        for (int i = i1 + 1; i < typeStr.length(); i++) {
            char c = typeStr.charAt(i);
            // 只在分隔符前后的空格才跳过，保留类型名称中间的空格（如 CHARACTER VARYING）
            // 检查是否是分隔符前后的空格
            boolean isSpaceAroundDelimiter = false;
            if (c == ' ') {
                // 检查前一个字符是否是分隔符
                if (i > i1 + 1) {
                    char prevChar = typeStr.charAt(i - 1);
                    if (prevChar == ',' || prevChar == '<') {
                        isSpaceAroundDelimiter = true;
                    }
                }
                // 检查下一个字符是否是分隔符
                if (i < typeStr.length() - 1) {
                    char nextChar = typeStr.charAt(i + 1);
                    if (nextChar == ',' || nextChar == '>') {
                        isSpaceAroundDelimiter = true;
                    }
                }
                // 只在分隔符前后的空格才跳过
                if (isSpaceAroundDelimiter) {
                    continue;
                }
            }
            if (c == '<') {
                z++;
            } else if (c == '>') {
                z--;
            }
            if (c == ',' && z == 0) {
                types.add(buildCollectionTypeConfig(s.toString().trim(), converter));
                s = new StringBuilder();
                continue;
            }
            if (z == -1) {
                types.add(buildCollectionTypeConfig(s.toString().trim(), converter));
                break;
            }
            s.append(c);
        }
        return new TypeConfig(rootType, types);
    }
}
