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

package com.hhoa.kline.plugins.jdbc.types.utils;

import com.hhoa.kline.plugins.jdbc.types.AtomicDataType;
import com.hhoa.kline.plugins.jdbc.types.CollectionDataType;
import com.hhoa.kline.plugins.jdbc.types.DataType;
import com.hhoa.kline.plugins.jdbc.types.KeyValueDataType;
import com.hhoa.kline.plugins.jdbc.types.exception.ValidationException;
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
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;
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

/** A converter between {@link LogicalType} and {@link DataType}. */
public final class LogicalTypeDataTypeConverter {

    private static final DefaultDataTypeCreator dataTypeCreator = new DefaultDataTypeCreator();

    /** Returns the data type of a logical type without explicit conversions. */
    public static DataType toDataType(LogicalType logicalType) {
        return logicalType.accept(dataTypeCreator);
    }

    /** Returns the logical type of a data type. */
    public static LogicalType toLogicalType(DataType dataType) {
        return dataType.getLogicalType();
    }

    // --------------------------------------------------------------------------------------------

    private static class DefaultDataTypeCreator implements LogicalTypeVisitor<DataType> {

        @Override
        public DataType visit(CharType charType) {
            return new AtomicDataType(charType);
        }

        @Override
        public DataType visit(VarCharType varCharType) {
            return new AtomicDataType(varCharType);
        }

        @Override
        public DataType visit(BooleanType booleanType) {
            return new AtomicDataType(booleanType);
        }

        @Override
        public DataType visit(BinaryType binaryType) {
            return new AtomicDataType(binaryType);
        }

        @Override
        public DataType visit(VarBinaryType varBinaryType) {
            return new AtomicDataType(varBinaryType);
        }

        @Override
        public DataType visit(DecimalType decimalType) {
            return new AtomicDataType(decimalType);
        }

        @Override
        public DataType visit(TinyIntType tinyIntType) {
            return new AtomicDataType(tinyIntType);
        }

        @Override
        public DataType visit(SmallIntType smallIntType) {
            return new AtomicDataType(smallIntType);
        }

        @Override
        public DataType visit(IntType intType) {
            return new AtomicDataType(intType);
        }

        @Override
        public DataType visit(BigIntType bigIntType) {
            return new AtomicDataType(bigIntType);
        }

        @Override
        public DataType visit(FloatType floatType) {
            return new AtomicDataType(floatType);
        }

        @Override
        public DataType visit(DoubleType doubleType) {
            return new AtomicDataType(doubleType);
        }

        @Override
        public DataType visit(DateType dateType) {
            return new AtomicDataType(dateType);
        }

        @Override
        public DataType visit(TimeType timeType) {
            return new AtomicDataType(timeType);
        }

        @Override
        public DataType visit(TimestampType timestampType) {
            return new AtomicDataType(timestampType);
        }

        @Override
        public DataType visit(ZonedTimestampType zonedTimestampType) {
            return new AtomicDataType(zonedTimestampType);
        }

        @Override
        public DataType visit(LocalZonedTimestampType localZonedTimestampType) {
            return new AtomicDataType(localZonedTimestampType);
        }

        @Override
        public DataType visit(YearMonthIntervalType yearMonthIntervalType) {
            return new AtomicDataType(yearMonthIntervalType);
        }

        @Override
        public DataType visit(DayTimeIntervalType dayTimeIntervalType) {
            return new AtomicDataType(dayTimeIntervalType);
        }

        @Override
        public DataType visit(ArrayType arrayType) {
            return new CollectionDataType(arrayType, arrayType.getElementType().accept(this));
        }

        @Override
        public DataType visit(MapType mapType) {
            return new KeyValueDataType(
                    mapType,
                    mapType.getKeyType().accept(this),
                    mapType.getValueType().accept(this));
        }

        @Override
        public DataType visit(NullType nullType) {
            return new AtomicDataType(nullType);
        }

        @Override
        public DataType visit(LogicalType other) {
            if (other.is(LogicalTypeRoot.UNRESOLVED)) {
                throw new ValidationException(
                        String.format(
                                "Unresolved logical type '%s' cannot be used to create a data type.",
                                other));
            }
            // for legacy types
            return new AtomicDataType(other);
        }
    }

    // --------------------------------------------------------------------------------------------

    private LogicalTypeDataTypeConverter() {
        // do not instantiate
    }
}
