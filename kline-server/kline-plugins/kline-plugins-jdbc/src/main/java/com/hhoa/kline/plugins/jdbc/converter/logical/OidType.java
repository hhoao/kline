package com.hhoa.kline.plugins.jdbc.converter.logical;

import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeVisitor;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.postgresql.core.Oid;

public class OidType extends PgCustomType {

    private static final Set<String> INPUT_CONVERSION = conversionSet(Long.class.getName());

    private static final Class<?> DEFAULT_CONVERSION = Long.class;

    private static final Set<String> OUTPUT_CONVERSION = conversionSet(Reader.class.getName());

    public OidType(boolean isNullable, LogicalTypeRoot typeRoot, Boolean isArray) {
        super(isNullable, typeRoot, isArray, Oid.INT8_ARRAY);
    }

    @Override
    public String asSerializableString() {
        return "PG-OID";
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
        return new OidType(isNullable, getTypeRoot(), isArray());
    }
}
