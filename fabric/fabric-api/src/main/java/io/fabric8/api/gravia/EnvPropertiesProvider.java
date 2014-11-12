/*
 * #%L
 * Gravia :: Runtime :: API
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.api.gravia;


/**
 * A {@link PropertiesProvider} backed by Environmental Variables.
 */
public class EnvPropertiesProvider extends AbstractPropertiesProvider {

    public static final String ENV_PREFIX_KEY = "environment.prefix";
    public static final String DEFAULT_ENV_PREFIX = "GRAVIA_";
    private static final String ENV_REPLACE_PATTERN = "-|\\.";

    private final String environmentVariablePrefix;

    public EnvPropertiesProvider() {
        this(DEFAULT_ENV_PREFIX);
    }

    public EnvPropertiesProvider(PropertiesProvider source) {
        this(String.valueOf(source.getProperty(ENV_PREFIX_KEY)));
    }

    public EnvPropertiesProvider(String environmentVariablePrefix) {
        IllegalArgumentAssertion.assertNotNull(environmentVariablePrefix, "Environmental variable prefix");
        this.environmentVariablePrefix = environmentVariablePrefix;
    }

    @Override
    public Object getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        String envVar =  SecurityActions.getEnv(toEnvVariable(environmentVariablePrefix, key), null);
        return envVar != null ? envVar : defaultValue;
    }

    /**
     * Convert a system property name to an env variable name.
     * The convention is that env variables are prefixed with the specified prefix, capitalized and dots are converted to
     * underscores.
     * @param prefix    The prefix to use.
     * @param name      The system property name to convert.
     * @return          The corresponding env variable name.
     */
    private static String toEnvVariable(String prefix, String name) {
        if (name == null || name.isEmpty()) {
            return name;
        } else {
            return prefix + name.replaceAll(ENV_REPLACE_PATTERN,"_").toUpperCase();
        }
    }
}
