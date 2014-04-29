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
    public static final String CONTAINER_TYPE_PID = "io.fabric8.runtime.container";

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


    /**
     * Config Admin property keys of the {@link #CONTAINER_TYPE_PID}
     */
    public static class PROPERTIES {
        public static final String CONTAINER_TYPE = "image";
        public static final String JAVA_LIBRARY_PATH = "javaLibraryPath";
    }

    /**
     * Environment variables
     */
    public static class ENV_VARS {
        public static final String ZOOKEEPER_URL = "FABRIC8_ZOOKEEPER_URL";
        public static final String ZOOKEEPER_PASSWORD = "FABRIC8_ZOOKEEPER_PASSWORD";
        public static final String ZOOKEEPER_PASSWORD_ENCODE = "FABRIC8_ZOOKEEPER_PASSWORD_ENCODE";

        public static final String KARAF_NAME = "FABRIC8_KARAF_NAME";

        public static final String FABRIC8_MANUALIP = "FABRIC8_MANUALIP";
        public static final String FABRIC8_GLOBAL_RESOLVER = "FABRIC8_GLOBAL_RESOLVER";
        public static final String FABRIC8_FABRIC_ENVIRONMENT = "FABRIC8_FABRIC_ENVIRONMENT";
    }

    /**
     * Environment variables for the Java Container
     */
    public static class JAVA_CONTAINER_ENV_VARS {
        /**
         * The java command line agent options
         */
        public static final String FABRIC8_JAVA_AGENT = "FABRIC8_JAVA_AGENT";

        /**
         * The java command line JVM arguments
         */
        public static final String FABRIC8_JVM_ARGS = "FABRIC8_JVM_ARGS";

        /**
         * The name of the main Java class to execute
         */
        public static final String FABRIC8_JAVA_MAIN = "FABRIC8_JAVA_MAIN";

        /**
         * The command line arguments to the Java main
         */
        public static final String FABRIC8_MAIN_ARGS = "FABRIC8_MAIN_ARGS";

        public static final String[] ALL_ENV_VARS = {
                FABRIC8_JAVA_AGENT, FABRIC8_JVM_ARGS, FABRIC8_JAVA_MAIN, FABRIC8_MAIN_ARGS
        };
    }

}