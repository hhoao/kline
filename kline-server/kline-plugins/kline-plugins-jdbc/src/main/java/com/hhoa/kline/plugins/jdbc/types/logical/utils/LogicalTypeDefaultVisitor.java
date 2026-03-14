/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hhoa.kline.plugins.jdbc.types.logical.utils;

import com.hhoa.kline.plugins.jdbc.types.logical.ArrayType;
import com.hhoa.kline.plugins.jdbc.types.logical.BigIntType;
import com.hhoa.kline.plugins.jdbc.types.logical.BinaryType;
import com.hhoa.kline.plugins.jdbc.types.logical.BooleanType;
import com.hhoa.kline.plugins.jdbc.types.logical.CharType;
import com.hhoa.kline.plugins.jdbc.types.logical.DateType;
import com.hhoa.kline.plugins.jdbc.types.logical.DayTimeIntervalType;
import com.hhoa.kline.plugins.jdbc.types.logical.DecimalType;
import com.hhoa.kline.plugins.jdbc.types.logical.DoubleType;
import com.hhoa.kline.plugins.jdbc.types.logical.FloatType;
import com.hhoa.kline.plugins.jdbc.types.logical.IntType;
import com.hhoa.kline.plugins.jdbc.types.logical.LocalZonedTimestampType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeVisitor;
import com.hhoa.kline.plugins.jdbc.types.logical.MapType;
import com.hhoa.kline.plugins.jdbc.types.logical.NullType;
import com.hhoa.kline.plugins.jdbc.types.logical.SmallIntType;
import com.hhoa.kline.plugins.jdbc.types.logical.TimeType;
import com.hhoa.kline.plugins.jdbc.types.logical.TimestampType;
import com.hhoa.kline.plugins.jdbc.types.logical.TinyIntType;
import com.hhoa.kline.plugins.jdbc.types.logical.VarBinaryType;
import com.hhoa.kline.plugins.jdbc.types.logical.VarCharType;
import com.hhoa.kline.plugins.jdbc.types.logical.YearMonthIntervalType;
import com.hhoa.kline.plugins.jdbc.types.logical.ZonedTimestampType;

/**
 * Implementation of {@link LogicalTypeVisitor} that redirects all calls to {@link
 * LogicalTypeDefaultVisitor#defaultMethod(LogicalType)}.
 */
public abstract class LogicalTypeDefaultVisitor<R> implements LogicalTypeVisitor<R> {

    @Override
    public R visit(CharType charType) {
        return defaultMethod(charType);
    }

    @Override
    public R visit(VarCharType varCharType) {
        return defaultMethod(varCharType);
    }

    @Override
    public R visit(BooleanType booleanType) {
        return defaultMethod(booleanType);
    }

    @Override
    public R visit(BinaryType binaryType) {
        return defaultMethod(binaryType);
    }

    @Override
    public R visit(VarBinaryType varBinaryType) {
        return defaultMethod(varBinaryType);
    }

    @Override
    public R visit(DecimalType decimalType) {
        return defaultMethod(decimalType);
    }

    @Override
    public R visit(TinyIntType tinyIntType) {
        return defaultMethod(tinyIntType);
    }

    @Override
    public R visit(SmallIntType smallIntType) {
        return defaultMethod(smallIntType);
    }

    @Override
    public R visit(IntType intType) {
        return defaultMethod(intType);
    }

    @Override
    public R visit(BigIntType bigIntType) {
        return defaultMethod(bigIntType);
    }

    @Override
    public R visit(FloatType floatType) {
        return defaultMethod(floatType);
    }

    @Override
    public R visit(DoubleType doubleType) {
        return defaultMethod(doubleType);
    }

    @Override
    public R visit(DateType dateType) {
        return defaultMethod(dateType);
    }

    @Override
    public R visit(TimeType timeType) {
        return defaultMethod(timeType);
    }

    @Override
    public R visit(TimestampType timestampType) {
        return defaultMethod(timestampType);
    }

    @Override
    public R visit(ZonedTimestampType zonedTimestampType) {
        return defaultMethod(zonedTimestampType);
    }

    @Override
    public R visit(LocalZonedTimestampType localZonedTimestampType) {
        return defaultMethod(localZonedTimestampType);
    }

    @Override
    public R visit(YearMonthIntervalType yearMonthIntervalType) {
        return defaultMethod(yearMonthIntervalType);
    }

    @Override
    public R visit(DayTimeIntervalType dayTimeIntervalType) {
        return defaultMethod(dayTimeIntervalType);
    }

    @Override
    public R visit(ArrayType arrayType) {
        return defaultMethod(arrayType);
    }

    @Override
    public R visit(MapType mapType) {
        return defaultMethod(mapType);
    }

    @Override
    public R visit(NullType nullType) {
        return defaultMethod(nullType);
    }

    @Override
    public R visit(LogicalType other) {
        return defaultMethod(other);
    }

    protected abstract R defaultMethod(LogicalType logicalType);
}
