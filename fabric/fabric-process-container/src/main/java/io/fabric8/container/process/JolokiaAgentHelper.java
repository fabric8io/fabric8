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

import io.fabric8.common.util.Strings;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;

import java.util.Map;
import java.util.Properties;

/**
 * Helper code to extract the Jolokia URL from the Java Agent settings
 */
public class JolokiaAgentHelper {

    public static String findJolokiaUrlFromEnvironmentVariables(Map<String, String> environmentVariables, String defaultHost) {
        String javaAgent = environmentVariables.get(JavaContainerEnvironmentVariables.FABRIC8_JAVA_AGENT);
        return findJolokiaUrlFromJavaAgent(javaAgent, defaultHost);
    }

    public static String findJolokiaUrlFromJavaAgent(String javaAgent, String defaultHost) {
        if (Strings.isNotBlank(javaAgent) && javaAgent.contains("jolokia")) {
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
}
