package com.hhoa.kline.plugins.jdbc.converter;

import com.hhoa.ai.kline.commons.utils.DateUtil;
import com.hhoa.kline.plugins.jdbc.converter.logical.BitType;
import com.hhoa.kline.plugins.jdbc.converter.logical.BoolType;
import com.hhoa.kline.plugins.jdbc.converter.logical.BpcharType;
import com.hhoa.kline.plugins.jdbc.converter.logical.ByteaType;
import com.hhoa.kline.plugins.jdbc.converter.logical.CharType;
import com.hhoa.kline.plugins.jdbc.converter.logical.DateType;
import com.hhoa.kline.plugins.jdbc.converter.logical.Float4Type;
import com.hhoa.kline.plugins.jdbc.converter.logical.Float8Type;
import com.hhoa.kline.plugins.jdbc.converter.logical.InetType;
import com.hhoa.kline.plugins.jdbc.converter.logical.Int2Type;
import com.hhoa.kline.plugins.jdbc.converter.logical.Int4Type;
import com.hhoa.kline.plugins.jdbc.converter.logical.Int8Type;
import com.hhoa.kline.plugins.jdbc.converter.logical.JsonType;
import com.hhoa.kline.plugins.jdbc.converter.logical.JsonbType;
import com.hhoa.kline.plugins.jdbc.converter.logical.MoneyType;
import com.hhoa.kline.plugins.jdbc.converter.logical.NameType;
import com.hhoa.kline.plugins.jdbc.converter.logical.NumericType;
import com.hhoa.kline.plugins.jdbc.converter.logical.OidType;
import com.hhoa.kline.plugins.jdbc.converter.logical.PgCustomType;
import com.hhoa.kline.plugins.jdbc.converter.logical.PgTimestampType;
import com.hhoa.kline.plugins.jdbc.converter.logical.PointType;
import com.hhoa.kline.plugins.jdbc.converter.logical.TextType;
import com.hhoa.kline.plugins.jdbc.converter.logical.TimeType;
import com.hhoa.kline.plugins.jdbc.converter.logical.TimestampTzType;
import com.hhoa.kline.plugins.jdbc.converter.logical.TimetzType;
import com.hhoa.kline.plugins.jdbc.converter.logical.UuidType;
import com.hhoa.kline.plugins.jdbc.converter.logical.VarbitType;
import com.hhoa.kline.plugins.jdbc.converter.logical.VarcharType;
import com.hhoa.kline.plugins.jdbc.converter.logical.XmlType;
import com.hhoa.kline.plugins.jdbc.types.AbstractBaseColumn;
import com.hhoa.kline.plugins.jdbc.types.AtomicDataType;
import com.hhoa.kline.plugins.jdbc.types.DataColumnFactory;
import com.hhoa.kline.plugins.jdbc.types.DataType;
import com.hhoa.kline.plugins.jdbc.types.DataTypeConverter;
import com.hhoa.kline.plugins.jdbc.types.DataTypeFactory;
import com.hhoa.kline.plugins.jdbc.types.DataTypeFactoryImpl;
import com.hhoa.kline.plugins.jdbc.types.DataTypes;
import com.hhoa.kline.plugins.jdbc.types.TypeConfig;
import com.hhoa.kline.plugins.jdbc.types.column.ArrayColumn;
import com.hhoa.kline.plugins.jdbc.types.column.BigDecimalColumn;
import com.hhoa.kline.plugins.jdbc.types.column.BooleanColumn;
import com.hhoa.kline.plugins.jdbc.types.column.BytesColumn;
import com.hhoa.kline.plugins.jdbc.types.column.DoubleColumn;
import com.hhoa.kline.plugins.jdbc.types.column.FloatColumn;
import com.hhoa.kline.plugins.jdbc.types.column.IntColumn;
import com.hhoa.kline.plugins.jdbc.types.column.LongColumn;
import com.hhoa.kline.plugins.jdbc.types.column.SqlDateColumn;
import com.hhoa.kline.plugins.jdbc.types.column.StringColumn;
import com.hhoa.kline.plugins.jdbc.types.column.TimeColumn;
import com.hhoa.kline.plugins.jdbc.types.column.TimestampColumn;
import com.hhoa.kline.plugins.jdbc.types.column.YearMonthColumn;
import com.hhoa.kline.plugins.jdbc.types.exception.UnsupportedTypeException;
import com.hhoa.kline.plugins.jdbc.types.logical.ArrayType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;
import com.hhoa.kline.plugins.jdbc.types.logical.TimestampType;
import com.hhoa.kline.plugins.jdbc.types.logical.YearMonthIntervalType;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.Oid;
import org.postgresql.jdbc.PgArray;
import org.postgresql.jdbc.PgSQLXML;
import org.postgresql.util.PGobject;

