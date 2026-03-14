//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.hhoa.kline.plugins.jdbc.types;

import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.utils.ClassDataTypeConverter;
import com.hhoa.kline.plugins.jdbc.types.utils.LogicalTypeDataTypeConverter;
import java.util.Optional;
import java.util.stream.Stream;

public final class TypeConversions {

    public static Optional<DataType> fromClassToDataType(Class<?> clazz) {
        return ClassDataTypeConverter.extractDataType(clazz);
    }

    public static DataType fromLogicalToDataType(LogicalType logicalType) {
        return LogicalTypeDataTypeConverter.toDataType(logicalType);
    }

    public static DataType[] fromLogicalToDataType(LogicalType[] logicalTypes) {
        return (DataType[])
                Stream.of(logicalTypes)
                        .map(LogicalTypeDataTypeConverter::toDataType)
                        .toArray(DataType[]::new);
    }

    public static LogicalType fromDataToLogicalType(DataType dataType) {
        return dataType.getLogicalType();
    }

    public static LogicalType[] fromDataToLogicalType(DataType[] dataTypes) {
        return (LogicalType[])
                Stream.of(dataTypes)
                        .map(TypeConversions::fromDataToLogicalType)
                        .toArray(LogicalType[]::new);
    }

    private TypeConversions() {}
}
