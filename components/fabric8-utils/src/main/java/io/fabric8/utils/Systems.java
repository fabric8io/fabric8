/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Systems {

    private static final transient Logger LOG = LoggerFactory.getLogger(Systems.class);

    public static String getSystemPropertyOrEnvVar(String systemProperty, String envVarName, String defaultValue) {
        String answer = null;
        try {
            answer = System.getProperty(systemProperty);
        } catch (Exception e) {
            LOG.warn("Failed to look up environment variable $" + envVarName + ". " + e, e);
        }
        if (Strings.isNullOrBlank(answer)) {
            answer = System.getenv(envVarName);
        }
        if (Strings.isNotBlank(answer)) {
            return answer;
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the value of the given environment variable or system property and if both are blank return the default value
     */
    public static String getEnvVarOrSystemProperty(String envVarName, String systemProperty, String defaultValue) {
        String answer = null;
        try {
            answer = System.getenv(envVarName);
        } catch (Exception e) {
            LOG.warn("Failed to look up environment variable $" + envVarName + ". " + e, e);
        }
        if (Strings.isNullOrBlank(answer)) {
            answer = System.getProperty(systemProperty, defaultValue);
        }
        if (Strings.isNotBlank(answer)) {
            return answer;
        } else {
            return defaultValue;
        }
    }

    public static boolean hasEnvVarOrSystemProperty(String envVarName) {
        return getEnvVarOrSystemProperty(envVarName) != null;
    }

    public static String getEnvVarOrSystemProperty(String envVarName, String defaultValue) {
        return getEnvVarOrSystemProperty(envVarName,envVarName,defaultValue);
    }

    public static String getEnvVarOrSystemProperty(String envVarName) {
        return getEnvVarOrSystemProperty(envVarName,envVarName,(String)null);
    }

    public static Number getEnvVarOrSystemProperty(final String name, final Number defaultValue) {
        String result =  getEnvVarOrSystemProperty(name, defaultValue.toString());
        return Integer.parseInt(result);
    }

    public static Boolean getEnvVarOrSystemProperty(final String name, final Boolean defaultValue) {
        String result =  getEnvVarOrSystemProperty(name, defaultValue.toString());
        return Boolean.parseBoolean(result);
    }


    /**
     * Returns the value of the given environment variable if its not blank or the given default value
     */
    public static String getEnvVar(String envVarName, String defaultValue) {
        String envVar = null;
        try {
            envVar = System.getenv(envVarName);
        } catch (Exception e) {
            LOG.warn("Failed to look up environment variable $" + envVarName + ". " + e, e);
        }
        if (Strings.isNotBlank(envVar)) {
            return envVar;
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the service host and port for the given environment variable name.
     *
     * @param serviceNameEnvVar the name of the environment variable used to configure the name of the service to connect to
     * @param defaultServiceName the default name of the service to use if the environment variable is not set
     * @param defaultHost the default host to use if not injected via an environment variable (e.g. localhost)
     * @parma defaultPort the default port to use to connect to the service if there is not an environment variable defined
     */
    public static String getServiceHostAndPort(String serviceNameEnvVar, String defaultServiceName, String defaultHost, String defaultPort) {
        String serviceName = Systems.getEnvVarOrSystemProperty(serviceNameEnvVar, serviceNameEnvVar, defaultServiceName);
        String serviceEnvVarPrefix = getServiceEnvVarPrefix(serviceName);
        String hostEnvVar = serviceEnvVarPrefix + "_HOST";
        String portEnvVar = serviceEnvVarPrefix + "_PORT";

        String host = Systems.getEnvVarOrSystemProperty(hostEnvVar, hostEnvVar, defaultHost);
        String port = Systems.getEnvVarOrSystemProperty(portEnvVar, portEnvVar, defaultPort);

        String answer = host + ":" + port;

        LOG.info("Connecting to service " + serviceName + " on " + answer
                + " from $" + hostEnvVar + " and $" + portEnvVar
                + ". To use a different service address please specify $" + serviceNameEnvVar + "=someServiceName where 'someServiceName' is the id of a service in Kubernetes");
        return answer;
    }


    /**
     * Returns the service host and port for the given environment variable name.
     *
     * @param serviceName the name of the service which is used as a prefix to access the <code>${serviceName}_SERVICE_HOST</code> and <code>${serviceName}_SERVICE_PORT</code> environment variables to find the hos and port
     * @param defaultHost the default host to use if not injected via an environment variable (e.g. localhost)
     * @parma defaultPort the default port to use to connect to the service if there is not an environment variable defined
     */
    public static String getServiceHostAndPort(String serviceName, String defaultHost, String defaultPort) {
        String serviceEnvVarPrefix = getServiceEnvVarPrefix(serviceName);
        String hostEnvVar = serviceEnvVarPrefix + "_HOST";
        String portEnvVar = serviceEnvVarPrefix + "_PORT";

        String host = Systems.getEnvVarOrSystemProperty(hostEnvVar, hostEnvVar, defaultHost);
        String port = Systems.getEnvVarOrSystemProperty(portEnvVar, portEnvVar, defaultPort);

        String answer = host + ":" + port;

        LOG.info("Connecting to service " + serviceName + " on " + answer
                + " from $" + hostEnvVar + " and $" + portEnvVar);
        return answer;
    }


    protected static String getServiceEnvVarPrefix(String serviceName) {
        return serviceName.toUpperCase().replace('-', '_') + "_SERVICE";
    }

}
