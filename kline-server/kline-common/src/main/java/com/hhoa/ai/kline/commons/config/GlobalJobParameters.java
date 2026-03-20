package com.hhoa.ai.kline.commons.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract class for a custom user configuration object registered at the execution config.
 *
 * <p>This user config is accessible at runtime through
 * getRuntimeContext().getExecutionConfig().GlobalJobParameters()
 */
public class GlobalJobParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Convert UserConfig into a {@code Map<String, String>} representation. This can be used by the
     * runtime, for example for presenting the user config in the web frontend.
     *
     * @return Key/Value representation of the UserConfig
     */
    public Map<String, String> toMap() {
        return Collections.emptyMap();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash();
    }
}
