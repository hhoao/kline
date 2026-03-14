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

package com.hhoa.kline.plugins.jdbc.types;

import com.hhoa.ai.kline.commons.utils.Preconditions;
import com.hhoa.kline.plugins.jdbc.types.exception.ValidationException;
import com.hhoa.kline.plugins.jdbc.types.logical.ArrayType;
import com.hhoa.kline.plugins.jdbc.types.logical.BigIntType;
import com.hhoa.kline.plugins.jdbc.types.logical.BinaryType;
import com.hhoa.kline.plugins.jdbc.types.logical.BooleanType;
import com.hhoa.kline.plugins.jdbc.types.logical.CharType;
import com.hhoa.kline.plugins.jdbc.types.logical.DateType;
import com.hhoa.kline.plugins.jdbc.types.logical.DayTimeIntervalType;
import com.hhoa.kline.plugins.jdbc.types.logical.DayTimeIntervalType.DayTimeResolution;
import com.hhoa.kline.plugins.jdbc.types.logical.DecimalType;
import com.hhoa.kline.plugins.jdbc.types.logical.DoubleType;
import com.hhoa.kline.plugins.jdbc.types.logical.FloatType;
import com.hhoa.kline.plugins.jdbc.types.logical.IntType;
import com.hhoa.kline.plugins.jdbc.types.logical.LocalZonedTimestampType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.MapType;
import com.hhoa.kline.plugins.jdbc.types.logical.NullType;
import com.hhoa.kline.plugins.jdbc.types.logical.SmallIntType;
import com.hhoa.kline.plugins.jdbc.types.logical.TimeType;
import com.hhoa.kline.plugins.jdbc.types.logical.TimestampType;
import com.hhoa.kline.plugins.jdbc.types.logical.TinyIntType;
import com.hhoa.kline.plugins.jdbc.types.logical.VarBinaryType;
import com.hhoa.kline.plugins.jdbc.types.logical.VarCharType;
import com.hhoa.kline.plugins.jdbc.types.logical.YearMonthIntervalType;
import com.hhoa.kline.plugins.jdbc.types.logical.YearMonthIntervalType.YearMonthResolution;
import com.hhoa.kline.plugins.jdbc.types.logical.ZonedTimestampType;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link DataType} can be used to declare input and/or output types of operations. This class
 * enumerates all pre-defined data types of the Table & SQL API.
 *
 * <p>For convenience, this class also contains methods for creating {@link UnresolvedDataType}s
 * that need to be resolved at later stages. This is in particular useful for more complex types
 * that are expressed as {@link Class} (see {@link #of(Class)}) or types that need to be looked up
 * in a catalog (see {@link #of(String)}).
 *
 * <p>NOTE: Planners might not support every data type with the desired precision or parameter.
 * Please see the planner compatibility and limitations section in the website documentation before
 * using a data type.
 */
public final class DataTypes {

    /**
     * Creates a {@link DataType} from a {@link LogicalType} with default conversion class.
     *
     * @return the {@link LogicalType} converted to a {@link DataType}.
     * @see LogicalType#getDefaultConversion()
     */
    public static DataType of(LogicalType logicalType) {
        return TypeConversions.fromLogicalToDataType(logicalType);
    }

    /**
     * Creates an unresolved type that will be resolved to a {@link DataType} by analyzing the given
     * class later.
     *
     * <p>During the resolution, Java reflection is used which can be supported by {@link
     * DataTypeHint} annotations for nested, structured types.
     *
     * <p>It will throw an {@link ValidationException} in cases where the reflective extraction
     * needs more information or simply fails.
     *
     * <p>The following examples show how to use and enrich the extraction process:
     *
     * <pre>{@code
     * // returns INT
     * of(Integer.class)
     *
     * // returns TIMESTAMP(9)
     * of(java.time.LocalDateTime.class)
     *
     * // returns an anonymous, unregistered structured type
     * // that is deeply integrated into the API compared to opaque RAW types
     * class User {
     *
     *   // extract fields automatically
     *   public String name;
     *   public int age;
     *
     *   // enrich the extraction with precision information
     *   public @DataTypeHint("DECIMAL(10,2)") BigDecimal accountBalance;
     *
     *   // enrich the extraction with forcing using RAW types
     *   public @DataTypeHint(forceRawPattern = "scala.") Address address;
     *
     *   // enrich the extraction by specifying defaults
     *   public @DataTypeHint(defaultSecondPrecision = 3) Log log;
     * }
     * of(User.class)
     * }</pre>
     *
     * <p>Note: In most of the cases, the {@link UnresolvedDataType} will be automatically resolved
     * by the API. At other locations, a {@link DataTypeFactory} is provided.
     */
    public static UnresolvedDataType of(Class<?> unresolvedClass) {
        Preconditions.checkNotNull(unresolvedClass, "Unresolved class name must not be null.");
        return new UnresolvedDataType(
                () -> String.format("'%s'", unresolvedClass.getName()),
                (factory) -> factory.createDataType(unresolvedClass));
    }

    /**
     * Creates an unresolved type that will be resolved to a {@link DataType} by using a fully or
     * partially defined name.
     *
     * <p>It includes both built-in types (e.g. "INT") as well as user-defined types (e.g.
     * "mycat.mydb.Money").
     *
     * <p>Note: In most of the cases, the {@link UnresolvedDataType} will be automatically resolved
     * by the API. At other locations, a {@link DataTypeFactory} is provided.
     */
    public static UnresolvedDataType of(String unresolvedName) {
        Preconditions.checkNotNull(unresolvedName, "Unresolved name must not be null.");
        return new UnresolvedDataType(
                () -> unresolvedName, (factory) -> factory.createDataType(unresolvedName));
    }

    // we use SQL-like naming for data types and avoid Java keyword clashes
    // CHECKSTYLE.OFF: MethodName

    /**
     * Data type of a fixed-length character string {@code CHAR(n)} where {@code n} is the number of
     * code points. {@code n} must have a value between 1 and {@link Integer#MAX_VALUE} (both
     * inclusive).
     *
     * @see CharType
     */
    public static DataType CHAR(int n) {
        return new AtomicDataType(new CharType(n));
    }

    /**
     * Data type of a variable-length character string {@code VARCHAR(n)} where {@code n} is the
     * maximum number of code points. {@code n} must have a value between 1 and {@link
     * Integer#MAX_VALUE} (both inclusive).
     *
     * @see VarCharType
     */
    public static DataType VARCHAR(int n) {
        return new AtomicDataType(new VarCharType(n));
    }

    /**
     * Data type of a variable-length character string with defined maximum length. This is a
     * shortcut for {@code VARCHAR(2147483647)} for representing JVM strings.
     *
     * @see VarCharType
     */
    public static DataType STRING() {
        return VARCHAR(Integer.MAX_VALUE);
    }

    /**
     * Data type of a boolean with a (possibly) three-valued logic of {@code TRUE, FALSE, UNKNOWN}.
     *
     * @see BooleanType
     */
    public static DataType BOOLEAN() {
        return new AtomicDataType(new BooleanType());
    }

    /**
     * Data type of a fixed-length binary string (=a sequence of bytes) {@code BINARY(n)} where
     * {@code n} is the number of bytes. {@code n} must have a value between 1 and {@link
     * Integer#MAX_VALUE} (both inclusive).
     *
     * @see BinaryType
     */
    public static DataType BINARY(int n) {
        return new AtomicDataType(new BinaryType(n));
    }

    /**
     * Data type of a variable-length binary string (=a sequence of bytes) {@code VARBINARY(n)}
     * where {@code n} is the maximum number of bytes. {@code n} must have a value between 1 and
     * {@link Integer#MAX_VALUE} (both inclusive).
     *
     * @see VarBinaryType
     */
    public static DataType VARBINARY(int n) {
        return new AtomicDataType(new VarBinaryType(n));
    }

    /**
     * Data type of a variable-length binary string (=a sequence of bytes) with defined maximum
     * length. This is a shortcut for {@code VARBINARY(2147483647)} for representing JVM byte
     * arrays.
     *
     * @see VarBinaryType
     */
    public static DataType BYTES() {
        return VARBINARY(Integer.MAX_VALUE);
    }

    /**
     * Data type of a decimal number with fixed precision and scale {@code DECIMAL(p, s)} where
     * {@code p} is the number of digits in a number (=precision) and {@code s} is the number of
     * digits to the right of the decimal point in a number (=scale). {@code p} must have a value
     * between 1 and 38 (both inclusive). {@code s} must have a value between 0 and {@code p} (both
     * inclusive).
     *
     * @see DecimalType
     */
    public static DataType DECIMAL(int precision, int scale) {
        return new AtomicDataType(new DecimalType(precision, scale));
    }

    /**
     * Data type of a 1-byte signed integer with values from -128 to 127.
     *
     * @see TinyIntType
     */
    public static DataType TINYINT() {
        return new AtomicDataType(new TinyIntType());
    }

    /**
     * Data type of a 2-byte signed integer with values from -32,768 to 32,767.
     *
     * @see SmallIntType
     */
    public static DataType SMALLINT() {
        return new AtomicDataType(new SmallIntType());
    }

    /**
     * Data type of a 4-byte signed integer with values from -2,147,483,648 to 2,147,483,647.
     *
     * @see IntType
     */
    public static DataType INT() {
        return new AtomicDataType(new IntType());
    }

    /**
     * Data type of an 8-byte signed integer with values from -9,223,372,036,854,775,808 to
     * 9,223,372,036,854,775,807.
     *
     * @see BigIntType
     */
    public static DataType BIGINT() {
        return new AtomicDataType(new BigIntType());
    }

    /**
     * Data type of a 4-byte single precision floating point number.
     *
     * @see FloatType
     */
    public static DataType FLOAT() {
        return new AtomicDataType(new FloatType());
    }

    /**
     * Data type of an 8-byte double precision floating point number.
     *
     * @see DoubleType
     */
    public static DataType DOUBLE() {
        return new AtomicDataType(new DoubleType());
    }

    /**
     * Data type of a date consisting of {@code year-month-day} with values ranging from {@code
     * 0000-01-01} to {@code 9999-12-31}.
     *
     * <p>Compared to the SQL standard, the range starts at year {@code 0000}.
     *
     * @see DataType
     */
    public static DataType DATE() {
        return new AtomicDataType(new DateType());
    }

    /**
     * Data type of a time WITHOUT time zone {@code TIME(p)} where {@code p} is the number of digits
     * of fractional seconds (=precision). {@code p} must have a value between 0 and 9 (both
     * inclusive).
     *
     * <p>An instance consists of {@code hour:minute:second[.fractional]} with up to nanosecond
     * precision and values ranging from {@code 00:00:00.000000000} to {@code 23:59:59.999999999}.
     *
     * <p>Compared to the SQL standard, leap seconds (23:59:60 and 23:59:61) are not supported as
     * the semantics are closer to {@link java.time.LocalTime}. A time WITH time zone is not
     * provided.
     *
     * @see #TIME()
     * @see TimeType
     */
    public static DataType TIME(int precision) {
        return new AtomicDataType(new TimeType(precision));
    }

    /**
     * Data type of a time WITHOUT time zone {@code TIME} with no fractional seconds by default.
     *
     * <p>An instance consists of {@code hour:minute:second} with up to second precision and values
     * ranging from {@code 00:00:00} to {@code 23:59:59}.
     *
     * <p>Compared to the SQL standard, leap seconds (23:59:60 and 23:59:61) are not supported as
     * the semantics are closer to {@link java.time.LocalTime}. A time WITH time zone is not
     * provided.
     *
     * @see #TIME(int)
     * @see TimeType
     */
    public static DataType TIME() {
        return new AtomicDataType(new TimeType());
    }

    /**
     * Data type of a timestamp WITHOUT time zone {@code TIMESTAMP(p)} where {@code p} is the number
     * of digits of fractional seconds (=precision). {@code p} must have a value between 0 and 9
     * (both inclusive).
     *
     * <p>An instance consists of {@code year-month-day hour:minute:second[.fractional]} with up to
     * nanosecond precision and values ranging from {@code 0000-01-01 00:00:00.000000000} to {@code
     * 9999-12-31 23:59:59.999999999}.
     *
     * <p>Compared to the SQL standard, leap seconds (23:59:60 and 23:59:61) are not supported as
     * the semantics are closer to {@link java.time.LocalDateTime}.
     *
     * @see #TIMESTAMP_WITH_TIME_ZONE(int)
     * @see #TIMESTAMP_WITH_LOCAL_TIME_ZONE(int)
     * @see TimestampType
     */
    public static DataType TIMESTAMP(int precision) {
        return new AtomicDataType(new TimestampType(precision));
    }

    /**
     * Data type of a timestamp WITHOUT time zone {@code TIMESTAMP} with 6 digits of fractional
     * seconds by default.
     *
     * <p>An instance consists of {@code year-month-day hour:minute:second[.fractional]} with up to
     * microsecond precision and values ranging from {@code 0000-01-01 00:00:00.000000} to {@code
     * 9999-12-31 23:59:59.999999}.
     *
     * <p>Compared to the SQL standard, leap seconds (23:59:60 and 23:59:61) are not supported as
     * the semantics are closer to {@link java.time.LocalDateTime}.
     *
     * @see #TIMESTAMP(int)
     * @see #TIMESTAMP_WITH_TIME_ZONE(int)
     * @see #TIMESTAMP_WITH_LOCAL_TIME_ZONE(int)
     * @see TimestampType
     */
    public static DataType TIMESTAMP() {
        return new AtomicDataType(new TimestampType());
    }

    /**
     * Data type of a timestamp WITH time zone {@code TIMESTAMP(p) WITH TIME ZONE} where {@code p}
     * is the number of digits of fractional seconds (=precision). {@code p} must have a value
     * between 0 and 9 (both inclusive).
     *
     * <p>An instance consists of {@code year-month-day hour:minute:second[.fractional] zone} with
     * up to nanosecond precision and values ranging from {@code 0000-01-01 00:00:00.000000000
     * +14:59} to {@code 9999-12-31 23:59:59.999999999 -14:59}.
     *
     * <p>Compared to the SQL standard, leap seconds (23:59:60 and 23:59:61) are not supported as
     * the semantics are closer to {@link java.time.OffsetDateTime}.
     *
     * @see #TIMESTAMP(int)
     * @see #TIMESTAMP_WITH_LOCAL_TIME_ZONE(int)
     * @see ZonedTimestampType
     */
    public static DataType TIMESTAMP_WITH_TIME_ZONE(int precision) {
        return new AtomicDataType(new ZonedTimestampType(precision));
    }

    /**
     * Data type of a timestamp WITH time zone {@code TIMESTAMP WITH TIME ZONE} with 6 digits of
     * fractional seconds by default.
     *
     * <p>An instance consists of {@code year-month-day hour:minute:second[.fractional] zone} with
     * up to microsecond precision and values ranging from {@code 0000-01-01 00:00:00.000000 +14:59}
     * to {@code 9999-12-31 23:59:59.999999 -14:59}.
     *
     * <p>Compared to the SQL standard, leap seconds (23:59:60 and 23:59:61) are not supported as
     * the semantics are closer to {@link java.time.OffsetDateTime}.
     *
     * @see #TIMESTAMP_WITH_TIME_ZONE(int)
     * @see #TIMESTAMP(int)
     * @see #TIMESTAMP_WITH_LOCAL_TIME_ZONE(int)
     * @see ZonedTimestampType
     */
    public static DataType TIMESTAMP_WITH_TIME_ZONE() {
        return new AtomicDataType(new ZonedTimestampType());
    }

    /**
     * Data type of a timestamp WITH LOCAL time zone {@code TIMESTAMP(p) WITH LOCAL TIME ZONE} where
     * {@code p} is the number of digits of fractional seconds (=precision). {@code p} must have a
     * value between 0 and 9 (both inclusive).
     *
     * <p>An instance consists of {@code year-month-day hour:minute:second[.fractional] zone} with
     * up to nanosecond precision and values ranging from {@code 0000-01-01 00:00:00.000000000
     * +14:59} to {@code 9999-12-31 23:59:59.999999999 -14:59}. Leap seconds (23:59:60 and 23:59:61)
     * are not supported as the semantics are closer to {@link java.time.OffsetDateTime}.
     *
     * <p>Compared to {@link ZonedTimestampType}, the time zone offset information is not stored
     * physically in every datum. Instead, the type assumes {@link java.time.Instant} semantics in
     * UTC time zone at the edges of the table ecosystem. Every datum is interpreted in the local
     * time zone configured in the current session for computation and visualization.
     *
     * <p>This type fills the gap between time zone free and time zone mandatory timestamp types by
     * allowing the interpretation of UTC timestamps according to the configured session timezone.
     *
     * @see #TIMESTAMP(int)
     * @see #TIMESTAMP_WITH_TIME_ZONE(int)
     * @see LocalZonedTimestampType
     */
    public static DataType TIMESTAMP_WITH_LOCAL_TIME_ZONE(int precision) {
        return new AtomicDataType(new LocalZonedTimestampType(precision));
    }

    /**
     * Data type of a timestamp WITH LOCAL time zone. This is a synonym for {@link
     * DataTypes#TIMESTAMP_WITH_LOCAL_TIME_ZONE(int)}.
     */
    public static DataType TIMESTAMP_LTZ(int precision) {
        return new AtomicDataType(new LocalZonedTimestampType(precision));
    }

    /**
     * Data type of a timestamp WITH LOCAL time zone {@code TIMESTAMP WITH LOCAL TIME ZONE} with 6
     * digits of fractional seconds by default.
     *
     * <p>An instance consists of {@code year-month-day hour:minute:second[.fractional] zone} with
     * up to microsecond precision and values ranging from {@code 0000-01-01 00:00:00.000000 +14:59}
     * to {@code 9999-12-31 23:59:59.999999 -14:59}. Leap seconds (23:59:60 and 23:59:61) are not
     * supported as the semantics are closer to {@link java.time.OffsetDateTime}.
     *
     * <p>Compared to {@link ZonedTimestampType}, the time zone offset information is not stored
     * physically in every datum. Instead, the type assumes {@link java.time.Instant} semantics in
     * UTC time zone at the edges of the table ecosystem. Every datum is interpreted in the local
     * time zone configured in the current session for computation and visualization.
     *
     * <p>This type fills the gap between time zone free and time zone mandatory timestamp types by
     * allowing the interpretation of UTC timestamps according to the configured session timezone.
     *
     * @see #TIMESTAMP_WITH_LOCAL_TIME_ZONE(int)
     * @see #TIMESTAMP(int)
     * @see #TIMESTAMP_WITH_TIME_ZONE(int)
     * @see LocalZonedTimestampType
     */
    public static DataType TIMESTAMP_WITH_LOCAL_TIME_ZONE() {
        return new AtomicDataType(new LocalZonedTimestampType());
    }

    /**
     * Data type of a timestamp WITH LOCAL time zone. This is a synonym for {@link
     * DataTypes#TIMESTAMP_WITH_LOCAL_TIME_ZONE()}.
     */
    public static DataType TIMESTAMP_LTZ() {
        return new AtomicDataType(new LocalZonedTimestampType());
    }

    /**
     * Data type of a temporal interval. There are two types of temporal intervals: day-time
     * intervals with up to nanosecond granularity or year-month intervals with up to month
     * granularity.
     *
     * <p>An interval of day-time consists of {@code +days hours:months:seconds.fractional} with
     * values ranging from {@code -999999 23:59:59.999999999} to {@code +999999 23:59:59.999999999}.
     * The type must be parameterized to one of the following resolutions: interval of days,
     * interval of days to hours, interval of days to minutes, interval of days to seconds, interval
     * of hours, interval of hours to minutes, interval of hours to seconds, interval of minutes,
     * interval of minutes to seconds, or interval of seconds. The value representation is the same
     * for all types of resolutions. For example, an interval of seconds of 70 is always represented
     * in an interval-of-days-to-seconds format (with default precisions): {@code +00
     * 00:01:10.000000}).
     *
     * <p>An interval of year-month consists of {@code +years-months} with values ranging from
     * {@code -9999-11} to {@code +9999-11}. The type must be parameterized to one of the following
     * resolutions: interval of years, interval of years to months, or interval of months. The value
     * representation is the same for all types of resolutions. For example, an interval of months
     * of 50 is always represented in an interval-of-years-to-months format (with default year
     * precision): {@code +04-02}.
     *
     * <p>Examples: {@code INTERVAL(DAY(2))} for a day-time interval or {@code INTERVAL(YEAR(4))}
     * for a year-month interval.
     *
     * @see DayTimeIntervalType
     * @see YearMonthIntervalType
     */
    public static DataType INTERVAL(Resolution resolution) {
        Preconditions.checkNotNull(resolution, "Interval resolution must not be null.");
        return new AtomicDataType(Resolution.resolveInterval(resolution, null));
    }

    /**
     * Data type of a temporal interval. There are two types of temporal intervals: day-time
     * intervals with up to nanosecond granularity or year-month intervals with up to month
     * granularity.
     *
     * <p>An interval of day-time consists of {@code +days hours:months:seconds.fractional} with
     * values ranging from {@code -999999 23:59:59.999999999} to {@code +999999 23:59:59.999999999}.
     * The type must be parameterized to one of the following resolutions: interval of days,
     * interval of days to hours, interval of days to minutes, interval of days to seconds, interval
     * of hours, interval of hours to minutes, interval of hours to seconds, interval of minutes,
     * interval of minutes to seconds, or interval of seconds. The value representation is the same
     * for all types of resolutions. For example, an interval of seconds of 70 is always represented
     * in an interval-of-days-to-seconds format (with default precisions): {@code +00
     * 00:01:10.000000}.
     *
     * <p>An interval of year-month consists of {@code +years-months} with values ranging from
     * {@code -9999-11} to {@code +9999-11}. The type must be parameterized to one of the following
     * resolutions: interval of years, interval of years to months, or interval of months. The value
     * representation is the same for all types of resolutions. For example, an interval of months
     * of 50 is always represented in an interval-of-years-to-months format (with default year
     * precision): {@code +04-02}.
     *
     * <p>Examples: {@code INTERVAL(DAY(2), SECOND(9))} for a day-time interval or {@code
     * INTERVAL(YEAR(4), MONTH())} for a year-month interval.
     *
     * @see DayTimeIntervalType
     * @see YearMonthIntervalType
     */
    public static DataType INTERVAL(Resolution upperResolution, Resolution lowerResolution) {
        Preconditions.checkNotNull(upperResolution, "Upper interval resolution must not be null.");
        Preconditions.checkNotNull(lowerResolution, "Lower interval resolution must not be null.");
        return new AtomicDataType(Resolution.resolveInterval(upperResolution, lowerResolution));
    }

    /**
     * Data type of an array of elements with same subtype.
     *
     * <p>Compared to the SQL standard, the maximum cardinality of an array cannot be specified but
     * is fixed at {@link Integer#MAX_VALUE}. Also, any valid type is supported as a subtype.
     *
     * @see ArrayType
     */
    public static DataType ARRAY(DataType elementDataType) {
        Preconditions.checkNotNull(elementDataType, "Element data type must not be null.");
        return new CollectionDataType(
                new ArrayType(elementDataType.getLogicalType()), elementDataType);
    }

    /**
     * Data type of an associative array that maps keys (including {@code NULL}) to values
     * (including {@code NULL}). A map cannot contain duplicate keys; each key can map to at most
     * one value.
     *
     * <p>There is no restriction of key types; it is the responsibility of the user to ensure
     * uniqueness. The map type is an extension to the SQL standard.
     *
     * @see MapType
     */
    public static DataType MAP(DataType keyDataType, DataType valueDataType) {
        Preconditions.checkNotNull(keyDataType, "Key data type must not be null.");
        Preconditions.checkNotNull(valueDataType, "Value data type must not be null.");
        return new KeyValueDataType(
                new MapType(keyDataType.getLogicalType(), valueDataType.getLogicalType()),
                keyDataType,
                valueDataType);
    }

    /**
     * Unresolved data type of an associative array that maps keys (including {@code NULL}) to
     * values (including {@code NULL}). A map cannot contain duplicate keys; each key can map to at
     * most one value.
     *
     * <p>There is no restriction of key types; it is the responsibility of the user to ensure
     * uniqueness. The map type is an extension to the SQL standard.
     *
     * <p>Note: Compared to {@link #MAP(DataType, DataType)}, this method produces an {@link
     * UnresolvedDataType}. In most of the cases, the {@link UnresolvedDataType} will be
     * automatically resolved by the API. At other locations, a {@link DataTypeFactory} is provided.
     *
     * @see MapType
     */
    public static UnresolvedDataType MAP(
            AbstractDataType<?> keyDataType, AbstractDataType<?> valueDataType) {
        Preconditions.checkNotNull(keyDataType, "Key data type must not be null.");
        Preconditions.checkNotNull(valueDataType, "Value data type must not be null.");
        return new UnresolvedDataType(
                () -> String.format(MapType.FORMAT, keyDataType, valueDataType),
                factory ->
                        MAP(
                                factory.createDataType(keyDataType),
                                factory.createDataType(valueDataType)));
    }

    /**
     * Data type for representing untyped {@code NULL} values. A null type has no other value except
     * {@code NULL}, thus, it can be cast to any nullable type similar to JVM semantics.
     *
     * <p>This type helps in representing unknown types in API calls that use a {@code NULL} literal
     * as well as bridging to formats such as JSON or Avro that define such a type as well.
     *
     * <p>The null type is an extension to the SQL standard.
     *
     * <p>Note: The runtime does not support this type. It is a pure helper type during translation
     * and planning. Table columns cannot be declared with this type. Functions cannot declare
     * return types of this type.
     *
     * @see NullType
     */
    public static DataType NULL() {
        return new AtomicDataType(new NullType());
    }

    // --------------------------------------------------------------------------------------------
    // Helper functions
    // --------------------------------------------------------------------------------------------

    /**
     * Resolution in seconds with 6 digits for fractional seconds by default.
     *
     * @see #SECOND(int)
     */
    public static Resolution SECOND() {
        return new Resolution(
                Resolution.IntervalUnit.SECOND, DayTimeIntervalType.DEFAULT_FRACTIONAL_PRECISION);
    }

    /**
     * Resolution in seconds and (possibly) fractional seconds. The precision is the number of
     * digits of fractional seconds. It must have a value between 0 and 9 (both inclusive). If no
     * fractional is specified, it is equal to 6 by default.
     *
     * @see #SECOND()
     */
    public static Resolution SECOND(int precision) {
        return new Resolution(Resolution.IntervalUnit.SECOND, precision);
    }

    /** Resolution in minutes. */
    public static Resolution MINUTE() {
        return new Resolution(Resolution.IntervalUnit.MINUTE);
    }

    /** Resolution in hours. */
    public static Resolution HOUR() {
        return new Resolution(Resolution.IntervalUnit.HOUR);
    }

    /**
     * Resolution in days. The precision is the number of digits of days. It must have a value
     * between 1 and 6 (both inclusive). If no precision is specified, it is equal to 2 by default.
     *
     * @see #DAY()
     */
    public static Resolution DAY(int precision) {
        return new Resolution(Resolution.IntervalUnit.DAY, precision);
    }

    /**
     * Resolution in days with 2 digits for the number of days by default.
     *
     * @see #DAY(int)
     */
    public static Resolution DAY() {
        return new Resolution(
                Resolution.IntervalUnit.DAY, DayTimeIntervalType.DEFAULT_DAY_PRECISION);
    }

    /** Resolution in months. */
    public static Resolution MONTH() {
        return new Resolution(Resolution.IntervalUnit.MONTH);
    }

    /**
     * Resolution in years. The precision is the number of digits of years. It must have a value
     * between 1 and 4 (both inclusive). If no precision is specified, it is equal to 2.
     *
     * @see #YEAR()
     */
    public static Resolution YEAR(int precision) {
        return new Resolution(Resolution.IntervalUnit.YEAR, precision);
    }

    /**
     * Resolution in years with 2 digits for the number of years by default.
     *
     * @see #YEAR(int)
     */
    public static Resolution YEAR() {
        return new Resolution(
                Resolution.IntervalUnit.YEAR, YearMonthIntervalType.DEFAULT_PRECISION);
    }

    // --------------------------------------------------------------------------------------------
    // Helper classes
    // --------------------------------------------------------------------------------------------

    /**
     * Helper class for defining the resolution of an interval.
     *
     * @see #INTERVAL(Resolution)
     */
    public static final class Resolution {

        private static final int EMPTY_PRECISION = -1;

        private enum IntervalUnit {
            SECOND,
            MINUTE,
            HOUR,
            DAY,
            MONTH,
            YEAR
        }

        private static final Map<List<IntervalUnit>, BiFunction<Integer, Integer, LogicalType>>
                resolutionMapping = new HashMap<>();

        static {
            addResolutionMapping(
                    IntervalUnit.YEAR,
                    null,
                    (p1, p2) -> new YearMonthIntervalType(YearMonthResolution.YEAR, p1));
            addResolutionMapping(
                    IntervalUnit.MONTH,
                    null,
                    (p1, p2) -> new YearMonthIntervalType(YearMonthResolution.MONTH));
            addResolutionMapping(
                    IntervalUnit.YEAR,
                    IntervalUnit.MONTH,
                    (p1, p2) -> new YearMonthIntervalType(YearMonthResolution.YEAR_TO_MONTH, p1));
            addResolutionMapping(
                    IntervalUnit.DAY,
                    null,
                    (p1, p2) ->
                            new DayTimeIntervalType(
                                    DayTimeResolution.DAY,
                                    p1,
                                    DayTimeIntervalType.DEFAULT_FRACTIONAL_PRECISION));
            addResolutionMapping(
                    IntervalUnit.DAY,
                    IntervalUnit.HOUR,
                    (p1, p2) ->
                            new DayTimeIntervalType(
                                    DayTimeResolution.DAY_TO_HOUR,
                                    p1,
                                    DayTimeIntervalType.DEFAULT_FRACTIONAL_PRECISION));
            addResolutionMapping(
                    IntervalUnit.DAY,
                    IntervalUnit.MINUTE,
                    (p1, p2) ->
                            new DayTimeIntervalType(
                                    DayTimeResolution.DAY_TO_MINUTE,
                                    p1,
                                    DayTimeIntervalType.DEFAULT_FRACTIONAL_PRECISION));
            addResolutionMapping(
                    IntervalUnit.DAY,
                    IntervalUnit.SECOND,
                    (p1, p2) -> new DayTimeIntervalType(DayTimeResolution.DAY_TO_SECOND, p1, p2));
            addResolutionMapping(
                    IntervalUnit.HOUR,
                    null,
                    (p1, p2) -> new DayTimeIntervalType(DayTimeResolution.HOUR));
            addResolutionMapping(
                    IntervalUnit.HOUR,
                    IntervalUnit.MINUTE,
                    (p1, p2) -> new DayTimeIntervalType(DayTimeResolution.HOUR_TO_MINUTE));
            addResolutionMapping(
                    IntervalUnit.HOUR,
                    IntervalUnit.SECOND,
                    (p1, p2) ->
                            new DayTimeIntervalType(
                                    DayTimeResolution.HOUR_TO_SECOND,
                                    DayTimeIntervalType.DEFAULT_DAY_PRECISION,
                                    p2));
            addResolutionMapping(
                    IntervalUnit.MINUTE,
                    null,
                    (p1, p2) -> new DayTimeIntervalType(DayTimeResolution.MINUTE));
            addResolutionMapping(
                    IntervalUnit.MINUTE,
                    IntervalUnit.SECOND,
                    (p1, p2) ->
                            new DayTimeIntervalType(
                                    DayTimeResolution.MINUTE_TO_SECOND,
                                    DayTimeIntervalType.DEFAULT_DAY_PRECISION,
                                    p2));
            addResolutionMapping(
                    IntervalUnit.SECOND,
                    null,
                    (p1, p2) ->
                            new DayTimeIntervalType(
                                    DayTimeResolution.SECOND,
                                    DayTimeIntervalType.DEFAULT_DAY_PRECISION,
                                    p1));
        }

        private static void addResolutionMapping(
                IntervalUnit leftUnit,
                IntervalUnit rightUnit,
                BiFunction<Integer, Integer, LogicalType> typeProvider) {
            resolutionMapping.put(Arrays.asList(leftUnit, rightUnit), typeProvider);
        }

        private static LogicalType resolveInterval(
                Resolution fromResolution, @Nullable Resolution toResolution) {
            final IntervalUnit toBoundary = toResolution == null ? null : toResolution.unit;
            final int toPrecision = toResolution == null ? EMPTY_PRECISION : toResolution.precision;

            final BiFunction<Integer, Integer, LogicalType> typeProvider =
                    resolutionMapping.get(Arrays.asList(fromResolution.unit, toBoundary));
            if (typeProvider == null) {
                throw new ValidationException(
                        String.format(
                                "Unsupported interval definition '%s TO %s'. "
                                        + "Please check the documentation for supported combinations for year-month and day-time intervals.",
                                fromResolution.unit, toBoundary));
            }
            return typeProvider.apply(fromResolution.precision, toPrecision);
        }

        private final int precision;

        private final IntervalUnit unit;

        private Resolution(IntervalUnit unit, int precision) {
            this.unit = unit;
            this.precision = precision;
        }

        private Resolution(IntervalUnit unit) {
            this(unit, EMPTY_PRECISION);
        }

        @Override
        public String toString() {
            if (precision != EMPTY_PRECISION) {
                return String.format("%s(%d)", unit, precision);
            }
            return unit.toString();
        }
    }

    /**
     * Common helper class for resolved and unresolved fields of a row or structured type.
     *
     * @see #FIELD(String, DataType)
     * @see #FIELD(String, DataType, String)
     * @see #FIELD(String, AbstractDataType)
     * @see #FIELD(String, AbstractDataType, String)
     */
    public abstract static class AbstractField {

        protected final String name;

        protected final @Nullable String description;

        private AbstractField(String name, @Nullable String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        protected abstract AbstractDataType<?> getAbstractDataType();

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AbstractField that = (AbstractField) o;
            return name.equals(that.name) && Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, description);
        }

        @Override
        public String toString() {
            if (description != null) {
                return String.format("%s %s '%s'", name, getAbstractDataType(), description);
            }
            return String.format("%s %s", name, getAbstractDataType());
        }
    }

    private DataTypes() {
        // no instances
    }

    // CHECKSTYLE.ON: MethodName
}
