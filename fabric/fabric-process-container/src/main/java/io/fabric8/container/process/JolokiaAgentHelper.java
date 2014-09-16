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
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profiles;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.groovy.GroovyPlaceholderResolver;
import io.fabric8.internal.JsonHelper;
import io.fabric8.service.EnvPlaceholderResolver;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.InterpolationHelper;
import io.fabric8.zookeeper.utils.ZooKeeperMasterCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    /**
     * Updates the environment variables to pass along the system properties so they can be used by the Java Container
     */
    public static void updateSystemPropertiesEnvironmentVariable(Map<String, String> environmentVariables, FabricService fabricService, String versionId, Set<String> profileIds) {
        Map<String, String> systemProperties = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, Constants.SYSTEM_PROPERTIES_PID);
        updateSystemPropertiesEnvironmentVariable(environmentVariables, systemProperties);
    }

    /**
     * Updates the environment variables to pass along the system properties so they can be used by the Java Container
     */
    public static void updateSystemPropertiesEnvironmentVariable(Map<String, String> environmentVariables, Map<String, String> systemProperties) {
        if (systemProperties != null && systemProperties.size() > 0) {
            StringBuilder buffer = new StringBuilder();
            Set<Map.Entry<String, String>> entries = systemProperties.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String name = entry.getKey();
                String value = entry.getValue();
                if (buffer.length() > 0) {
                    buffer.append(" ");
                }
                buffer.append("-D");
                buffer.append(name);
                buffer.append("=");
                buffer.append(value);
            }
            environmentVariables.put(EnvironmentVariables.FABRIC8_SYSTEM_PROPERTIES, buffer.toString());
        }
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
     *
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
     *
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
     *
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
     * Replaces any ${env:NAME} expressions in the given map from the given environment variables.
     * <p/>
     * If preserveUnresolved is set to true, eventual tokens that are not found in the replacements map are kept; when the flag is set to false,
     * not matching tokens are replaced by an empty String.
     */
    public static void substituteEnvironmentVariableExpressions(Map<String, String> map, Map<String, String> environmentVariables, FabricService fabricService, final CuratorFramework curator, boolean preserveUnresolved) {
        for (String key : map.keySet()) {
            String text = map.get(key);
            String oldText = text;
            if (Strings.isNotBlank(text)) {
                text = substituteVariableExpression(text, environmentVariables, fabricService, curator, preserveUnresolved);
                if (!Objects.equal(oldText, text)) {
                    map.put(key, text);
                }
            }
        }
    }

    public static String substituteVariableExpression(String text, Map<String, String> environmentVariables, FabricService fabricService, final CuratorFramework curator, boolean preserveUnresolved) {
        String answer = text;
        if (environmentVariables != null && Strings.isNotBlank(answer)) {
            String envExprPrefix = "${env:";
            int startIdx = 0;
            while (true) {
                int idx = answer.indexOf(envExprPrefix, startIdx);
                if (idx < 0) {
                    break;
                }
                int startEndIdx = idx + envExprPrefix.length();
                int endIdx = answer.indexOf("}", startEndIdx);
                if (endIdx < 0) {
                    break;
                }
                String expression = answer.substring(startEndIdx, endIdx);
                String value = EnvPlaceholderResolver.resolveExpression(expression, environmentVariables, preserveUnresolved);
                if (!Objects.equal(expression, value)) {
                    answer = answer.substring(0, idx) + value + answer.substring(endIdx + 1);
                } else {
                    // ignore this expression
                    startIdx++;
                }
            }
        }
        if (Strings.isNullOrBlank(answer)) {
            return answer;
        }
        String zkUser = null;
        String zkPassword = null;
        if (fabricService != null) {
            zkUser = fabricService.getZooKeeperUser();
            zkPassword = fabricService.getZookeeperPassword();
        }
        if (Strings.isNotBlank(zkUser)) {
            answer = answer.replace("${zookeeper.user}", zkUser);
        }
        if (Strings.isNotBlank(zkPassword)) {
            answer = answer.replace("${zookeeper.password}", zkPassword);
        }
        if (curator != null) {
            InterpolationHelper.SubstitutionCallback substitutionCallback = new InterpolationHelper.SubstitutionCallback() {
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
            };
            // replace Groovy / ZooKeeper expressions
            if (preserveUnresolved) {
                answer = InterpolationHelper.substVarsPreserveUnresolved(answer, "dummy", null, Collections.EMPTY_MAP, substitutionCallback);
            } else {
                answer = InterpolationHelper.substVars(answer, "dummy", null, Collections.EMPTY_MAP, substitutionCallback);
            }
        }
        return answer;
    }

    /**
     * Replaces any ${env:NAME} expressions in the given map from the given environment variables
     */
    public static void substituteEnvironmentVariableExpressions(Map<String, String> map, Map<String, String> environmentVariables, FabricService fabricService, final CuratorFramework curator) {
        substituteEnvironmentVariableExpressions(map, environmentVariables, fabricService, curator, false);
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
     *
     * @param overrides
     * @return
     */
    private static Map<String, EnvironmentVariableOverride> getStringEnvironmentVariableOverrideMap(EnvironmentVariableOverride... overrides) {
        Map<String, EnvironmentVariableOverride> overridesMap = new HashMap<String, EnvironmentVariableOverride>();
        for (EnvironmentVariableOverride override : overrides) {
            overridesMap.put(override.getKey(), override);
        }
        return overridesMap;
    }

    /**
     * Helper to update the java main class arguments
     *
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
     *
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
     *
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
    public static void jolokiaKeepAliveCheck(ZooKeeperMasterCache zkMasterCache, FabricService fabric, String jolokiaUrl, String containerName) {
        Container container = null;
        try {
            container = fabric.getContainer(containerName);
        } catch (Exception e) {
            // ignore
        }
        jolokiaKeepAliveCheck(zkMasterCache, fabric, jolokiaUrl, container);
    }

    public static void jolokiaKeepAliveCheck(ZooKeeperMasterCache zkMasterCache, FabricService fabric, String jolokiaUrl, Container container) {
        if (container != null) {
            if (!Objects.equal(jolokiaUrl, container.getJolokiaUrl())) {
                container.setJolokiaUrl(jolokiaUrl);
            }
            jolokiaKeepAliveCheck(zkMasterCache, fabric, container, null);
        }
    }

    /**
     * Checks the container is still alive and updates its provision list if its changed
     */
    public static List<String> jolokiaKeepAliveCheck(ZooKeeperMasterCache zkMasterCache, FabricService fabric, Container container, Map<String, String> envVars) {
        List<String> newZkContainerPaths = new ArrayList<>();
        String jolokiaUrl = container.getJolokiaUrl();
        if (Strings.isNullOrBlank(jolokiaUrl)) {
            return newZkContainerPaths;
        }

        String containerName = container.getId();
        boolean debugLog = LOG.isDebugEnabled();
        if (debugLog) {
            LOG.debug("Performing keep alive jolokia check on " + containerName + " URL: " + jolokiaUrl);
        }

        String user = fabric.getZooKeeperUser();
        String password = fabric.getZookeeperPassword();
        String url = jolokiaUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        String listUrl = url + "list/?maxDepth=1";
        List<String> jmxDomains = new ArrayList<String>();
        boolean valid = false;
        try {
            URL theUrl = new URL(listUrl);
            URLConnection urlConnection = theUrl.openConnection();
            String authString = user + ":" + password;
            String authStringEnc = Base64Encoder.encode(authString);
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            InputStream is = urlConnection.getInputStream();
            JsonNode jsonNode = jolokiaMapper.readTree(is);
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
            LOG.warn("Failed to query: " + listUrl + ". " + e, e);
        }

        String provisionResult = container.getProvisionResult();
        if (debugLog) {
            LOG.debug("Current provision result: " + provisionResult + " valid: " + valid);
        }
        valid = valid && performExtraJolokiaChecks(zkMasterCache, fabric, container, jmxDomains, url, envVars, newZkContainerPaths);
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
        return newZkContainerPaths;
    }

    /**
     * Based on the container's configuration we may decide to perform extra additional checks
     * for things inside the JVM; such as checking if any web applications or CXF endpoints are created and
     * registering them automatically into the ZK registry.
     *
     * @return true if the container is deemed to still be valid after performing the checks
     */
    protected static boolean performExtraJolokiaChecks(ZooKeeperMasterCache zkMasterCache, FabricService fabric, Container container, List<String> jmxDomains, String url, Map<String, String> envVars, List<String> newZkContainerPaths) {
        if (zkMasterCache != null) {
            for (String jmxDomain : jmxDomains) {
                // check for tomcat web contexts
                if (jmxDomain.startsWith("Catalina") || jmxDomain.startsWith("Tomcat")) {
                    // get get the port from the ports...
                    String tomcatUrl = url + "?maxDepth=6&maxCollectionSize=500&ignoreErrors=true&canonicalNaming=false";
                    String json = "{\"type\":\"read\",\"mbean\":\"*:j2eeType=WebModule,*\",\"attribute\":[\"displayName\",\"path\",\"stateName\",\"startTime\"]}";

                    JsonNode jsonNode = postJson(tomcatUrl, json);
                    if (jsonNode != null) {
                        List<JsonNode> values = jsonNode.findValues("value");
                        for (JsonNode value : values) {
                            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> next = fields.next();
                                JsonNode node = next.getValue();
                                Object startTime = getValue(node, "startTime");
                                Object stateName = getValue(node, "stateName");
                                Object pathObject = getValue(node, "path");
                                String path = pathObject != null ? pathObject.toString() : "/";
                                if (path.length() == 0) {
                                    path = "/";
                                }
                                Object displayName = getValue(node, "displayName");
                                updateZookeeperEntry(zkMasterCache, fabric, container, envVars, path, stateName, startTime, displayName, newZkContainerPaths);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    protected static void updateZookeeperEntry(ZooKeeperMasterCache zkMasterCache, FabricService fabric, Container container, Map<String, String> envVars, String path, Object stateName, Object startTime, Object displayName, List<String> newZkContainerPaths) {
        String matchedZkPath = null;
        Map<String, String> configuration = container.getOverlayProfile().getConfiguration(Constants.WEB_CONTEXT_PATHS_PID);
        if (configuration != null) {
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                if (entry.getValue().equals(path)) {
                    matchedZkPath = entry.getKey();
                    break;
                }
            }
        }
        if (matchedZkPath != null) {
            // lets combine it with the container id and make the ZK entry....
            String zkPath = ZkPath.WEBAPPS_CLUSTER.getPath(matchedZkPath);
            if (isWebAppActive(stateName)) {
                // lets write a new ZK entry...
                String id = container.getId();
                String httpUrl = container.getHttpUrl();
                // TODO we should update this so its always set?
                // lets try calculate it
                if (Strings.isNullOrBlank(httpUrl)) {
                    if (envVars != null) {
                        String portText = envVars.get("FABRIC8_HTTP_PROXY_PORT");
                        if (Strings.isNotBlank(portText)) {
                            String ip = container.getIp();
                            if (Strings.isNotBlank(ip)) {
                                httpUrl = "http://" + ip + ":" + portText;
                            }
                        }
                    }
                    if (httpUrl == null) {
                        httpUrl = "";
                    }
                }
                String url = httpUrl + path;
                String json = "{\"id\":" + JsonHelper.jsonEncodeString(id)
                        + ", \"container\":" + JsonHelper.jsonEncodeString(id)
                        + ", \"services\":[" + JsonHelper.jsonEncodeString(url) + "]"
                        + "}";

                try {
                    zkMasterCache.setStringData(zkPath, json, CreateMode.EPHEMERAL);
                    newZkContainerPaths.add(zkPath);
                } catch (Exception e) {
                    LOG.warn("Failed to register web app json at path " + path + " json: " + json + ". " + e, e);
                }
            } else {
                // lets delete the ZK entry if it exists
                try {
                    zkMasterCache.deleteData(zkPath);
                } catch (Exception e) {
                    LOG.warn("Failed to remove web app json at path " + path + ". " + e, e);
                }
            }
        }
    }

    protected static boolean isWebAppActive(Object stateName) {
        if (stateName != null) {
            String text = stateName.toString().toLowerCase();
            return text.contains("start");
        }
        return false;
    }

    /**
     * A Helper method to get the value from the JsonNode as a primitive type value
     */
    protected static Object getValue(JsonNode node, String key) {
        JsonNode jsonNode = node.get(key);
        if (jsonNode != null) {
            if (jsonNode.isTextual()) {
                return jsonNode.textValue();
            } else if (jsonNode.isLong()) {
                return jsonNode.asLong();
            }
        }
        return null;
    }


    /**
     * Posts a blob of JSON to a URL and returns the JSON object
     */
    protected static JsonNode postJson(String url, String json) {
        try {
            URL theUrl = new URL(url);
            URLConnection connection = theUrl.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setConnectTimeout(50000);
            connection.setReadTimeout(5000);
            connection.connect();
            OutputStream os = connection.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write(json);
            writer.close();
            if (connection instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) connection).getResponseCode();
                if (code < 200 || code >= 300) {
                    LOG.warn("Got a " + code + " when posting to URL " + url + " with JSON: " + json);
                } else {
                    return jolokiaMapper.readTree(connection.getInputStream());
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed ot post to " + url + " with JSON: " + json + ". " + e, e);
        }
        return null;
    }
}