public class PostgresqlDataConverter {
    public static final DataTypeFactory dataTypeFactory = new DataTypeFactoryImpl();

    /**
     * inspired by Postgresql doc. <a
     * href="https://www.postgresql.org/docs/current/datatype.html">...</a>
     */
    public static DataType getDataType(TypeConfig type) {
        return switch (type.getType()) {
            case "ARRAY" -> {
                // 处理数组类型（ARRAY<...>）
                if (!type.isCollectionType()) {
                    throw new UnsupportedTypeException(
                            "ARRAY type must be collection type: " + type);
                }
                TypeConfig elementType = type.getElementType(0);
                if (elementType == null) {
                    throw new UnsupportedTypeException(
                            "ARRAY type must have element type: " + type);
                }
                // 递归处理元素类型
                DataType elementDataType = getDataType(elementType);
                yield DataTypes.ARRAY(elementDataType);
            }
            case "BIT" -> new AtomicDataType(new BitType(true, LogicalTypeRoot.BOOLEAN, false));
            case "BOOLEAN", "BOOL" -> DataTypes.BOOLEAN();
            case "SMALLINT", "SMALLSERIAL", "INT2", "INT", "INTEGER", "SERIAL", "INT4" ->
                    DataTypes.INT();
            case "BIGINT", "BIGSERIAL", "OID", "INT8" -> DataTypes.BIGINT();
            case "REAL", "FLOAT4" -> DataTypes.FLOAT();
            case "FLOAT", "DOUBLE PRECISION", "FLOAT8" -> DataTypes.DOUBLE();
            case "MONEY" -> new AtomicDataType(new MoneyType(true, LogicalTypeRoot.DOUBLE, false));
            case "DECIMAL", "NUMERIC" -> type.toDecimalDataType();
            case "CHARACTER VARYING",
                    "CHARACTER VARYIN",
                    "VARCHAR",
                    "CHARACTER",
                    "CHAR",
                    "TEXT",
                    "NAME",
                    "BPCHAR" ->
                    DataTypes.STRING();
            // Binary Data Types
            case "BYTEA" -> DataTypes.BYTES();
            case "VARBIT" ->
                    new AtomicDataType(new VarbitType(true, LogicalTypeRoot.VARCHAR, false));
            case "XML" -> new AtomicDataType(new XmlType(true, LogicalTypeRoot.VARCHAR, false));
            case "UUID" -> new AtomicDataType(new UuidType(true, LogicalTypeRoot.VARCHAR, false));
            case "POINT" -> new AtomicDataType(new PointType(true, LogicalTypeRoot.VARCHAR, false));
            case "INET" -> new AtomicDataType(new InetType(true, LogicalTypeRoot.VARCHAR, false));
            // Date/Time Types
            case "ABSTIME", "TIMESTAMP", "TIMESTAMPTZ" -> type.toTimestampDataType(6);
            case "TIMESTAMP WITHOUT TIME ZONE" -> type.toZonedTimestampDataType();
            case "TIMESTAMP WITH TIME ZONE" -> type.toTimestampDataType(6);
            case "DATE" -> DataTypes.DATE();
            case "TIME", "TIMETZ" -> type.toTimeDataType(6);
            case "JSON" -> new AtomicDataType(new JsonType(true, LogicalTypeRoot.VARCHAR, false));
            case "JSONB" -> new AtomicDataType(new JsonbType(true, LogicalTypeRoot.VARCHAR, false));
            case "_BIT" -> new AtomicDataType(new BitType(true, LogicalTypeRoot.VARCHAR, true));
            case "_BOOL" -> new AtomicDataType(new BoolType(true, LogicalTypeRoot.VARCHAR, true));
            case "_INT2" -> new AtomicDataType(new Int2Type(true, LogicalTypeRoot.VARCHAR, true));
            case "_INT4" -> new AtomicDataType(new Int4Type(true, LogicalTypeRoot.VARCHAR, true));
            case "_INT8" -> new AtomicDataType(new Int8Type(true, LogicalTypeRoot.VARCHAR, true));
            case "_FLOAT4" ->
                    new AtomicDataType(new Float4Type(true, LogicalTypeRoot.VARCHAR, true));
            case "_FLOAT8" ->
                    new AtomicDataType(new Float8Type(true, LogicalTypeRoot.VARCHAR, true));
            case "_NUMERIC" ->
                    new AtomicDataType(new NumericType(true, LogicalTypeRoot.VARCHAR, true));
            case "_TIME" -> new AtomicDataType(new TimeType(true, LogicalTypeRoot.VARCHAR, true));
            case "_TIMETZ" ->
                    new AtomicDataType(new TimetzType(true, LogicalTypeRoot.VARCHAR, true));
            case "_TIMESTAMP" ->
                    new AtomicDataType(new PgTimestampType(true, LogicalTypeRoot.VARCHAR, true));
            case "_TIMESTAMPTZ" ->
                    new AtomicDataType(new TimestampTzType(true, LogicalTypeRoot.VARCHAR, true));
            case "_DATE" -> new AtomicDataType(new DateType(true, LogicalTypeRoot.VARCHAR, true));
            case "_BYTEA" -> new AtomicDataType(new ByteaType(true, LogicalTypeRoot.VARCHAR, true));
            case "_VARCHAR" ->
                    new AtomicDataType(new VarcharType(true, LogicalTypeRoot.VARCHAR, true));
            case "_OID" -> new AtomicDataType(new OidType(true, LogicalTypeRoot.VARCHAR, true));
            case "_BPCHAR" ->
                    new AtomicDataType(new BpcharType(true, LogicalTypeRoot.VARCHAR, true));
            case "_TEXT" -> new AtomicDataType(new TextType(true, LogicalTypeRoot.VARCHAR, true));
            case "_MONEY" -> new AtomicDataType(new MoneyType(true, LogicalTypeRoot.VARCHAR, true));
            // case "_INTERVAL":
            case "_CHAR" -> new AtomicDataType(new CharType(true, LogicalTypeRoot.VARCHAR, true));
            case "_VARBIT" ->
                    new AtomicDataType(new VarbitType(true, LogicalTypeRoot.VARCHAR, true));
            case "_NAME" -> new AtomicDataType(new NameType(true, LogicalTypeRoot.VARCHAR, true));
            case "_UUID" -> new AtomicDataType(new UuidType(true, LogicalTypeRoot.VARCHAR, true));
            case "_XML" -> new AtomicDataType(new XmlType(true, LogicalTypeRoot.VARCHAR, true));
            case "_INET" -> new AtomicDataType(new InetType(true, LogicalTypeRoot.VARCHAR, true));
            default -> throw new UnsupportedTypeException(type);
        };
    }

