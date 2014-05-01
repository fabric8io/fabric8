/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.process;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Helper code to extract the Jolokia URL from the Java Agent settings
 */
public class JolokiaAgentHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaAgentHelper.class);
    private static ObjectMapper jolokiaMapper = new ObjectMapper();

    public static String findJolokiaUrlFromEnvironmentVariables(Map<String, String> environmentVariables, String defaultHost) {
        String javaAgent = getJavaAgent(environmentVariables);
        return findJolokiaUrlFromJavaAgent(javaAgent, defaultHost);
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

    /**
     * Updates the configuration and environment variables to reflect the new jolokia port
     */
    public static void updateJolokiaPort(JavaContainerConfig javaConfig, Map<String, String> environmentVariables, int jolokiaPort) {
        String javaAgent = javaConfig.getJavaAgent();
        if (Strings.isNotBlank(javaAgent)) {
            javaAgent = javaAgent.replace("${env:FABRIC8_JOLOKIA_PROXY_PORT}", "" + jolokiaPort);
        }
        javaConfig.setJavaAgent(javaAgent);
        javaConfig.updateEnvironmentVariables(environmentVariables);
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
                    Iterator<String> iter = value.getFieldNames();
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
