package com.hhoa.kline.plugins.jdbc.converter.logical;

import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeVisitor;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InetType extends PgCustomType {

    private static final Set<String> INPUT_CONVERSION = conversionSet(String.class.getName());

    private static final Class<?> DEFAULT_CONVERSION = String.class;

    private static final Set<String> OUTPUT_CONVERSION = conversionSet(Reader.class.getName());

    public InetType(boolean isNullable, LogicalTypeRoot typeRoot, boolean isArray) {
        // PostgreSQL INET array OID is 1041 (标准 PostgreSQL OID)
        // 注意：PostgreSQL JDBC 驱动可能没有定义 INET_ARRAY 常量，所以使用硬编码值
        super(isNullable, typeRoot, isArray, 1041);
    }

    @Override
    public String asSerializableString() {
        return "PG-INET";
    }

    @Override
    public boolean supportsInputConversion(Class<?> clazz) {
        return INPUT_CONVERSION.contains(clazz.getName());
    }

    @Override
    public boolean supportsOutputConversion(Class<?> clazz) {
        return OUTPUT_CONVERSION.contains(clazz.getName());
    }

    @Override
    public Class<?> getDefaultConversion() {
        return DEFAULT_CONVERSION;
    }

    @Override
    public List<LogicalType> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R> R accept(LogicalTypeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public LogicalType copy(boolean isNullable) {
        return new InetType(isNullable, getTypeRoot(), isArray());
    }
}
