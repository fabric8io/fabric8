/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.container.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.api.Container;
import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.FabricService;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.groovy.GroovyPlaceholderResolver;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.InterpolationHelper;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Helper code to extract the Jolokia URL from the Java Agent settings
 */
public class JolokiaAgentHelper {
    public static final int DEFAULT_JOLOKIA_PORT = 8778;
    public static final String JOLOKIA_PORTS_PID = "io.fabric8.jolokia";

    /**
     * The name of the Jolokia port in the configuration PID io.fabric8.ports
     */
    public static final String JOLOKIA_PORT_NAME = "JOLOKIA";

    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaAgentHelper.class);
    private static ObjectMapper jolokiaMapper = new ObjectMapper();

    public static String findJolokiaUrlFromEnvironmentVariables(Map<String, String> environmentVariables, String defaultHost) {
        String javaAgent = getJavaAgent(environmentVariables);
        String answer = findJolokiaUrlFromJavaAgent(javaAgent, defaultHost);
        if (Strings.isNullOrBlank(answer)) {
            answer = environmentVariables.get(EnvironmentVariables.FABRIC8_JOLOKIA_URL);
        }
        return answer;
    }

    public static String getJavaAgent(Map<String, String> environmentVariables) {
        return environmentVariables.get(JavaContainerEnvironmentVariables.FABRIC8_JAVA_AGENT);
    }

    /**
     * Returns true if the java agent environment variable contains jolokia
     */
    public static boolean hasJolokiaAgent(String javaAgent) {
        return Strings.isNotBlank(javaAgent) && javaAgent.contains("jolokia");
    }

    /**
     * Returns true if the java agent environment variable contains jolokia
     */
    public static boolean hasJolokiaAgent(Map<String, String> environmentVariables) {
        String javaAgent = getJavaAgent(environmentVariables);
        return hasJolokiaAgent(javaAgent);
    }


    public static String findJolokiaUrlFromJavaAgent(String javaAgent, String defaultHost) {
        if (hasJolokiaAgent(javaAgent)) {
            Properties properties = new Properties();
            String propertyText = javaAgent.trim();
            while (propertyText.endsWith("\"") || propertyText.endsWith("\'")) {
                propertyText = propertyText.substring(0, propertyText.length() - 1);
            }
            int start = javaAgent.indexOf('=');
            if (start >= 0) {
                propertyText = propertyText.substring(start + 1);
                String[] valueExpressions = propertyText.split(",");
                if (valueExpressions != null) {
                    for (String expression : valueExpressions) {
                        String[] keyValue = expression.split("=");
                        if (keyValue != null && keyValue.length > 1) {
                            properties.put(keyValue[0], keyValue[1]);
                        }
                    }
                }
            }
            String port = properties.getProperty("port", "8778");
            String host = properties.getProperty("host", "0.0.0.0");
            if (host.equals("0.0.0.0")) {
                host = defaultHost;
            }
            return "http://" + host + ":" + port + "/jolokia/";
        }
        return null;
    }


    public interface EnvironmentVariableOverride {
        public String getKey();
        public String getValue(String originalValue);
    }

    public interface UpdateAction {
        public String go(String javaAgent);
    }

    /**
     * Returns an environment variable override to update the container's advertised Jolokia port
     * @param jolokiaPort
     * @return
     */
    public static EnvironmentVariableOverride getJolokiaPortOverride(final int jolokiaPort) {
        return new EnvironmentVariableOverride() {
            public String getKey() {
                return "FABRIC8_JOLOKIA_PROXY_PORT";
            }

            public String getValue(String originalValue) {
                return "" + jolokiaPort;
            }
        };
    }

    /**
     * Returns an environment variable override to update the container's advertised Jolokia agentId
     * @param prefix
     * @return
     */
    public static EnvironmentVariableOverride getJolokiaAgentIdOverride(final String prefix) {
        return new EnvironmentVariableOverride() {
            public String getKey() {
                return "FABRIC8_KARAF_NAME";
            }

            public String getValue(String originalValue) {
                if (Strings.isNullOrBlank(prefix)) {
                    return originalValue;
                } else {
                    return prefix + "--" + originalValue;
                }
            }
        };
    }


    /**
     * Substitutes environment variables for the javaAgent, jvmArguments and arguments settings
     * @param javaConfig
     * @param environmentVariables
     * @param isJavaContainer
     * @param overrides
     */
    public static void substituteEnvironmentVariables(JavaContainerConfig javaConfig, final Map<String, String> environmentVariables, boolean isJavaContainer, EnvironmentVariableOverride... overrides) {

        UpdateAction action = substituteEnvironmentVariablesOnly(environmentVariables, overrides);
        updateJavaAgent(javaConfig, environmentVariables, isJavaContainer, action);
        updateArguments(javaConfig, environmentVariables, isJavaContainer, action);
        updateJvmArguments(javaConfig, environmentVariables, isJavaContainer, action);
    }

    public static UpdateAction substituteEnvironmentVariablesOnly(final Map<String, String> environmentVariables, EnvironmentVariableOverride... overrides) {
        final Map<String, EnvironmentVariableOverride> overridesMap = getStringEnvironmentVariableOverrideMap(overrides);
        final Map<String, EnvironmentVariableOverride> used = new HashMap<String, EnvironmentVariableOverride>();

        return new UpdateAction() {
            public String go(String string) {
                String answer = string;
                for (String key : environmentVariables.keySet()) {
                    String value = environmentVariables.get(key);
                    if (overridesMap.containsKey(key)) {
                        EnvironmentVariableOverride override = overridesMap.remove(key);
                        value = override.getValue(value);
                        used.put(key, override);
                    }
                    answer = answer.replace("${env:" + key + "}", value);
                }
                // handle any overrides that weren't in the environment map too
                for (String key : overridesMap.keySet()) {
                    answer = answer.replace("${env:" + key + "}", overridesMap.get(key).getValue(null));
                }
                for (String key : used.keySet()) {
                    overridesMap.put(key, used.get(key));
                }
                return answer;
            }
        };
    }


    /**
     * Replaces any ${env:NAME} expressions in the given map from the given environment variables
     */
    public static void substituteEnvironmentVariableExpressions(Map<String, String> map, Map<String, String> environmentVariables, FabricService fabricService, final CuratorFramework curator) {
        String zkUser = null;
        String zkPassword = null;
        if (fabricService != null) {
            zkUser = fabricService.getZooKeeperUser();
            zkPassword = fabricService.getZookeeperPassword();
        }

        Set<Map.Entry<String, String>> envEntries = environmentVariables.entrySet();
        for (String key : map.keySet()) {
            String text = map.get(key);
            String oldText = text;
            if (Strings.isNotBlank(text)) {
                for (Map.Entry<String, String> envEntry : envEntries) {
                    String envKey = envEntry.getKey();
                    String envValue = envEntry.getValue();
                    if (Strings.isNotBlank(envKey) && Strings.isNotBlank(envValue)) {
                        text = text.replace("${env:" + envKey + "}", envValue);
                    }
                }
                if (Strings.isNotBlank(zkUser)) {
                    text = text.replace("${zookeeper.user}", zkUser);
                }
                if (Strings.isNotBlank(zkPassword)) {
                    text = text.replace("${zookeeper.password}", zkPassword);
                }
                if (curator != null) {
                    // replace Groovy / ZooKeeper expressions
                    text = InterpolationHelper.substVars(text, "dummy", null, Collections.EMPTY_MAP,  new InterpolationHelper.SubstitutionCallback() {
                        @Override
                        public String getValue(String key) {
                            if (key.startsWith("zk:")) {
                                try {
                                    return new String(ZkPath.loadURL(curator, key), "UTF-8");
                                } catch (Exception e) {
                                    //ignore and just return null.
                                }
                            } else if (key.startsWith("groovy:")) {
                                try {
                                    return GroovyPlaceholderResolver.resolveValue(curator, key);
                                } catch (Exception e) {
                                    //ignore and just return null.
                                }
                            }
                            return null;
                        }
                    });                }
                if (!Objects.equal(oldText, text)) {
                    map.put(key, text);
                }
            }
        }
    }

    /**
     * Replaces any ${env:NAME} expressions in the given map keys from the given environment variables
     */
    public static Map<String, String> substituteEnvironmentVariableExpressionKeysAndValues(Map<String, String> map, Map<String, String> environmentVariables) {
        Map<String, String> answer = new HashMap<String, String>();
        Set<Map.Entry<String, String>> envEntries = environmentVariables.entrySet();
        for (String key : map.keySet()) {
            String text = key;
            if (Strings.isNotBlank(text)) {
                for (Map.Entry<String, String> envEntry : envEntries) {
                    String envKey = envEntry.getKey();
                    String envValue = envEntry.getValue();
                    if (Strings.isNotBlank(envKey) && Strings.isNotBlank(envValue)) {
                        text = text.replace("${env:" + envKey + "}", envValue);
                    }
                }
                answer.put(text, map.get(key));
            }
        }
        substituteEnvironmentVariableExpressions(answer, environmentVariables, null, null);
        return answer;
    }
    

    /**
     * Helper to convert an array of overrides into a map for quicker lookup of overrides for environment variables
     * @param overrides
     * @return
     */
    private static Map<String, EnvironmentVariableOverride> getStringEnvironmentVariableOverrideMap(EnvironmentVariableOverride ... overrides) {
        Map<String, EnvironmentVariableOverride> overridesMap = new HashMap<String, EnvironmentVariableOverride>();
        for (EnvironmentVariableOverride override : overrides) {
            overridesMap.put(override.getKey(), override);
        }
        return overridesMap;
    }

    /**
     * Helper to update the java main class arguments
     * @param javaConfig
     * @param environmentVariables
     * @param isJavaContainer
     * @param action
     */
    private static void updateArguments(JavaContainerConfig javaConfig, Map<String, String> environmentVariables, boolean isJavaContainer, UpdateAction action) {
        String arguments = javaConfig.getArguments();
        if (Strings.isNotBlank(arguments)) {
            arguments = action.go(arguments);
        }
        javaConfig.setArguments(arguments);
        javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);
    }

    /**
     * Helper to update the JVM arguments
     * @param javaConfig
     * @param environmentVariables
     * @param isJavaContainer
     * @param action
     */
    private static void updateJvmArguments(JavaContainerConfig javaConfig, Map<String, String> environmentVariables, boolean isJavaContainer, UpdateAction action) {
        String jvmArguments = javaConfig.getJvmArguments();
        if (Strings.isNotBlank(jvmArguments)) {
            jvmArguments = action.go(jvmArguments);
        }
        javaConfig.setJvmArguments(jvmArguments);
        javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);
    }

    /**
     * Helper to update the java agent argument for the container
     * @param javaConfig
     * @param environmentVariables
     * @param isJavaContainer
     * @param action
     */
    private static void updateJavaAgent(JavaContainerConfig javaConfig, Map<String, String> environmentVariables, boolean isJavaContainer, UpdateAction action) {
        String javaAgent = javaConfig.getJavaAgent();
        if (Strings.isNotBlank(javaAgent)) {
            javaAgent = action.go(javaAgent);
        }
        javaConfig.setJavaAgent(javaAgent);
        javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);
    }

    /**
     * Checks the container is still alive and updates its provision list if its changed
     */
    public static void jolokiaKeepAliveCheck(FabricService fabric, String jolokiaUrl, String containerName) {
        Container container = null;
        try {
            container = fabric.getContainer(containerName);
        } catch (Exception e) {
            // ignore
        }
        if (container != null) {
            if (!Objects.equal(jolokiaUrl, container.getJolokiaUrl())) {
                container.setJolokiaUrl(jolokiaUrl);
            }
            jolokiaKeepAliveCheck(fabric, container);
        }
    }

    /**
     * Checks the container is still alive and updates its provision list if its changed
     */
    public static void jolokiaKeepAliveCheck(FabricService fabric, Container container) {
        String jolokiaUrl = container.getJolokiaUrl();
        if (Strings.isNullOrBlank(jolokiaUrl)) {
            return;
        }

        String containerName = container.getId();
        boolean debugLog = LOG.isDebugEnabled();
        if (debugLog) {
            LOG.debug("Performing keep alive jolokia check on " + containerName + " URL: " + jolokiaUrl);
        }

        String user = fabric.getZooKeeperUser();
        String password = fabric.getZookeeperPassword();
        String url = jolokiaUrl;
        int idx = jolokiaUrl.indexOf("://");
        if (idx > 0) {
            url = "http://" + user + ":" + password + "@" + jolokiaUrl.substring(idx + 3);
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "list/?maxDepth=1";

        List<String> jmxDomains = new ArrayList<String>();
        boolean valid = false;
        try {
            URL theUrl = new URL(url);
            JsonNode jsonNode = jolokiaMapper.readTree(theUrl);
            if (jsonNode != null) {
                JsonNode value = jsonNode.get("value");
                if (value != null) {
                    Iterator<String> iter = value.fieldNames();
                    while (iter.hasNext()) {
                        jmxDomains.add(iter.next());
                    }
                    if (debugLog) {
                        LOG.debug("Container " + containerName + " has JMX Domains: " + jmxDomains);
                    }
                    valid = jmxDomains.size() > 0;
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to query: " + url + ". " + e, e);
        }

        String provisionResult = container.getProvisionResult();
        if (debugLog) {
            LOG.debug("Current provision result: " + provisionResult + " valid: " + valid);
        }
        if (valid) {
            if (!Objects.equal(Container.PROVISION_SUCCESS, provisionResult) || !container.isAlive()) {
                container.setProvisionResult(Container.PROVISION_SUCCESS);
                container.setProvisionException(null);
                container.setAlive(true);
                JavaContainers.registerJolokiaUrl(container, jolokiaUrl);
            }
            if (!Objects.equal(jmxDomains, container.getJmxDomains())) {
                container.setJmxDomains(jmxDomains);
            }
        } else {
            if (container.isAlive()) {
                container.setAlive(true);
            }
            if (!Objects.equal(Container.PROVISION_FAILED, provisionResult)) {
                container.setProvisionResult(Container.PROVISION_FAILED);
            }
        }
    }

}
