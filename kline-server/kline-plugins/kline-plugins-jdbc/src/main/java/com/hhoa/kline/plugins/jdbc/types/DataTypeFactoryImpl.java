//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.hhoa.kline.plugins.jdbc.types;

import com.hhoa.kline.plugins.jdbc.types.exception.ValidationException;
import com.hhoa.kline.plugins.jdbc.types.extraction.DataTypeExtractor;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.utils.LogicalTypeParser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DataTypeFactoryImpl implements DataTypeFactory {
    private final Map<Class<?>, DataType> cache = new ConcurrentHashMap<>();

    public DataType createDataType(AbstractDataType<?> abstractDataType) {
        if (abstractDataType instanceof DataType) {
            return (DataType) abstractDataType;
        } else if (abstractDataType instanceof UnresolvedDataType) {
            return ((UnresolvedDataType) abstractDataType).toDataType(this);
        } else {
            throw new ValidationException("Unsupported abstract data type.");
        }
    }

    public DataType createDataType(String typeString) {
        return TypeConversions.fromLogicalToDataType(this.createLogicalType(typeString));
    }

    public <T> DataType createDataType(Class<T> clazz) {
        DataType dataType = cache.get(clazz);
        if (dataType != null) {
            return dataType;
        }
        dataType = DataTypeExtractor.extractFromType(this, clazz);
        cache.put(clazz, dataType);
        return dataType;
    }

    public LogicalType createLogicalType(String typeString) {
        return LogicalTypeParser.parse(typeString);
    }
}