    public static DataColumnFactory createInternalConverter(LogicalType type) {
        return switch (type.getTypeRoot()) {
            case BOOLEAN -> val -> new BooleanColumn((Boolean) val);
            case INTEGER -> val -> new IntColumn((Integer) val);
            case INTERVAL_YEAR_MONTH ->
                    val -> {
                        YearMonthIntervalType yearMonthIntervalType = (YearMonthIntervalType) type;
                        if (yearMonthIntervalType.getResolution()
                                == YearMonthIntervalType.YearMonthResolution.YEAR) {
                            return new YearMonthColumn(
                                    Integer.parseInt(String.valueOf(val).substring(0, 4)));
                        } else {
                            throw new UnsupportedOperationException(
                                    "jdbc converter only support YEAR_MONTH");
                        }
                    };
            case FLOAT -> val -> new FloatColumn((Float) val);
            case DOUBLE -> val -> new DoubleColumn((Double) val);
            case BIGINT -> val -> new LongColumn((Long) val);
            case DECIMAL -> val -> new BigDecimalColumn((BigDecimal) val);
            case CHAR, VARCHAR -> {
                if (type instanceof PgCustomType && ((PgCustomType) type).isArray()) {
                    yield val -> new StringColumn(val.toString());
                } else if (type instanceof JsonType
                        || type instanceof JsonbType
                        || type instanceof VarbitType
                        || type instanceof PointType
                        || type instanceof InetType) {
                    yield val -> new StringColumn(((PGobject) val).getValue());
                } else if (type instanceof UuidType) {
                    yield val -> new StringColumn(((UUID) val).toString());
                } else if (type instanceof XmlType) {
                    yield val -> new StringColumn(((PgSQLXML) val).getString());
                }
                yield val -> new StringColumn(val.toString());
            }
            case DATE -> val -> new SqlDateColumn((Date) val);
            case TIME_WITHOUT_TIME_ZONE -> val -> new TimeColumn((Time) val);
            case TIMESTAMP_WITH_TIME_ZONE, TIMESTAMP_WITHOUT_TIME_ZONE ->
                    val -> {
                        if (val instanceof PGobject) {
                            return new TimestampColumn(
                                    DateUtil.convertToTimestampWithZone(val.toString()), 0);
                        }
                        // 处理 LocalDateTime 类型
                        if (val instanceof LocalDateTime) {
                            return new TimestampColumn(
                                    Timestamp.valueOf((LocalDateTime) val),
                                    ((TimestampType) (type)).getPrecision());
                        }
                        // 处理 Timestamp 类型
                        return new TimestampColumn(
                                (Timestamp) val, ((TimestampType) (type)).getPrecision());
                    };
            case BINARY, VARBINARY -> val -> new BytesColumn((byte[]) val);
            case ARRAY -> {
                if (type instanceof ArrayType arrayType) {
                    LogicalType elementType = arrayType.getElementType();
                    yield val -> new ArrayColumn(val, elementType);
                }
                throw new UnsupportedOperationException(
                        "ARRAY type must be instance of ArrayType: " + type);
            }
            default -> throw new UnsupportedOperationException("Unsupported type:" + type);
        };
    }

