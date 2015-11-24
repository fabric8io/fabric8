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
package io.fabric8.mq.core;

import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for working with Fabric8 MQ and Apache ActiveMQ inside Kubernetes
 */
public class MQs {
    private static final transient Logger LOG = LoggerFactory.getLogger(MQs.class);

    public static final String SERVICE_NAME_ENV_VAR = "A_MQ_SERVICE_NAME";
    public static final String DEFAULT_SERVICE_NAME = "broker";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_PORT = "61616";

    /**
     * Returns the brokerURL to connect to the Apache ActiveMQ broker using the given Kubernetes service name and an optional parametes string
     */
    public static String getBrokerURL(String serviceName, String parameters) {
        if (parameters == null) {
            parameters = "";
        }
        if (Strings.isNullOrBlank(serviceName)) {
            serviceName = Systems.getEnvVarOrSystemProperty(MQs.SERVICE_NAME_ENV_VAR, MQs.SERVICE_NAME_ENV_VAR, MQs.DEFAULT_SERVICE_NAME);
        }
        String serviceEnvVarPrefix = getServiceEnvVarPrefix(serviceName);
        String hostEnvVar = serviceEnvVarPrefix + "_HOST";
        String portEnvVar = serviceEnvVarPrefix + "_PORT";

        String host = Systems.getEnvVarOrSystemProperty(hostEnvVar, hostEnvVar, DEFAULT_HOST);
        String port = Systems.getEnvVarOrSystemProperty(portEnvVar, portEnvVar, DEFAULT_PORT);

        LOG.info("Connecting to ActiveMQ broker on " + host + ":" + port
                + " from $" + hostEnvVar + " and $" + portEnvVar
                + ". To use a different broker service please specify $" + SERVICE_NAME_ENV_VAR + "=someBrokerServiceName where 'someBrokerServiceName' is a defined ActiveMQ broker service in Kubernetes");
        String answer = "failover:(tcp://" + host + ":" + port + ")" + parameters;
        LOG.info("BrokerURL is: " + answer);
        return answer;
    }

    protected static String getServiceEnvVarPrefix(String serviceName) {
        return serviceName.toUpperCase() + "_SERVICE";
    }
}
