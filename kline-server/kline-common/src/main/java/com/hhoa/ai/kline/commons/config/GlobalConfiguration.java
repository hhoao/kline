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

import com.google.common.annotations.VisibleForTesting;
import com.hhoa.ai.kline.commons.utils.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global configuration object for . Similar to Java properties configuration objects it includes
 * key-value pairs which represent the framework's configuration.
 */
public final class GlobalConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalConfiguration.class);

    public static final String LEGACY_FLINK_CONF_FILENAME = "flink-conf.yaml";

    public static final String FLINK_CONF_FILENAME = "config.yaml";

    // key separator character
    private static final String KEY_SEPARATOR = ".";

    // the keys whose values should be hidden
    private static final String[] SENSITIVE_KEYS =
            new String[] {
                "password",
                "secret",
                "fs.azure.account.key",
                "apikey",
                "auth-params",
                "service-key",
                "token",
                "basic-auth",
                "jaas.config",
                "http-headers"
            };

    // the hidden content to be displayed
    public static final String HIDDEN_CONTENT = "******";

    private static boolean standardYaml = true;

    // --------------------------------------------------------------------------------------------

    private GlobalConfiguration() {}

    private static void logConfiguration(String prefix, Configuration config) {
        config.confData.forEach(
                (key, value) ->
                        LOG.info(
                                "{} configuration property: {}, {}",
                                prefix,
                                key,
                                isSensitive(key) ? HIDDEN_CONTENT : value));
    }

    /**
     * Flattens a nested configuration map to be only one level deep.
     *
     * <p>Nested keys are concatinated using the {@code KEY_SEPARATOR} character. So that:
     *
     * <pre>
     * keyA:
     *   keyB:
     *     keyC: "hello"
     *     keyD: "world"
     * </pre>
     *
     * <p>becomes:
     *
     * <pre>
     * keyA.keyB.keyC: "hello"
     * keyA.keyB.keyD: "world"
     * </pre>
     *
     * @param config an arbitrarily nested config map
     * @param keyPrefix The string to prefix the keys in the current config level
     * @return A flattened, 1 level deep map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> flatten(Map<String, Object> config, String keyPrefix) {
        final Map<String, Object> flattenedMap = new HashMap<>();

        config.forEach(
                (key, value) -> {
                    String flattenedKey = keyPrefix + key;
                    if (value instanceof Map) {
                        Map<String, Object> e = (Map<String, Object>) value;
                        flattenedMap.putAll(flatten(e, flattenedKey + KEY_SEPARATOR));
                    } else {
                        flattenedMap.put(flattenedKey, value);
                    }
                });

        return flattenedMap;
    }

    private static Map<String, Object> flatten(Map<String, Object> config) {
        // Since we start flattening from the root, keys should not be prefixed with anything.
        return flatten(config, "");
    }

    /**
     * Check whether the key is a hidden key.
     *
     * @param key the config key
     */
    public static boolean isSensitive(String key) {
        Preconditions.checkNotNull(key, "key is null");
        final String keyInLower = key.toLowerCase();
        for (String hideKey : SENSITIVE_KEYS) {
            if (keyInLower.length() >= hideKey.length() && keyInLower.contains(hideKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStandardYaml() {
        return standardYaml;
    }

    @VisibleForTesting
    public static void setStandardYaml(boolean standardYaml) {
        GlobalConfiguration.standardYaml = standardYaml;
    }
}
