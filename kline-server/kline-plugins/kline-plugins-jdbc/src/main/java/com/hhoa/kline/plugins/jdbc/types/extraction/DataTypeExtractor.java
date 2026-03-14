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

package com.hhoa.kline.plugins.jdbc.types.extraction;

import static com.hhoa.kline.plugins.jdbc.types.extraction.ExtractionUtils.*;

import com.hhoa.kline.plugins.jdbc.types.DataType;
import com.hhoa.kline.plugins.jdbc.types.DataTypeFactory;
import com.hhoa.kline.plugins.jdbc.types.DataTypes;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.utils.ClassDataTypeConverter;
import jakarta.annotation.Nullable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reflection-based utility that analyzes a given {@link Type}, method, or class to extract a
 * (possibly nested) {@link DataType} from it.
 */
public final class DataTypeExtractor {

    private final DataTypeFactory typeFactory;

    private final String contextExplanation;

    private DataTypeExtractor(DataTypeFactory typeFactory, String contextExplanation) {
        this.typeFactory = typeFactory;
        this.contextExplanation = contextExplanation;
    }

    /** Extracts a data type from a type without considering surrounding classes or templates. */
    public static DataType extractFromType(DataTypeFactory typeFactory, Type type) {
        return extractDataTypeWithClassContext(
                typeFactory, DataTypeTemplate.fromDefaults(), null, type, "");
    }

    /** Extracts a data type from a type without considering surrounding classes but templates. */
    public static DataType extractFromType(
            DataTypeFactory typeFactory, DataTypeTemplate template, Type type) {
        return extractDataTypeWithClassContext(typeFactory, template, null, type, "");
    }

    /**
     * Extracts a data type from a type variable at {@code genericPos} of {@code baseClass} using
     * the information of the most specific type {@code contextType}.
     */
    public static DataType extractFromGeneric(
            DataTypeFactory typeFactory, Class<?> baseClass, int genericPos, Type contextType) {
        final TypeVariable<?> variable = baseClass.getTypeParameters()[genericPos];
        return extractDataTypeWithClassContext(
                typeFactory,
                DataTypeTemplate.fromDefaults(),
                contextType,
                variable,
                String.format(
                        " in generic class '%s' in %s",
                        baseClass.getName(), contextType.toString()));
    }

    private static DataType extractDataTypeWithClassContext(
            DataTypeFactory typeFactory,
            DataTypeTemplate outerTemplate,
            @Nullable Type contextType,
            Type type,
            String contextExplanation) {
        final DataTypeExtractor extractor = new DataTypeExtractor(typeFactory, contextExplanation);
        final List<Type> typeHierarchy;
        if (contextType != null) {
            typeHierarchy = collectTypeHierarchy(contextType);
        } else {
            typeHierarchy = Collections.emptyList();
        }
        return extractor.extractDataTypeOrRaw(outerTemplate, typeHierarchy, type);
    }

    // --------------------------------------------------------------------------------------------

    private DataType extractDataTypeOrRaw(
            DataTypeTemplate outerTemplate, List<Type> typeHierarchy, Type type) {
        // best effort resolution of type variables, the resolved type can still be a
        // variable
        final Type resolvedType;
        if (type instanceof TypeVariable) {
            resolvedType = resolveVariable(typeHierarchy, (TypeVariable<?>) type);
        } else {
            resolvedType = type;
        }
        // merge outer template with template of type itself
        DataTypeTemplate template = outerTemplate;
        final Class<?> clazz = toClass(resolvedType);
        // main work
        DataType dataType = extractDataTypeOrRawWithTemplate(template, typeHierarchy, resolvedType);
        // final work
        return closestBridging(dataType, clazz);
    }

    private DataType extractDataTypeOrRawWithTemplate(
            DataTypeTemplate template, List<Type> typeHierarchy, Type type) {
        // template defines a data type
        if (template.dataType != null) {
            return template.dataType;
        }
        try {
            return extractDataTypeOrError(template, typeHierarchy, type);
        } catch (Throwable t) {
            throw extractionError(
                    t,
                    "Could not extract a data type from '%s'%s. "
                            + "Please pass the required data type manually or allow RAW types.",
                    type.toString(),
                    contextExplanation);
        }
    }

    private DataType extractDataTypeOrError(
            DataTypeTemplate template, List<Type> typeHierarchy, Type type) {
        // still a type variable
        if (type instanceof TypeVariable) {
            throw extractionError(
                    "Unresolved type variable '%s'. A data type cannot be extracted from a type variable. "
                            + "The original content might have been erased due to Java type erasure.",
                    type.toString());
        }

        // ARRAY
        DataType resultDataType = extractArrayType(template, typeHierarchy, type);
        if (resultDataType != null) {
            return resultDataType;
        }

        // early and helpful exception for common mistakes
        checkForCommonErrors(type);

        // PREDEFINED
        resultDataType = extractPredefinedType(template, type);
        if (resultDataType != null) {
            return resultDataType;
        }

        // MAP
        resultDataType = extractMapType(template, typeHierarchy, type);
        if (resultDataType != null) {
            return resultDataType;
        }

        return null;
    }

