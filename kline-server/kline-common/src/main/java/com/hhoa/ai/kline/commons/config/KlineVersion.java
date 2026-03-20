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

package com.hhoa.ai.kline.commons.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** It used for API versioning, during SQL/Table API upgrades, and for migration tests. */
public enum KlineVersion {

    // NOTE: the version strings must not change,
    // as they are used to locate snapshot file paths.
    // The definition order (enum ordinal) matters for performing version arithmetic.
    v1_0_0("1.0.0");

    private final String versionStr;

    KlineVersion(String versionStr) {
        this.versionStr = versionStr;
    }

    @Override
    public String toString() {
        return versionStr;
    }

    public boolean isNewerVersionThan(KlineVersion otherVersion) {
        return this.ordinal() > otherVersion.ordinal();
    }

    /** Returns all versions within the defined range, inclusive both start and end. */
    public static Set<KlineVersion> rangeOf(KlineVersion start, KlineVersion end) {
        return Stream.of(KlineVersion.values())
                .filter(v -> v.ordinal() >= start.ordinal() && v.ordinal() <= end.ordinal())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static final Map<String, KlineVersion> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(v -> v.versionStr, Function.identity()));

    public static Optional<KlineVersion> byCode(String code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }

    public static KlineVersion valueOf(int majorVersion, int minorVersion) {
        return KlineVersion.valueOf("v" + majorVersion + "_" + minorVersion);
    }

    public static List<KlineVersion> all() {
        return Arrays.asList(values());
    }

    /** Returns the version for the current branch. */
    public static KlineVersion current() {
        return values()[values().length - 1];
    }
}
