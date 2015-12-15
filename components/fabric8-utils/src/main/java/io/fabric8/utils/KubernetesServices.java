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
package io.fabric8.utils;

import javax.management.ServiceNotFoundException;

/**
 * Some helper methods for working with kubernetes services environment
 * variables for service discovery
 */
public class KubernetesServices {
    public static final String DEFAULT_PROTO = "tcp";
    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";


    /**
     * Returns the String <code>host:port</code> for the Given the Kubernetes service name.
     *
     * If the service cannot be found then the default value is returned.
     */
    public static String serviceToHostAndPort(String serviceName, String portName, String defaultValue) {
        String host = serviceToHostOrBlank(serviceName);
        String port = serviceToPortOrBlank(serviceName, portName);
        if (Strings.isNullOrBlank(host) || Strings.isNullOrBlank(port)) {
            return defaultValue;
        } else {
            return host + ":" + port;
        }
    }

    /**
     * Returns the String <code>host:port</code> for the Given the Kubernetes service name.
     *
     * @throws ServiceNotFoundException if the service host could not be found
     */
    public static String serviceToHostAndPort(String serviceName, String portName) {
        String host = serviceToHost(serviceName);
        String port = serviceToPort(serviceName, portName);
        return host + ":" + port;
    }


    /**
     * Returns the service host name or a blank string if it could not be resolved
     */
    public static String serviceToHostOrBlank(String serviceName) {
        return Systems.getEnvVarOrSystemProperty(toServiceHostEnvironmentVariable(serviceName), "");
    }

    /**
     * Returns the host name for the given service name
     * @throws ServiceNotFoundException if the service host could not be found
     */
    public static String serviceToHost(String serviceName) {
        String hostEnvVar = toServiceHostEnvironmentVariable(serviceName);
        String answer = Systems.getEnvVarOrSystemProperty(hostEnvVar, "");
        if (Strings.isNullOrBlank(answer)) {
            throw new KubernetesServiceNotFoundException(serviceName, hostEnvVar);
        } else {
            return answer;
        }
    }

    /**
     * Returns the named port for the given service name
     * @throws ServiceNotFoundException if the service port could not be found
     */
    public static String serviceToPort(String serviceName, String portName) {
        String portEnvVar = toServicePortEnvironmentVariable(serviceName, portName);
        String answer = Systems.getEnvVarOrSystemProperty(portEnvVar, "");
        if (Strings.isNullOrBlank(answer)) {
            throw new KubernetesServiceNotFoundException(serviceName, portEnvVar);
        }
        return answer;
    }

    /**
     * Returns the default port for the given service name or blank
     */
    public static String serviceToPortOrBlank(String serviceName) {
        return serviceToPortOrBlank(serviceName, null);
    }

    /**
     * Returns the named port for the given service name or blank
     */
    public static String serviceToPortOrBlank(String serviceName, String portName) {
        String envVarName = toServicePortEnvironmentVariable(serviceName, portName);
        return Systems.getEnvVarOrSystemProperty(envVarName, "");
    }

    public static String serviceToProtocol(String serviceName, String servicePort) {
        return Systems.getEnvVarOrSystemProperty(toEnvVariable(serviceName + PORT_SUFFIX + "_" + servicePort + PROTO_SUFFIX), DEFAULT_PROTO);
    }

    /**
     * Returns the kubernetes environment variable name for the service host for the given service name
     */
    public static String toServiceHostEnvironmentVariable(String serviceName) {
        return toEnvVariable(serviceName + HOST_SUFFIX);
    }

    /**
     * Returns the kubernetes environment variable name for the service port for the given service and port name
     */
    public static String toServicePortEnvironmentVariable(String serviceName, String portName) {
        String name = serviceName + PORT_SUFFIX + (Strings.isNotBlank(portName) ? "_" + portName : "");
        return toEnvVariable(name);
    }

    public static String toEnvVariable(String serviceName) {
        return serviceName.toUpperCase().replaceAll("-", "_");
    }
}
