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
package io.fabric8.service.child;

/**
 */
public class ChildConstants {
    /**
     * The PID for environment variables to inject into docker processes
     */
    public static final String ENVIRONMENT_VARIABLES_PID = "io.fabric8.environment";

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

}