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
package io.fabric8.api;

/**
 * Environment variables for working with fabric8
 */
public class EnvironmentVariables {
    /**
     * The PID for environment variables to inject into docker processes
     */
    public static final String ENVIRONMENT_VARIABLES_PID = "io.fabric8.environment";

    public static final String ZOOKEEPER_URL = "FABRIC8_ZOOKEEPER_URL";
    public static final String ZOOKEEPER_USER = "FABRIC8_ZOOKEEPER_USER";

    public static final String ZOOKEEPER_PASSWORD = "FABRIC8_ZOOKEEPER_PASSWORD";
    public static final String ZOOKEEPER_PASSWORD_RAW = "FABRIC8_ZOOKEEPER_PASSWORD_RAW";
    public static final String ZOOKEEPER_PASSWORD_ENCODE = "FABRIC8_ZOOKEEPER_PASSWORD_ENCODE";

    public static final String KARAF_NAME = "FABRIC8_KARAF_NAME";

    /**
     * The container name - the more standard long term replacement for {@link #KARAF_NAME}
     */
    public static final String CONTAINER_NAME = "FABRIC8_CONTAINER_NAME";

    public static final String FABRIC8_MANUALIP = "FABRIC8_MANUALIP";
    public static final String FABRIC8_GLOBAL_RESOLVER = "FABRIC8_GLOBAL_RESOLVER";
    public static final String FABRIC8_FABRIC_ENVIRONMENT = "FABRIC8_FABRIC_ENVIRONMENT";

    /**
     * The default address (host name or IP address) to listen on for network connections
     */
    public static final String FABRIC8_LISTEN_ADDRESS = "FABRIC8_LISTEN_ADDRESS";

    /**
     * Each container gets its own local IP address starting at 127.0.0.1, 127.0.0.2, 127.0.0.3
     * for use for example with local cassandra clusters
     */
    public static final String FABRIC8_LOCAL_CONTAINER_ADDRESS = "FABRIC8_LOCAL_CONTAINER_ADDRESS";

    /**
     * The Java debugger port to used to connect to a container's JVM
     */
    public static final String FABRIC8_JPDA_PROXY_PORT = "FABRIC8_JPDA_PROXY_PORT";


    /**
     * The URL of the jolokia URL for a process
     */
    public static final String FABRIC8_JOLOKIA_URL = "FABRIC8_JOLOKIA_URL";
}
