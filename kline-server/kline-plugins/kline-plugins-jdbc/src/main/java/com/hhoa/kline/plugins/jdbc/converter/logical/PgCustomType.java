package com.hhoa.kline.plugins.jdbc.converter.logical;

import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;

public abstract class PgCustomType extends LogicalType {

    private final boolean isArray;
    private final int arrayOid;

    public PgCustomType(
            boolean isNullable, LogicalTypeRoot typeRoot, boolean isArray, Integer oid) {
        super(isNullable, typeRoot);
        this.isArray = isArray;
        this.arrayOid = oid;
    }

    public boolean isArray() {
        return isArray;
    }

    public int getArrayOid() {
        return arrayOid;
    }
}
