/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.arquillian.kubernetes;

import io.fabric8.kubernetes.client.internal.Utils;
import io.fabric8.utils.Strings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.fabric8.arquillian.kubernetes.Constants.ANSI_LOGGER_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_CONFIG_FILE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_KUBERNETES_MASTER;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_NAMESPACE_CLEANUP_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_NAMESPACE_CLEANUP_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_WAIT_FOR_SERVICE_CONNECTION_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_WAIT_FOR_SERVICE_CONNECTION_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_WAIT_POLL_INTERVAL;
import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_WAIT_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_CONFIG_RESOURCE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_CONFIG_URL;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_DEPENDENCIES;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_INIT_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.GOFABRIC8_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_DOMAIN;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_MASTER;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_CLEANUP_CONFIRM_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_CLEANUP_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_CLEANUP_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_LAZY_CREATE_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_TO_USE;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_CONNECTION_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_CONNECTION_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_LIST;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_POLL_INTERVAL;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_TIMEOUT;

public class Configuration {

    private String masterUrl;
    private List<String> environmentDependencies = new ArrayList<>();
    private URL environmentConfigUrl;

    private String namespaceToUse;
    private boolean namespaceLazyCreateEnabled = DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED;
    private boolean namespaceCleanupEnabled = DEFAULT_NAMESPACE_CLEANUP_ENABLED;
    private long namespaceCleanupTimeout = 10000L;
    private boolean namespaceCleanupConfirmationEnabled = false;

    private long waitTimeout = DEFAULT_WAIT_TIMEOUT;
    private long waitPollInterval = DEFAULT_WAIT_POLL_INTERVAL;
    private boolean waitForServiceConnectionEnabled = DEFAULT_WAIT_FOR_SERVICE_CONNECTION_ENABLED;
    private List<String> waitForServiceList = new ArrayList<>();
    private long waitForServiceConnectionTimeout = DEFAULT_WAIT_FOR_SERVICE_CONNECTION_TIMEOUT;

    private boolean ansiLoggerEnabled = true;
    private boolean environmentInitEnabled = true;
    private String kubernetesDomain;
    private Boolean gofabric8Enabled;
    private Map<String, String> properties;

    public Map<String, String> getProperties() {
        return properties;
    }

    public Boolean getGofabric8Enabled() {
        return gofabric8Enabled;
    }

    public String getKubernetesDomain() {
        return kubernetesDomain;
    }

    public boolean isEnvironmentInitEnabled() {
        return environmentInitEnabled;
    }

    public boolean isAnsiLoggerEnabled() {
        return ansiLoggerEnabled;
    }

    public long getWaitForServiceConnectionTimeout() {
        return waitForServiceConnectionTimeout;
    }

    public List<String> getWaitForServiceList() {
        return waitForServiceList;
    }

    public boolean isWaitForServiceConnectionEnabled() {
        return waitForServiceConnectionEnabled;
    }

    public long getWaitPollInterval() {
        return waitPollInterval;
    }

    public long getWaitTimeout() {
        return waitTimeout;
    }

    public boolean isNamespaceCleanupConfirmationEnabled() {
        return namespaceCleanupConfirmationEnabled;
    }

    public long getNamespaceCleanupTimeout() {
        return namespaceCleanupTimeout;
    }

    public boolean isNamespaceCleanupEnabled() {
        return namespaceCleanupEnabled;
    }

    public boolean isNamespaceLazyCreateEnabled() {
        return namespaceLazyCreateEnabled;
    }

    public String getNamespaceToUse() {
        return namespaceToUse;
    }

    public URL getEnvironmentConfigUrl() {
        return environmentConfigUrl;
    }

