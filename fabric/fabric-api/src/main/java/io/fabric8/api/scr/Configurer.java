package io.fabric8.api.scr;

import java.util.Map;

public interface Configurer {

    /**
     * Configures the specified instance with the provided configuration.
     * @param configuration The configuration.
     * @param target        The target that will receive the configuration.
     * @param <T>
     */
    <T> void configure(Map<String, ?> configuration, T target) throws Exception;
}
