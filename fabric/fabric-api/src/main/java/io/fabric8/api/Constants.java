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
 * The Fabric Constants
 */
public interface Constants {

    String AGENT_PID = "io.fabric8.agent";
    String AGENT_PROPERTIES = AGENT_PID + Profile.PROPERTIES_SUFFIX;
    String DATASTORE_PID = "io.fabric8.datastore";
    String ZOOKEEPER_CLIENT_PID = "io.fabric8.zookeeper";
    String ZOOKEEPER_SERVER_PID = "io.fabric8.zookeeper.server";

    /**
     * The PID of the system properties used for the Java Container
     */
    String SYSTEM_PROPERTIES_PID = "io.fabric8.system";
    /**
     * The PID for the java container configuration,
     * such as the main class and the command line arguments.
     */
    String JAVA_CONTAINER_PID = "io.fabric8.container.java";
    /**
     * The PID for the process container configuration,
     * such as the distro and controller configuration
     */
    String PROCESS_CONTAINER_PID = "io.fabric8.container.process";
    /**
     * The PID for the process container overlay resources such as for downloading files via URLs
     * and storing them inside the process installation
     */
    String PROCESS_CONTAINER_OVERLAY_RESOURCES_PID = "io.fabric8.container.process.overlay.resources";
    /**
     * The variables PID used to define variables which can then be replaced inside templates such as MVEL.
     */
    String TEMPLATE_VARIABLES_PID = "io.fabric8.template.variables";
    /**
     * The PID for the list of ports and their default values
     */
    String PORTS_PID = "io.fabric8.ports";
    /**
     * The PID of the context paths; mapping of group/artifact id -> context path mappings
     */
    String WEB_CONTEXT_PATHS_PID = "io.fabric8.web.contextPath";
    /**
     * The Fabric8 management domain
     */
    String MANAGEMENT_DOMAIN = "io.fabric8";

    public static class Ports {
        public static final String JOLOKIA = "JOLOKIA";
    }
}