    public List<String> getEnvironmentDependencies() {
        return environmentDependencies;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public boolean isUseGoFabric8() {
        return gofabric8Enabled != null && gofabric8Enabled.booleanValue();
    }

    public static Configuration fromMap(Map<String, String> map) {
        Configuration configuration = new Configuration();
        try {
            configuration.masterUrl = getStringProperty(KUBERNETES_MASTER, map, DEFAULT_KUBERNETES_MASTER);
            configuration.environmentInitEnabled = getBooleanProperty(ENVIRONMENT_INIT_ENABLED, map, true);
            configuration.environmentConfigUrl = getKubernetesConfigurationUrl(map);
            configuration.environmentDependencies = Strings.splitAndTrimAsList(getStringProperty(ENVIRONMENT_DEPENDENCIES, map, ""), " ");

            configuration.namespaceLazyCreateEnabled = getBooleanProperty(NAMESPACE_LAZY_CREATE_ENABLED, map, DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED);
            configuration.namespaceToUse = getStringProperty(NAMESPACE_TO_USE, map, null);
            //We default to "cleanup=true" when generating namespace and "cleanup=false" when using existing namespace.
            configuration.namespaceCleanupEnabled = getBooleanProperty(NAMESPACE_CLEANUP_ENABLED, map, Strings.isNullOrBlank(configuration.namespaceToUse));
            configuration.namespaceCleanupConfirmationEnabled = getBooleanProperty(NAMESPACE_CLEANUP_CONFIRM_ENABLED, map, false);
            configuration.namespaceCleanupTimeout = getLongProperty(NAMESPACE_CLEANUP_TIMEOUT, map, DEFAULT_NAMESPACE_CLEANUP_TIMEOUT);

            configuration.waitTimeout = getLongProperty(WAIT_TIMEOUT, map, DEFAULT_WAIT_TIMEOUT);
            configuration.waitPollInterval = getLongProperty(WAIT_POLL_INTERVAL, map, DEFAULT_WAIT_POLL_INTERVAL);
            configuration.waitForServiceList = Strings.splitAndTrimAsList(getStringProperty(WAIT_FOR_SERVICE_LIST, map, ""), " ");
            configuration.waitForServiceConnectionEnabled = getBooleanProperty(WAIT_FOR_SERVICE_CONNECTION_ENABLED, map, DEFAULT_WAIT_FOR_SERVICE_CONNECTION_ENABLED);
            configuration.waitForServiceConnectionTimeout = getLongProperty(WAIT_FOR_SERVICE_CONNECTION_TIMEOUT, map, DEFAULT_NAMESPACE_CLEANUP_TIMEOUT);

            configuration.ansiLoggerEnabled = getBooleanProperty(ANSI_LOGGER_ENABLED, map, true);
            configuration.kubernetesDomain = getStringProperty(KUBERNETES_DOMAIN, map, "vagrant.f8");
            configuration.gofabric8Enabled = getBooleanProperty(GOFABRIC8_ENABLED, map, false);

            configuration.properties = map;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return configuration;
    }

    /**
     * Applies the kubernetes json url to the configuration.
     *
     * @param map The arquillian configuration.
     */
    private static URL getKubernetesConfigurationUrl(Map<String, String> map) throws MalformedURLException {
        if (map.containsKey(ENVIRONMENT_CONFIG_URL)) {
            return new URL(map.get(ENVIRONMENT_CONFIG_URL));
        } else if (map.containsKey(ENVIRONMENT_CONFIG_RESOURCE_NAME)) {
            String resourceName = map.get(ENVIRONMENT_CONFIG_RESOURCE_NAME);
            return resourceName.startsWith("/") ? Configuration.class.getResource(resourceName) : Configuration.class.getResource("/" + resourceName);
        } else if (Strings.isNotBlank(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_URL, ""))) {
            return new URL(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_URL, ""));
        } else {
            String resourceName = Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_RESOURCE_NAME, "/" + DEFAULT_CONFIG_FILE_NAME);
            return resourceName.startsWith("/") ? Configuration.class.getResource(resourceName) : Configuration.class.getResource("/" + resourceName);
        }
    }

    private static String getStringProperty(String name, Map<String, String> map, String defaultValue) {
        if (map.containsKey(name)) {
            return map.get(name);
        } else {
            return Utils.getSystemPropertyOrEnvVar(name, defaultValue);
        }
    }

    private static Boolean getBooleanProperty(String name, Map<String, String> map, Boolean defaultValue) {
        if (map.containsKey(name)) {
            return Boolean.parseBoolean(map.get(name));
        } else {
            return Utils.getSystemPropertyOrEnvVar(name, defaultValue);
        }
    }

    private static Long getLongProperty(String name, Map<String, String> map, Long defaultValue) {
        if (map.containsKey(name)) {
            return Long.parseLong(map.get(name));
        } else {
            return Long.parseLong(Utils.getSystemPropertyOrEnvVar(name, String.valueOf(defaultValue)));
        }
    }
}

