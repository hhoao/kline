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

import static com.hhoa.ai.kline.commons.config.ConfigurationUtils.canBePrefixMap;
import static com.hhoa.ai.kline.commons.config.ConfigurationUtils.containsPrefixMap;
import static com.hhoa.ai.kline.commons.config.ConfigurationUtils.convertToPropertiesPrefixed;
import static com.hhoa.ai.kline.commons.config.ConfigurationUtils.removePrefixMap;
import static com.hhoa.ai.kline.commons.utils.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.hhoa.ai.kline.commons.utils.collection.CollectionUtils;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lightweight configuration object which stores key/value pairs. */
public class Configuration extends GlobalJobParameters
        implements Serializable, Cloneable, ReadableConfig, WritableConfig {

    @Serial private static final long serialVersionUID = 1L;

    /** The log object used for debugging. */
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    /**
     * Stores the concrete key/value pairs of this configuration object.
     *
     * <p>NOTE: This map stores the values that are actually used, and does not include any escaping
     * that is required by the standard YAML syntax.
     */
    protected final HashMap<String, Object> confData;

    /** Creates a new empty configuration. */
    public Configuration() {
        this.confData = new HashMap<>();
    }

    @VisibleForTesting
    public Configuration(boolean standardYaml) {
        this.confData = new HashMap<>();
    }

    /**
     * Creates a new configuration with the copy of the given configuration.
     *
     * @param other The configuration to copy the entries from.
     */
    public Configuration(Configuration other) {
        this.confData = new HashMap<>(other.confData);
    }

    // --------------------------------------------------------------------------------------------

    /** Creates a new configuration that is initialized with the options of the given map. */
    public static Configuration fromMap(Map<String, String> map) {
        final Configuration configuration = new Configuration();
        map.forEach(configuration::setString);
        return configuration;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the value associated with the given key as a string. We encourage users and
     * developers to always use ConfigOption for getting the configurations if possible, for its
     * rich description, type, default-value and other supports. The string-key-based getter should
     * only be used when ConfigOption is not applicable, e.g., the key is programmatically generated
     * in runtime.
     *
     * @param key the key pointing to the associated value
     * @param defaultValue the default value which is returned in case there is no value associated
     *     with the given key
     * @return the (default) value associated with the given key
     */
    public String getString(String key, String defaultValue) {
        return getRawValue(key).map(ConfigurationUtils::convertToString).orElse(defaultValue);
    }

    /**
     * Adds the given key/value pair to the configuration object. We encourage users and developers
     * to always use ConfigOption for setting the configurations if possible, for its rich
     * description, type, default-value and other supports. The string-key-based setter should only
     * be used when ConfigOption is not applicable, e.g., the key is programmatically generated in
     * runtime.
     *
     * @param key the key of the key/value pair to be added
     * @param value the value of the key/value pair to be added
     */
    public void setString(String key, String value) {
        setValueInternal(key, value);
    }

    /**
     * Returns the value associated with the given key as a byte array.
     *
     * @param key The key pointing to the associated value.
     * @param defaultValue The default value which is returned in case there is no value associated
     *     with the given key.
     * @return the (default) value associated with the given key.
     */
    public byte[] getBytes(String key, byte[] defaultValue) {
        return getRawValue(key)
                .map(
                        o -> {
                            if (o.getClass().equals(byte[].class)) {
                                return (byte[]) o;
                            } else {
                                throw new IllegalArgumentException(
                                        String.format(
                                                "Configuration cannot evaluate value %s as a byte[] value",
                                                o));
                            }
                        })
                .orElse(defaultValue);
    }

    /**
     * Adds the given byte array to the configuration object. If key is <code>null</code> then
     * nothing is added.
     *
     * @param key The key under which the bytes are added.
     * @param bytes The bytes to be added.
     */
    public void setBytes(String key, byte[] bytes) {
        setValueInternal(key, bytes);
    }

    /**
     * Returns the value associated with the given config option as a string.
     *
     * @param configOption The configuration option
     * @return the (default) value associated with the given config option
     */
    public String getValue(ConfigOption<?> configOption) {
        return Optional.ofNullable(
                        getRawValueFromOption(configOption).orElseGet(configOption::defaultValue))
                .map(String::valueOf)
                .orElse(null);
    }

    /**
     * Returns the value associated with the given config option as an enum.
     *
     * @param enumClass The return enum class
     * @param configOption The configuration option
     * @throws IllegalArgumentException If the string associated with the given config option cannot
     *     be parsed as a value of the provided enum class.
     */
    public <T extends Enum<T>> T getEnum(
            final Class<T> enumClass, final ConfigOption<String> configOption) {
        checkNotNull(enumClass, "enumClass must not be null");
        checkNotNull(configOption, "configOption must not be null");

        Object rawValue = getRawValueFromOption(configOption).orElseGet(configOption::defaultValue);
        try {
            return ConfigurationUtils.convertToEnum(rawValue, enumClass);
        } catch (IllegalArgumentException ex) {
            final String errorMessage =
                    String.format(
                            "Value for config option %s must be one of %s (was %s)",
                            configOption.key(),
                            Arrays.toString(enumClass.getEnumConstants()),
                            rawValue);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the keys of all key/value pairs stored inside this configuration object.
     *
     * @return the keys of all key/value pairs stored inside this configuration object
     */
    public Set<String> keySet() {
        synchronized (this.confData) {
            return new HashSet<>(this.confData.keySet());
        }
    }

    /** Adds all entries in this {@code Configuration} to the given {@link Properties}. */
    public void addAllToProperties(Properties props) {
        synchronized (this.confData) {
            for (Map.Entry<String, Object> entry : this.confData.entrySet()) {
                props.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addAll(Configuration other) {
        synchronized (this.confData) {
            synchronized (other.confData) {
                this.confData.putAll(other.confData);
            }
        }
    }

    /**
     * Adds all entries from the given configuration into this configuration. The keys are prepended
     * with the given prefix.
     *
     * @param other The configuration whose entries are added to this configuration.
     * @param prefix The prefix to prepend.
     */
    public void addAll(Configuration other, String prefix) {
        final StringBuilder bld = new StringBuilder();
        bld.append(prefix);
        final int pl = bld.length();

        synchronized (this.confData) {
            synchronized (other.confData) {
                for (Map.Entry<String, Object> entry : other.confData.entrySet()) {
                    bld.setLength(pl);
                    bld.append(entry.getKey());
                    this.confData.put(bld.toString(), entry.getValue());
                }
            }
        }
    }

    @Override
    public Configuration clone() {
        Configuration config = new Configuration();
        config.addAll(this);

        return config;
    }

    /**
     * Checks whether there is an entry with the specified key.
     *
     * @param key key of entry
     * @return true if the key is stored, false otherwise
     */
    public boolean containsKey(String key) {
        synchronized (this.confData) {
            return this.confData.containsKey(key);
        }
    }

    /**
     * Checks whether there is an entry for the given config option.
     *
     * @param configOption The configuration option
     * @return <tt>true</tt> if a valid (current or deprecated) key of the config option is stored,
     *     <tt>false</tt> otherwise
     */
    public boolean contains(ConfigOption<?> configOption) {
        synchronized (this.confData) {
            final BiFunction<String, Boolean, Optional<Boolean>> applier =
                    (key, canBePrefixMap) -> {
                        if (canBePrefixMap && containsPrefixMap(this.confData, key)
                                || this.confData.containsKey(key)) {
                            return Optional.of(true);
                        }
                        return Optional.empty();
                    };
            return applyWithOption(configOption, applier).orElse(false);
        }
    }

    private <T> Optional<T> applyWithOption(
            ConfigOption<?> option, BiFunction<String, Boolean, Optional<T>> applier) {
        final boolean canBePrefixMap = canBePrefixMap(option);
        final Optional<T> valueFromExactKey = applier.apply(option.key(), canBePrefixMap);
        if (valueFromExactKey.isPresent()) {
            return valueFromExactKey;
        } else if (option.hasFallbackKeys()) {
            // try the fallback keys
            for (FallbackKey fallbackKey : option.fallbackKeys()) {
                final Optional<T> valueFromFallbackKey =
                        applier.apply(fallbackKey.getKey(), canBePrefixMap);
                if (valueFromFallbackKey.isPresent()) {
                    loggingFallback(fallbackKey, option);
                    return valueFromFallbackKey;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Please check the java doc of {@link #getRawValueFromOption(ConfigOption)}. If no keys are
     * found in {@link Configuration}, default value of the given option will return. Please make
     * sure there will be at least one value available. Otherwise, a NPE will be thrown by when the
     * value is used.
     *
     * <p>NOTE: current logic is not able to get the default value of the fallback key's
     * ConfigOption, in case the given ConfigOption has no default value. If you want to use
     * fallback key, please make sure its value could be found in {@link Configuration} at runtime.
     *
     * @param option metadata of the option to read
     * @return the value of the given option
     */
    @Override
    public <T> T get(ConfigOption<T> option) {
        return getOptional(option).orElseGet(option::defaultValue);
    }

    /**
     * Returns the value associated with the given config option as a T. If no value is mapped under
     * any key of the option, it returns the specified default instead of the option's default
     * value.
     *
     * @param configOption The configuration option
     * @param overrideDefault The value to return if no value was mapper for any key of the option
     * @return the configured value associated with the given config option, or the overrideDefault
     */
    public <T> T get(ConfigOption<T> configOption, T overrideDefault) {
        return getOptional(configOption).orElse(overrideDefault);
    }

    @Override
    public <T> Optional<T> getOptional(ConfigOption<T> option) {
        Optional<Object> rawValue = getRawValueFromOption(option);
        Class<?> clazz = option.getClazz();

        try {
            if (option.isList()) {
                return rawValue.map(v -> ConfigurationUtils.convertToList(v, clazz));
            } else {
                return rawValue.map(v -> ConfigurationUtils.convertValue(v, clazz));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    GlobalConfiguration.isSensitive(option.key())
                            ? String.format("Could not parse value for key '%s'.", option.key())
                            : String.format(
                                    "Could not parse value '%s' for key '%s'.",
                                    rawValue.map(Object::toString).orElse(""), option.key()),
                    e);
        }
    }

    @Override
    public <T> Configuration set(ConfigOption<T> option, T value) {
        final boolean canBePrefixMap = canBePrefixMap(option);
        setValueInternal(option.key(), value, canBePrefixMap);
        return this;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public Map<String, String> toMap() {
        synchronized (this.confData) {
            Map<String, String> ret =
                    CollectionUtils.newHashMapWithExpectedSize(this.confData.size());
            for (Map.Entry<String, Object> entry : confData.entrySet()) {
                ret.put(entry.getKey(), ConfigurationUtils.convertToString(entry.getValue()));
            }
            return ret;
        }
    }

    /**
     * Convert Config into a {@code Map<String, String>} representation.
     *
     * <p>NOTE: This method is extracted from the {@link Configuration#toMap} method and should be
     * called when Config needs to be written to a file.
     *
     * <p>This method ensures the value is properly escaped when writing the key-value pair to a
     * standard YAML file.
     */
    public Map<String, String> toFileWritableMap() {
        return toMap();
    }

    /**
     * Removes given config option from the configuration.
     *
     * @param configOption config option to remove
     * @param <T> Type of the config option
     * @return true is config has been removed, false otherwise
     */
    public <T> boolean removeConfig(ConfigOption<T> configOption) {
        synchronized (this.confData) {
            final BiFunction<String, Boolean, Optional<Boolean>> applier =
                    (key, canBePrefixMap) -> {
                        if (canBePrefixMap && removePrefixMap(this.confData, key)
                                || this.confData.remove(key) != null) {
                            return Optional.of(true);
                        }
                        return Optional.empty();
                    };
            return applyWithOption(configOption, applier).orElse(false);
        }
    }

    /**
     * Removes given key from the configuration.
     *
     * @param key key of a config option to remove
     * @return true is config has been removed, false otherwise
     */
    public boolean removeKey(String key) {
        synchronized (this.confData) {
            boolean removed = this.confData.remove(key) != null;
            removed |= removePrefixMap(confData, key);
            return removed;
        }
    }

    // --------------------------------------------------------------------------------------------

    <T> void setValueInternal(String key, T value, boolean canBePrefixMap) {
        if (key == null) {
            throw new NullPointerException("Key must not be null.");
        }
        if (value == null) {
            throw new NullPointerException("Value must not be null.");
        }

        synchronized (this.confData) {
            if (canBePrefixMap) {
                removePrefixMap(this.confData, key);
            }
            this.confData.put(key, value);
        }
    }

    private <T> void setValueInternal(String key, T value) {
        setValueInternal(key, value, false);
    }

    private Optional<Object> getRawValue(String key) {
        return getRawValue(key, false);
    }

    private Optional<Object> getRawValue(String key, boolean canBePrefixMap) {
        if (key == null) {
            throw new NullPointerException("Key must not be null.");
        }

        synchronized (this.confData) {
            final Object valueFromExactKey = this.confData.get(key);
            if (!canBePrefixMap || valueFromExactKey != null) {
                return Optional.ofNullable(valueFromExactKey);
            }
            final Map<String, String> valueFromPrefixMap =
                    convertToPropertiesPrefixed(confData, key);
            if (valueFromPrefixMap.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(valueFromPrefixMap);
        }
    }

    /**
     * This method will do the following steps to get the value of a config option:
     *
     * <p>1. get the value from {@link Configuration}. <br>
     * 2. if key is not found, try to get the value with fallback keys from {@link Configuration}
     * <br>
     * 3. if no fallback keys are found, return {@link Optional#empty()}. <br>
     *
     * @return the value of the configuration or {@link Optional#empty()}.
     */
    private Optional<Object> getRawValueFromOption(ConfigOption<?> configOption) {
        return applyWithOption(configOption, this::getRawValue);
    }

    private void loggingFallback(FallbackKey fallbackKey, ConfigOption<?> configOption) {
        if (fallbackKey.isDeprecated()) {
            LOG.warn(
                    "Config uses deprecated configuration key '{}' instead of proper key '{}'",
                    fallbackKey.getKey(),
                    configOption.key());
        } else {
            LOG.info(
                    "Config uses fallback configuration key '{}' instead of key '{}'",
                    fallbackKey.getKey(),
                    configOption.key());
        }
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public int hashCode() {
        int hash = 0;
        for (String s : this.confData.keySet()) {
            hash ^= s.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Configuration) {
            Map<String, Object> otherConf = ((Configuration) obj).confData;

            for (Map.Entry<String, Object> e : this.confData.entrySet()) {
                Object thisVal = e.getValue();
                Object otherVal = otherConf.get(e.getKey());

                if (!thisVal.getClass().equals(byte[].class)) {
                    if (!thisVal.equals(otherVal)) {
                        return false;
                    }
                } else if (otherVal.getClass().equals(byte[].class)) {
                    if (!Arrays.equals((byte[]) thisVal, (byte[]) otherVal)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return ConfigurationUtils.hideSensitiveValues(
                        this.confData.entrySet().stream()
                                .collect(
                                        Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> entry.getValue().toString())))
                .toString();
    }
}
