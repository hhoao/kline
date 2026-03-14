package com.hhoa.kline.plugins.jdbc.converter.logical;

import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeVisitor;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.postgresql.core.Oid;

public class MoneyType extends PgCustomType {

    private static final Set<String> INPUT_CONVERSION = conversionSet(Double.class.getName());

    private static final Class<?> DEFAULT_CONVERSION = Double.class;

    private static final Set<String> OUTPUT_CONVERSION = conversionSet(Reader.class.getName());

    public MoneyType(boolean isNullable, LogicalTypeRoot typeRoot, boolean isArray) {
        super(isNullable, typeRoot, isArray, Oid.MONEY_ARRAY);
    }

    @Override
    public String asSerializableString() {
        return "PG-MONEY";
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
        return new MoneyType(isNullable, getTypeRoot(), isArray());
    }
}