    private @Nullable DataType extractArrayType(
            DataTypeTemplate template, List<Type> typeHierarchy, Type type) {
        // prefer BYTES over ARRAY<TINYINT> for byte[]
        if (type == byte[].class) {
            return DataTypes.BYTES();
        }
        // for T[]
        else if (type instanceof GenericArrayType) {
            final GenericArrayType genericArray = (GenericArrayType) type;
            return DataTypes.ARRAY(
                    extractDataTypeOrRaw(
                            template, typeHierarchy, genericArray.getGenericComponentType()));
        }

        final Class<?> clazz = toClass(type);
        if (clazz == null) {
            return null;
        }

        // for my.custom.Pojo[][]
        if (clazz.isArray()) {
            return DataTypes.ARRAY(
                    extractDataTypeOrRaw(template, typeHierarchy, clazz.getComponentType()));
        }

        // for List<T>
        // we only allow List here (not a subclass) because we cannot guarantee more
        // specific
        // data structures after conversion
        if (clazz != List.class) {
            return null;
        }
        if (!(type instanceof ParameterizedType)) {
            throw extractionError(
                    "The class '%s' needs generic parameters for an array type.",
                    List.class.getName());
        }
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final DataType element =
                extractDataTypeOrRaw(
                        template, typeHierarchy, parameterizedType.getActualTypeArguments()[0]);
        return DataTypes.ARRAY(element).bridgedTo(List.class);
    }

    private void checkForCommonErrors(Type type) {
        final Class<?> clazz = toClass(type);
        if (clazz == null) {
            return;
        }

        if (clazz == Object.class) {
            throw extractionError(
                    "Cannot extract a data type from a pure '%s' class. "
                            + "Usually, this indicates that class information is missing or got lost. "
                            + "Please specify a more concrete class or treat it as a RAW type.",
                    Object.class.getName());
        }
    }

    private @Nullable DataType extractPredefinedType(DataTypeTemplate template, Type type) {
        final Class<?> clazz = toClass(type);
        // all predefined types are representable as classes
        if (clazz == null) {
            return null;
        }

        // DECIMAL
        if (clazz == BigDecimal.class) {
            if (template.defaultDecimalPrecision != null && template.defaultDecimalScale != null) {
                return DataTypes.DECIMAL(
                        template.defaultDecimalPrecision, template.defaultDecimalScale);
            } else if (template.defaultDecimalPrecision != null) {
                return DataTypes.DECIMAL(template.defaultDecimalPrecision, 0);
            }
            throw extractionError(
                    "Values of '%s' need fixed precision and scale.", BigDecimal.class.getName());
        }

        // TIME
        else if (clazz == java.sql.Time.class || clazz == java.time.LocalTime.class) {
            if (template.defaultSecondPrecision != null) {
                return DataTypes.TIME(template.defaultSecondPrecision).bridgedTo(clazz);
            }
        }

        // TIMESTAMP
        else if (clazz == java.sql.Timestamp.class || clazz == java.time.LocalDateTime.class) {
            if (template.defaultSecondPrecision != null) {
                return DataTypes.TIMESTAMP(template.defaultSecondPrecision).bridgedTo(clazz);
            }
        }

        // TIMESTAMP WITH TIME ZONE
        else if (clazz == java.time.OffsetDateTime.class) {
            if (template.defaultSecondPrecision != null) {
                return DataTypes.TIMESTAMP_WITH_TIME_ZONE(template.defaultSecondPrecision);
            }
        }

        // TIMESTAMP WITH LOCAL TIME ZONE
        else if (clazz == java.time.Instant.class) {
            if (template.defaultSecondPrecision != null) {
                return DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(template.defaultSecondPrecision);
            }
        }

        // INTERVAL SECOND
        else if (clazz == java.time.Duration.class) {
            if (template.defaultSecondPrecision != null) {
                return DataTypes.INTERVAL(DataTypes.SECOND(template.defaultSecondPrecision));
            }
        }

        // INTERVAL YEAR TO MONTH
        else if (clazz == java.time.Period.class) {
            if (template.defaultYearPrecision != null && template.defaultYearPrecision == 0) {
                return DataTypes.INTERVAL(DataTypes.MONTH());
            } else if (template.defaultYearPrecision != null) {
                return DataTypes.INTERVAL(
                        DataTypes.YEAR(template.defaultYearPrecision), DataTypes.MONTH());
            }
        }

        return ClassDataTypeConverter.extractDataType(clazz).orElse(null);
    }

    private @Nullable DataType extractMapType(
            DataTypeTemplate template, List<Type> typeHierarchy, Type type) {
        final Class<?> clazz = toClass(type);
        // we only allow Map here (not a subclass) because we cannot guarantee more
        // specific
        // data structures after conversion
        if (clazz != Map.class) {
            return null;
        }
        if (!(type instanceof ParameterizedType)) {
            throw extractionError(
                    "The class '%s' needs generic parameters for a map type.", Map.class.getName());
        }
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final DataType key =
                extractDataTypeOrRaw(
                        template, typeHierarchy, parameterizedType.getActualTypeArguments()[0]);
        final DataType value =
                extractDataTypeOrRaw(
                        template, typeHierarchy, parameterizedType.getActualTypeArguments()[1]);
        return DataTypes.MAP(key, value);
    }

    /**
     * Use closest class for data type if possible. Even though a hint might have provided some data
     * type, in many cases, the conversion class can be enriched with the extraction type itself.
     */
    private DataType closestBridging(DataType dataType, @Nullable Class<?> clazz) {
        // no context class or conversion class is already more specific than context
        // class
        if (clazz == null || clazz.isAssignableFrom(dataType.getConversionClass())) {
            return dataType;
        }
        final LogicalType logicalType = dataType.getLogicalType();
        final boolean supportsConversion =
                logicalType.supportsInputConversion(clazz)
                        || logicalType.supportsOutputConversion(clazz);
        if (supportsConversion) {
            return dataType.bridgedTo(clazz);
        }
        return dataType;
    }
}
