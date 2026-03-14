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

import com.hhoa.kline.plugins.jdbc.types.DataType;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * Internal representation of a {@link DataTypeHint}.
 *
 * <p>All parameters of a template are optional. An empty annotation results in a template where all
 * members are {@code null}.
 */
final class DataTypeTemplate {

    final @Nullable DataType dataType;

    final @Nullable Integer defaultDecimalPrecision;

    final @Nullable Integer defaultDecimalScale;

    final @Nullable Integer defaultYearPrecision;

    final @Nullable Integer defaultSecondPrecision;

    private DataTypeTemplate(
            @Nullable DataType dataType,
            @Nullable Integer defaultDecimalPrecision,
            @Nullable Integer defaultDecimalScale,
            @Nullable Integer defaultYearPrecision,
            @Nullable Integer defaultSecondPrecision) {
        this.dataType = dataType;
        this.defaultDecimalPrecision = defaultDecimalPrecision;
        this.defaultDecimalScale = defaultDecimalScale;
        this.defaultYearPrecision = defaultYearPrecision;
        this.defaultSecondPrecision = defaultSecondPrecision;
    }

    /** Creates an instance with no parameter content. */
    static DataTypeTemplate fromDefaults() {
        return new DataTypeTemplate(null, null, null, null, null);
    }

    /** Copies this template but removes the explicit data type (if available). */
    DataTypeTemplate copyWithoutDataType() {
        return new DataTypeTemplate(
                null,
                defaultDecimalPrecision,
                defaultDecimalScale,
                defaultYearPrecision,
                defaultSecondPrecision);
    }

    /**
     * Merges this template with an inner annotation. The inner annotation has highest precedence
     * and definitely determines the explicit data type (if available).
     */
    DataTypeTemplate mergeWithInnerAnnotation(DataTypeTemplate otherTemplate) {
        return new DataTypeTemplate(
                otherTemplate.dataType,
                rightValueIfNotNull(defaultDecimalPrecision, otherTemplate.defaultDecimalPrecision),
                rightValueIfNotNull(defaultDecimalScale, otherTemplate.defaultDecimalScale),
                rightValueIfNotNull(defaultYearPrecision, otherTemplate.defaultYearPrecision),
                rightValueIfNotNull(defaultSecondPrecision, otherTemplate.defaultSecondPrecision));
    }

    /** Returns whether the given class is eligible for being treated as RAW type. */
    /** Returns whether the given class must be treated as RAW type. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataTypeTemplate that = (DataTypeTemplate) o;
        return Objects.equals(dataType, that.dataType)
                && Objects.equals(defaultDecimalPrecision, that.defaultDecimalPrecision)
                && Objects.equals(defaultDecimalScale, that.defaultDecimalScale)
                && Objects.equals(defaultYearPrecision, that.defaultYearPrecision)
                && Objects.equals(defaultSecondPrecision, that.defaultSecondPrecision);
    }

    @Override
    public int hashCode() {
        int result =
                Objects.hash(
                        dataType,
                        defaultDecimalPrecision,
                        defaultDecimalScale,
                        defaultYearPrecision,
                        defaultSecondPrecision);
        return result;
    }

    // --------------------------------------------------------------------------------------------

    private static <T> T rightValueIfNotNull(T l, T r) {
        if (r != null) {
            return r;
        }
        return l;
    }
}
