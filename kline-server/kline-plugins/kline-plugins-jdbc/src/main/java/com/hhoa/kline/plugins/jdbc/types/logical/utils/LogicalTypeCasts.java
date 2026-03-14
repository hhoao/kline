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

import static com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeFamily.*;
import static com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot.*;
import static com.hhoa.kline.plugins.jdbc.types.logical.utils.LogicalTypeChecks.*;

import com.hhoa.kline.plugins.jdbc.types.logical.DateType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalType;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeFamily;
import com.hhoa.kline.plugins.jdbc.types.logical.LogicalTypeRoot;
import com.hhoa.kline.plugins.jdbc.types.logical.VarBinaryType;
import com.hhoa.kline.plugins.jdbc.types.logical.VarCharType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for casting {@link LogicalType}.
 *
 * <p>This class aims to be compatible with the SQL standard. It is inspired by Apache Calcite's
 * {@code SqlTypeUtil#canCastFrom} method.
 *
 * <p>Casts can be performed in two ways: implicit or explicit.
 *
 * <p>Explicit casts correspond to the SQL cast specification and represent the logic behind a
 * {@code CAST(sourceType AS targetType)} operation. For example, it allows for converting most
 * types of the {@link LogicalTypeFamily#PREDEFINED} family to types of the {@link
 * LogicalTypeFamily#CHARACTER_STRING} family.
 *
 * <p>Implicit casts are used for safe type widening and type generalization (finding a common
 * supertype for a set of types) without loss of information. Implicit casts are similar to the Java
 * semantics (e.g. this is not possible: {@code int x = (String) z}).
 *
 * <p>Conversions that are defined by the {@link LogicalType} (e.g. interpreting a {@link DateType}
 * as integer value) are not considered here. They are an internal bridging feature that is not
 * standard compliant. If at all, {@code CONVERT} methods should make such conversions available.
 */
public final class LogicalTypeCasts {

    private static final Map<LogicalTypeRoot, Set<LogicalTypeRoot>> implicitCastingRules;

    private static final Map<LogicalTypeRoot, Set<LogicalTypeRoot>> explicitCastingRules;

    static {
        implicitCastingRules = new HashMap<>();
        explicitCastingRules = new HashMap<>();

        // identity casts

        for (LogicalTypeRoot typeRoot : allTypes()) {
            castTo(typeRoot).implicitFrom(typeRoot).build();
        }

        // cast specification

        castTo(CHAR)
                .implicitFrom(CHAR)
                .explicitFromFamily(PREDEFINED, CONSTRUCTED)
                .explicitFrom(NULL)
                .build();

        castTo(VARCHAR)
                .implicitFromFamily(CHARACTER_STRING)
                .explicitFromFamily(PREDEFINED, CONSTRUCTED)
                .explicitFrom(NULL)
                .build();

        castTo(BOOLEAN)
                .implicitFrom(BOOLEAN)
                .explicitFromFamily(CHARACTER_STRING, INTEGER_NUMERIC)
                .build();

        castTo(BINARY)
                .implicitFrom(BINARY)
                .explicitFromFamily(CHARACTER_STRING)
                .explicitFrom(VARBINARY)
                .build();

        castTo(VARBINARY)
                .implicitFromFamily(BINARY_STRING)
                .explicitFromFamily(CHARACTER_STRING)
                .explicitFrom(BINARY)
                .build();

        castTo(DECIMAL)
                .implicitFromFamily(NUMERIC)
                .explicitFromFamily(CHARACTER_STRING, INTERVAL)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(TINYINT)
                .implicitFrom(TINYINT)
                .explicitFromFamily(NUMERIC, CHARACTER_STRING, INTERVAL)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(SMALLINT)
                .implicitFrom(TINYINT, SMALLINT)
                .explicitFromFamily(NUMERIC, CHARACTER_STRING, INTERVAL)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(INTEGER)
                .implicitFrom(TINYINT, SMALLINT, INTEGER)
                .explicitFromFamily(NUMERIC, CHARACTER_STRING, INTERVAL)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(BIGINT)
                .implicitFrom(TINYINT, SMALLINT, INTEGER, BIGINT)
                .explicitFromFamily(NUMERIC, CHARACTER_STRING, INTERVAL)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(FLOAT)
                .implicitFrom(TINYINT, SMALLINT, INTEGER, BIGINT, FLOAT, DECIMAL)
                .explicitFromFamily(NUMERIC, CHARACTER_STRING)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(DOUBLE)
                .implicitFromFamily(NUMERIC)
                .explicitFromFamily(CHARACTER_STRING)
                .explicitFrom(BOOLEAN, TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .build();

        castTo(DATE)
                .implicitFrom(DATE, TIMESTAMP_WITHOUT_TIME_ZONE)
                .explicitFromFamily(TIMESTAMP, CHARACTER_STRING)
                .build();

        castTo(TIME_WITHOUT_TIME_ZONE)
                .implicitFrom(TIME_WITHOUT_TIME_ZONE, TIMESTAMP_WITHOUT_TIME_ZONE)
                .explicitFromFamily(TIME, TIMESTAMP, CHARACTER_STRING)
                .build();

        castTo(TIMESTAMP_WITHOUT_TIME_ZONE)
                .implicitFrom(TIMESTAMP_WITHOUT_TIME_ZONE, TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .explicitFromFamily(DATETIME, CHARACTER_STRING, NUMERIC)
                .build();

        castTo(TIMESTAMP_WITH_TIME_ZONE)
                .implicitFrom(TIMESTAMP_WITH_TIME_ZONE)
                .explicitFromFamily(DATETIME, CHARACTER_STRING)
                .build();

        castTo(TIMESTAMP_WITH_LOCAL_TIME_ZONE)
                .implicitFrom(TIMESTAMP_WITH_LOCAL_TIME_ZONE, TIMESTAMP_WITHOUT_TIME_ZONE)
                .explicitFromFamily(DATETIME, CHARACTER_STRING, NUMERIC)
                .build();

        castTo(INTERVAL_YEAR_MONTH)
                .implicitFrom(INTERVAL_YEAR_MONTH)
                .explicitFromFamily(EXACT_NUMERIC, CHARACTER_STRING)
                .build();

        castTo(INTERVAL_DAY_TIME)
                .implicitFrom(INTERVAL_DAY_TIME)
                .explicitFromFamily(EXACT_NUMERIC, CHARACTER_STRING)
                .build();
    }

    /**
     * Returns whether the source type can be safely interpreted as the target type. This allows
     * avoiding casts by ignoring some logical properties. This is basically a relaxed {@link
     * LogicalType#equals(Object)}.
     *
     * <p>In particular this means:
     *
     * <p>Atomic, non-string types (INT, BOOLEAN, ...) and user-defined structured types must be
     * fully equal (i.e. {@link LogicalType#equals(Object)}). However, a NOT NULL type can be stored
     * in NULL type but not vice versa.
     *
     * <p>Atomic, string types must be contained in the target type (e.g. CHAR(2) is contained in
     * VARCHAR(3), but VARCHAR(2) is not contained in CHAR(3)). Same for binary strings.
     *
     * <p>Constructed types (ARRAY, ROW, MAP, etc.) and user-defined distinct type must be of same
     * kind but ignore field names and other logical attributes. Structured and row kinds are
     * compatible. However, all the children types ({@link LogicalType#getChildren()}) must be
     * compatible.
     */
    public static boolean supportsAvoidingCast(LogicalType sourceType, LogicalType targetType) {
        final CastAvoidanceChecker checker = new CastAvoidanceChecker(sourceType);
        return targetType.accept(checker);
    }

    /** See {@link #supportsAvoidingCast(LogicalType, LogicalType)}. */
    public static boolean supportsAvoidingCast(
            List<LogicalType> sourceTypes, List<LogicalType> targetTypes) {
        if (sourceTypes.size() != targetTypes.size()) {
            return false;
        }
        for (int i = 0; i < sourceTypes.size(); i++) {
            if (!supportsAvoidingCast(sourceTypes.get(i), targetTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the source type can be safely casted to the target type without loosing
     * information.
     *
     * <p>Implicit casts are used for type widening and type generalization (finding a common
     * supertype for a set of types). Implicit casts are similar to the Java semantics (e.g. this is
     * not possible: {@code int x = (String) z}).
     */
    public static boolean supportsImplicitCast(LogicalType sourceType, LogicalType targetType) {
        return supportsCasting(sourceType, targetType, false);
    }

    /**
     * Returns whether the source type can be casted to the target type.
     *
     * <p>Explicit casts correspond to the SQL cast specification and represent the logic behind a
     * {@code CAST(sourceType AS targetType)} operation. For example, it allows for converting most
     * types of the {@link LogicalTypeFamily#PREDEFINED} family to types of the {@link
     * LogicalTypeFamily#CHARACTER_STRING} family.
     */
    public static boolean supportsExplicitCast(LogicalType sourceType, LogicalType targetType) {
        return supportsCasting(sourceType, targetType, true);
    }

    // --------------------------------------------------------------------------------------------

    private static boolean supportsCasting(
            LogicalType sourceType, LogicalType targetType, boolean allowExplicit) {
        // a NOT NULL type cannot store a NULL type
        // but it might be useful to cast explicitly with knowledge about the data
        if (sourceType.isNullable() && !targetType.isNullable() && !allowExplicit) {
            return false;
        }
        // ignore nullability during compare
        if (sourceType.copy(true).equals(targetType.copy(true))) {
            return true;
        }

        final LogicalTypeRoot sourceRoot = sourceType.getTypeRoot();
        final LogicalTypeRoot targetRoot = targetType.getTypeRoot();

        if (sourceRoot == NULL) {
            // null can be cast to an arbitrary type
            return true;
        } else if (sourceType.is(INTERVAL) && targetType.is(EXACT_NUMERIC)) {
            // cast between interval and exact numeric is only supported if interval has a
            // single
            // field
            return isSingleFieldInterval(sourceType);
        } else if (sourceType.is(EXACT_NUMERIC) && targetType.is(INTERVAL)) {
            // cast between interval and exact numeric is only supported if interval has a
            // single
            // field
            return isSingleFieldInterval(targetType);
        }

        if (implicitCastingRules.get(targetRoot).contains(sourceRoot)) {
            return true;
        }
        if (allowExplicit) {
            return explicitCastingRules.get(targetRoot).contains(sourceRoot);
        }
        return false;
    }

    private static CastingRuleBuilder castTo(LogicalTypeRoot targetType) {
        return new CastingRuleBuilder(targetType);
    }

    private static LogicalTypeRoot[] allTypes() {
        return LogicalTypeRoot.values();
    }

    private static class CastingRuleBuilder {

        private final LogicalTypeRoot targetType;
        private final Set<LogicalTypeRoot> implicitSourceTypes = new HashSet<>();
        private final Set<LogicalTypeRoot> explicitSourceTypes = new HashSet<>();

        CastingRuleBuilder(LogicalTypeRoot targetType) {
            this.targetType = targetType;
        }

        CastingRuleBuilder implicitFrom(LogicalTypeRoot... sourceTypes) {
            this.implicitSourceTypes.addAll(Arrays.asList(sourceTypes));
            return this;
        }

        CastingRuleBuilder implicitFromFamily(LogicalTypeFamily... sourceFamilies) {
            for (LogicalTypeFamily family : sourceFamilies) {
                for (LogicalTypeRoot root : LogicalTypeRoot.values()) {
                    if (root.getFamilies().contains(family)) {
                        this.implicitSourceTypes.add(root);
                    }
                }
            }
            return this;
        }

        CastingRuleBuilder explicitFrom(LogicalTypeRoot... sourceTypes) {
            this.explicitSourceTypes.addAll(Arrays.asList(sourceTypes));
            return this;
        }

        CastingRuleBuilder explicitFromFamily(LogicalTypeFamily... sourceFamilies) {
            for (LogicalTypeFamily family : sourceFamilies) {
                for (LogicalTypeRoot root : LogicalTypeRoot.values()) {
                    if (root.getFamilies().contains(family)) {
                        this.explicitSourceTypes.add(root);
                    }
                }
            }
            return this;
        }

        /**
         * Should be called after {@link #explicitFromFamily(LogicalTypeFamily...)} to remove
         * previously added types.
         */
        CastingRuleBuilder explicitNotFromFamily(LogicalTypeFamily... sourceFamilies) {
            for (LogicalTypeFamily family : sourceFamilies) {
                for (LogicalTypeRoot root : LogicalTypeRoot.values()) {
                    if (root.getFamilies().contains(family)) {
                        this.explicitSourceTypes.remove(root);
                    }
                }
            }
            return this;
        }

        void build() {
            implicitCastingRules.put(targetType, implicitSourceTypes);
            explicitCastingRules.put(targetType, explicitSourceTypes);
        }
    }

    // --------------------------------------------------------------------------------------------

    /** Checks if a source type can safely be interpreted as the target type. */
    private static class CastAvoidanceChecker extends LogicalTypeDefaultVisitor<Boolean> {

        private final LogicalType sourceType;

        private CastAvoidanceChecker(LogicalType sourceType) {
            this.sourceType = sourceType;
        }

        @Override
        public Boolean visit(VarCharType targetType) {
            if (sourceType.isNullable() && !targetType.isNullable()) {
                return false;
            }
            // CHAR and VARCHAR are very compatible within bounds
            if (sourceType.isAnyOf(LogicalTypeRoot.CHAR, LogicalTypeRoot.VARCHAR)
                    && getLength(sourceType) <= targetType.getLength()) {
                return true;
            }
            return defaultMethod(targetType);
        }

        @Override
        public Boolean visit(VarBinaryType targetType) {
            if (sourceType.isNullable() && !targetType.isNullable()) {
                return false;
            }
            // BINARY and VARBINARY are very compatible within bounds
            if (sourceType.isAnyOf(LogicalTypeRoot.BINARY, LogicalTypeRoot.VARBINARY)
                    && getLength(sourceType) <= targetType.getLength()) {
                return true;
            }
            return defaultMethod(targetType);
        }

        @Override
        protected Boolean defaultMethod(LogicalType targetType) {
            // quick path
            if (sourceType == targetType) {
                return true;
            }

            if (sourceType.isNullable() && !targetType.isNullable()
                    || sourceType.getClass() != targetType.getClass()
                    || // TODO drop this line once we remove legacy types
                    sourceType.getTypeRoot() != targetType.getTypeRoot()) {
                return false;
            }

            final List<LogicalType> sourceChildren = sourceType.getChildren();
            final List<LogicalType> targetChildren = targetType.getChildren();
            if (sourceChildren.isEmpty()) {
                // handles all types that are not of family CONSTRUCTED or USER DEFINED
                return sourceType.equals(targetType) || sourceType.copy(true).equals(targetType);
            } else {
                // handles all types of CONSTRUCTED family as well as distinct types
                return supportsAvoidingCast(sourceChildren, targetChildren);
            }
        }
    }

    private LogicalTypeCasts() {
        // no instantiation
    }
}
