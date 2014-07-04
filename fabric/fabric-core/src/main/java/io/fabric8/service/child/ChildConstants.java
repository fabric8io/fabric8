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
package io.fabric8.service.child;

/**
 */
public class ChildConstants {

    /**
     * The PID for the java container configuration,
     * such as the main class and the command line arguments.
     */
    public static final String JAVA_CONTAINER_PID = "io.fabric8.container.java";

    /**
     * The PID for the process container configuration,
     * such as the distro and controller configuration
     */
    public static final String PROCESS_CONTAINER_PID = "io.fabric8.container.process";

    /**
     * The PID for the process container overlay resources such as for downloading files via URLs
     * and storing them inside the process installation
     */
    public static final String PROCESS_CONTAINER_OVERLAY_RESOURCES_PID = "io.fabric8.container.process.overlay.resources";

    /**
     * The variables PID used to define variables which can then be replaced inside templates such as MVEL.
     */
    public static final String TEMPLATE_VARIABLES_PID = "io.fabric8.template.variables";

    /**
     * The PID for the list of ports and their default values
     */
    public static final String PORTS_PID = "io.fabric8.ports";

    /**
     * The PID of the context paths; mapping of group/artifact id -> context path mappings
     */
    public static final String WEB_CONTEXT_PATHS_PID = "io.fabric8.web.contextPath";
}