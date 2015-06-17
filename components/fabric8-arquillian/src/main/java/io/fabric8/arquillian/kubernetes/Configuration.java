/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.kubernetes;

import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.fabric8.arquillian.kubernetes.Constants.ANSI_LOGGER_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.CONFIG_FILE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.CONFIG_URL;
import static io.fabric8.arquillian.kubernetes.Constants.CONNECT_TO_SERVICES;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_CONFIG_FILE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.DEPENDENCIES;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_MASTER;
import static io.fabric8.arquillian.kubernetes.Constants.MASTER_URL;
import static io.fabric8.arquillian.kubernetes.Constants.POLL_INTERVAL;
import static io.fabric8.arquillian.kubernetes.Constants.SERVICE_CONNECTION_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_CONNECTION;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICES;

public class Configuration {

    private static final Long DEFAULT_TIMEOUT = 5 * 60 * 1000L;
    private static final Long DEFAULT_POLL_INTERVAL = 5 * 1000L;
    private static final Long DEFAULT_SERVICE_CONNECTION_TIMEOUT = 10 * 1000L;

    /**
     * We often won't be able to connect to the services from the JUnit test case
     * unless the user explicitly knows its OK and allows it. (e.g. there may not be a network route)
     */
    private static final boolean DEFAULT_CONNECT_TO_SERVICES = false;

    private String masterUrl;
    private List<String> dependencies = new ArrayList<>();
    private URL configUrl;
    private long timeout = DEFAULT_TIMEOUT;
    private long pollInterval = DEFAULT_POLL_INTERVAL;
    private boolean ansiLoggerEnabled = true;
    private boolean connectToServices = DEFAULT_CONNECT_TO_SERVICES;

    private List<String> waitForServices = new ArrayList<>();
    private boolean waitForServiceConnection = false;
    private Long serviceConnectionTimeout = DEFAULT_SERVICE_CONNECTION_TIMEOUT;
    private Map<String, String> properties;

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public URL getConfigUrl() {
        return configUrl;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    public boolean isAnsiLoggerEnabled() {
        return ansiLoggerEnabled;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean isWaitForServiceConnection() {
        return waitForServiceConnection;
    }

    public List<String> getWaitForServices() {
        return waitForServices;
    }

    public int getServiceConnectionTimeout() {
        return serviceConnectionTimeout.intValue();
    }

    public boolean isConnectToServices() {
        return connectToServices;
    }

    public void setConnectToServices(boolean connectToServices) {
        this.connectToServices = connectToServices;
    }

    public static Configuration fromMap(Map<String, String> map) {
        Configuration configuration = new Configuration();
        try {
            applyMasterUrl(configuration, map);
            applyConfigurationURL(configuration, map);
            applyDependencies(configuration, map);

            configuration.timeout = getLongProperty(TIMEOUT, map, DEFAULT_TIMEOUT);
            configuration.pollInterval = getLongProperty(POLL_INTERVAL, map, DEFAULT_POLL_INTERVAL);
            configuration.ansiLoggerEnabled = getBooleanProperty(ANSI_LOGGER_ENABLED, map, true);
            configuration.waitForServiceConnection = getBooleanProperty(WAIT_FOR_SERVICE_CONNECTION, map, true);
            configuration.waitForServices = Strings.splitAndTrimAsList(getStringProperty(WAIT_FOR_SERVICES, map, ""), " ");
            configuration.connectToServices = getBooleanProperty(CONNECT_TO_SERVICES, map, DEFAULT_CONNECT_TO_SERVICES);
            configuration.serviceConnectionTimeout = getLongProperty(SERVICE_CONNECTION_TIMEOUT, map, DEFAULT_SERVICE_CONNECTION_TIMEOUT);
            configuration.properties = map;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return configuration;
    }

    /**
     * Applies the kubernetes master url to the configuration.
     * @param configuration The target configuration object.
     * @param map           The arquillian configuration.
     */
    private static void applyMasterUrl(Configuration configuration, Map<String, String> map) {
        if (map.containsKey(MASTER_URL)) {
            configuration.masterUrl = map.get(MASTER_URL);
        } else {
            configuration.masterUrl = Systems.getEnvVarOrSystemProperty(KUBERNETES_MASTER, "");
        }

        if (Strings.isNullOrBlank(configuration.getMasterUrl())) {
            throw new IllegalStateException("Could not find a valid kubernetes URL.");
        }
    }

    /**
     * Applies the kubernetes json url to the configuration.
     * @param configuration The target configuration object.
     * @param map           The arquillian configuration.
     */
    private static void applyConfigurationURL(Configuration configuration, Map<String, String> map) throws MalformedURLException {
        if (map.containsKey(CONFIG_URL)) {
            configuration.configUrl = new URL(map.get(CONFIG_URL));
        } else if (map.containsKey(CONFIG_FILE_NAME)) {
            configuration.configUrl = Configuration.class.getResource("/" + map.get(CONFIG_FILE_NAME));
        } else {
            configuration.configUrl = Configuration.class.getResource("/" + DEFAULT_CONFIG_FILE_NAME);
        }
    }

    /**
     * Applies the kubernetes json url to the configuration.
     * @param configuration The target configuration object.
     * @param map           The arquillian configuration.
     */
    private static void applyDependencies(Configuration configuration, Map<String, String> map) throws MalformedURLException {
        if (map.containsKey(DEPENDENCIES)) {
            configuration.dependencies = Strings.splitAndTrimAsList(map.get(DEPENDENCIES), " ");
        }
    }

    private static String getStringProperty(String name, Map<String, String> map, String defaultValue) {
        if (map.containsKey(name)) {
            return map.get(name);
        } else {
            return  Systems.getEnvVarOrSystemProperty(name, defaultValue);
        }
    }

    private static Boolean getBooleanProperty(String name, Map<String, String> map, Boolean defaultValue) {
        if (map.containsKey(name)) {
            return Boolean.parseBoolean(map.get(name));
        } else {
            return Systems.getEnvVarOrSystemProperty(name, defaultValue);
        }
    }

    private static Long getLongProperty(String name, Map<String, String> map, Long defaultValue) {
        if (map.containsKey(name)) {
            return Long.parseLong(map.get(name));
        } else {
            return  Systems.getEnvVarOrSystemProperty(name, defaultValue).longValue();
        }
    }
}