    /**
     * 根据元素类型获取 PostgreSQL 数组 OID
     *
     * @param elementType 数组元素类型
     * @return PostgreSQL 数组 OID
     */
    private static int getArrayOid(LogicalType elementType) {
        LogicalTypeRoot elementRoot = elementType.getTypeRoot();

        return switch (elementRoot) {
            case INTEGER -> Oid.INT4_ARRAY;
            case BIGINT -> Oid.INT8_ARRAY;
            case SMALLINT -> Oid.INT2_ARRAY;
            case FLOAT -> Oid.FLOAT4_ARRAY;
            case DOUBLE -> Oid.FLOAT8_ARRAY;
            case DECIMAL -> Oid.NUMERIC_ARRAY;
            case CHAR, VARCHAR -> {
                // 检查是否是特定的文本类型
                if (elementType instanceof TextType) {
                    yield Oid.TEXT_ARRAY;
                } else if (elementType instanceof VarcharType) {
                    yield Oid.VARCHAR_ARRAY;
                } else if (elementType instanceof CharType) {
                    yield Oid.CHAR_ARRAY;
                } else if (elementType instanceof BpcharType) {
                    yield Oid.BPCHAR_ARRAY;
                } else if (elementType instanceof PgCustomType) {
                    // 对于 PgCustomType（如 InetType），使用其自己的数组 OID
                    yield ((PgCustomType) elementType).getArrayOid();
                } else {
                    // 默认使用 TEXT_ARRAY
                    yield Oid.TEXT_ARRAY;
                }
            }
            case BOOLEAN -> Oid.BOOL_ARRAY;
            case DATE -> Oid.DATE_ARRAY;
            case TIME_WITHOUT_TIME_ZONE -> Oid.TIME_ARRAY;
            case TIMESTAMP_WITHOUT_TIME_ZONE -> Oid.TIMESTAMP_ARRAY;
            case TIMESTAMP_WITH_TIME_ZONE -> Oid.TIMESTAMPTZ_ARRAY;
            default -> {
                // 对于未知类型，尝试使用 TEXT_ARRAY 作为默认值
                yield Oid.TEXT_ARRAY;
            }
        };
    }

