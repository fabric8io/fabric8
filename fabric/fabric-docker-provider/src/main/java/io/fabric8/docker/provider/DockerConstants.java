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
package io.fabric8.docker.provider;

/**
 * Various constants for the docker container provider
 */
public class DockerConstants {
    public static final String SCHEME = "docker";
    public static final String DOCKER_PROVIDER_PID = "io.fabric8.docker.provider";

    /**
     * The PID for environment variables to inject into docker processes
     */
    public static final String ENVIRONMENT_VARIABLES_PID = "io.fabric8.environment";

    /**
     * The default docker image to create
     */
    public static final String DEFAULT_IMAGE = "fabric8/fabric8";

    /**
     * Config Admin property keys of the {@link #DOCKER_PROVIDER_PID}
     */
    public static class PROPERTIES {
        public static final String IMAGE = "image";
        public static final String CMD = "cmd";
    }

    /**
     * Environment variables
     */
    public static class ENV_VARS {
        public static final String ZOOKEEPER_URL = "FABRIC8_ZOOKEEPER_URL";
        public static final String ZOOKEEPER_PASSWORD = "FABRIC8_ZOOKEEPER_PASSWORD";

        public static final String KARAF_NAME = "FABRIC8_KARAF_NAME";
        public static final String ZOOKEEPER_PASSWORD_ENCODE = "FABRIC8_ZOOKEEPER_PASSWORD_ENCODE";
    }
}
