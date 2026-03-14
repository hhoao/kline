//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.hhoa.kline.plugins.jdbc.types;

import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;

public interface DataTypeFactory {
    DataType createDataType(AbstractDataType<?> var1);

    DataType createDataType(String var1);

    <T> DataType createDataType(Class<T> var1);

    LogicalType createLogicalType(String var1);
}