    public static DataTypeConverter createExternalConverter(
            LogicalType type, BaseConnection connection) {
        switch (type.getTypeRoot()) {
            case ARRAY:
                if (type instanceof ArrayType arrayType) {
                    LogicalType elementType = arrayType.getElementType();

                    DataTypeConverter elementConverter =
                            createExternalConverter(elementType, connection);
                    if (connection != null) {
                        int arrayOid = getArrayOid(elementType);
                        return val -> new PgArray(connection, arrayOid, val.asString());
                    } else {
                        if (elementType instanceof InetType) {
                            // 对于 inet[] 类型，返回 PGobject 包装的数组字符串
                            // 格式：{192.168.1.1,192.168.1.2}
                            return val -> {
                                Object arrayData = val.getData();
                                if (arrayData == null) {
                                    return null;
                                }
                                String[] stringArray;
                                if (arrayData.getClass().isArray()) {
                                    stringArray = (String[]) arrayData;
                                } else if (arrayData instanceof List<?> list) {
                                    if (list.isEmpty()) {
                                        return null;
                                    }
                                    stringArray = list.toArray(new String[0]);
                                } else {
                                    return arrayData;
                                }
                                // 转换为 PostgreSQL 数组格式字符串并包装为 PGobject
                                String arrayString;
                                if (stringArray.length == 0) {
                                    arrayString = "{}";
                                } else {
                                    arrayString = "{" + String.join(",", stringArray) + "}";
                                }
                                PGobject pgObject = new PGobject();
                                pgObject.setType("inet[]");
                                pgObject.setValue(arrayString);
                                return pgObject;
                            };
                        }
                        return val -> {
                            Object arrayData = val.getData();
                            if (arrayData == null) {
                                return null;
                            }
                            Object[] objects;
                            // 如果已经是数组，直接返回
                            if (arrayData.getClass().isArray()) {
                                objects = (Object[]) arrayData;
                            } else if (arrayData instanceof List<?>) {
                                List<Object> list = (List<Object>) arrayData;
                                objects = list.toArray();
                            } else {
                                objects = new Object[] {arrayData};
                            }

                            if (objects.length > 0
                                    && !elementType
                                            .getDefaultConversion()
                                            .equals(objects[0].getClass())) {
                                DataType dataType =
                                        dataTypeFactory.createDataType(objects[0].getClass());
                                DataColumnFactory elementDataColumnFactory =
                                        createInternalConverter(dataType.getLogicalType());

                                Object[] data =
                                        Arrays.stream(objects)
                                                .map(
                                                        e -> {
                                                            try {
                                                                return elementConverter.convert(
                                                                        elementDataColumnFactory
                                                                                .get(e));
                                                            } catch (Exception ex) {
                                                                throw new RuntimeException(ex);
                                                            }
                                                        })
                                                .toArray();
                                return new ArrayColumn(data, data.length, elementType)
                                        .asArray(elementType);
                            } else {
                                return new ArrayColumn(objects, objects.length, elementType)
                                        .asArray(elementType);
                            }
                        };
                    }
                }
                throw new UnsupportedOperationException(
                        "ARRAY type must be instance of ArrayType: " + type);
            case BOOLEAN:
                if (type instanceof BitType) {
                    return val -> {
                        PGobject pgObject = new PGobject();
                        pgObject.setType("bit");
                        pgObject.setValue(val.asBoolean() ? "1" : "0");
                        return pgObject;
                    };
                }
                return AbstractBaseColumn::asBoolean;
            case INTEGER:
                return AbstractBaseColumn::asInt;
            case FLOAT:
                return AbstractBaseColumn::asFloat;
            case DOUBLE:
                if (type instanceof MoneyType) {
                    return val -> {
                        PGobject pGobject = new PGobject();
                        pGobject.setType("money");
                        pGobject.setValue(val.asString());
                        return pGobject;
                    };
                }
                return AbstractBaseColumn::asDouble;
            case BIGINT:
                return AbstractBaseColumn::asLong;
            case DECIMAL:
                return AbstractBaseColumn::asBigDecimal;
            case CHAR:
            case VARCHAR:
                switch (type) {
                    case PgCustomType pgCustomType when pgCustomType.isArray() -> {
                        final int oid = pgCustomType.getArrayOid();
                        return val -> new PgArray(connection, oid, (String) val.asString());
                    }
                    case JsonType jsonType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("json");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    case JsonbType jsonbType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("jsonb");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    case VarbitType varbitType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("varbit");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    case XmlType xmlType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("xml");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    case UuidType uuidType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("uuid");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    case PointType pointType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("point");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    case InetType inetType -> {
                        return val -> {
                            PGobject jsonObject = new PGobject();
                            jsonObject.setType("inet");
                            jsonObject.setValue(val.asString());
                            return jsonObject;
                        };
                    }
                    default -> {}
                }
                return AbstractBaseColumn::asString;
            case DATE:
                return AbstractBaseColumn::asSqlDate;
            case TIME_WITHOUT_TIME_ZONE:
                return AbstractBaseColumn::asTime;
            case TIMESTAMP_WITH_TIME_ZONE:
            case TIMESTAMP_WITHOUT_TIME_ZONE:
                return AbstractBaseColumn::asTimestamp;
            case BINARY:
            case VARBINARY:
                return AbstractBaseColumn::asBytes;
            default:
                throw new UnsupportedOperationException("Unsupported type:" + type);
        }
    }
}
