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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various constants for the docker container provider
 */
public class DockerConstants {
    private static final transient Logger LOG = LoggerFactory.getLogger(DockerConstants.class);

    public static final String SCHEME = "docker";
    public static final String DOCKER_PROVIDER_PID = "io.fabric8.docker.provider";

    /**
     * The PID for environment variables to inject into docker processes
     */
    public static final String ENVIRONMENT_VARIABLES_PID = "io.fabric8.environment";

    /**
     * The PID for the list of ports and their default values
     */
    public static final String PORTS_PID = "io.fabric8.ports";

    /**
     * The default docker image to create
     */
    public static final String DEFAULT_IMAGE = "fabric8/fabric8";

    /**
     * The start of the ports created to forward from the host to the internal docker port
     */
    public static final int DEFAULT_EXTERNAL_PORT = 49000;

    /**
     * Should we enable SSHD for debugging in the fabric8 docker container image?
     *
     * Ideally we'd maybe auto-detect it from the container image?
     */
    public static final boolean ENABLE_SSHD = isEnvVarEnabled(ENV_VARS.FABRIC8_DOCKER_SSHD);

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

        public static final String FABRIC8_MANUALIP = "FABRIC8_MANUALIP";
        public static final String FABRIC8_GLOBAL_RESOLVER = "FABRIC8_GLOBAL_RESOLVER";
        public static final String FABRIC8_FABRIC_ENVIRONMENT = "FABRIC8_FABRIC_ENVIRONMENT";


        /**
         * Environment variable to define the default container image to be used if none is explicitly configured
         * in the profile being created.
         *
         * This is the easiest way to change the docker container image; for example if you wish to use a local build
         * of your own docker container changes.
         *
         * If you have a local build of the fabric8-sshd container image then set this variable to "fabric8:fabric8-sshd" to enable
         * sshd enabled fabric8 cartridges
         */
        public static final String FABRIC8_DOCKER_DEFAULT_IMAGE = "FABRIC8_DOCKER_DEFAULT_IMAGE";

        /**
         * Debug env var to flag that sshd should be enabled for a cartridge
         */
        public static final String FABRIC8_DOCKER_SSHD = "FABRIC8_DOCKER_SSHD";
    }


    /**
     * Returns true if the given environment variable is "true" or "yes"
     */
    protected static boolean isEnvVarEnabled(String envVar) {
        String value = null;
        try {
            value = System.getenv(envVar);
        } catch (Exception e) {
            LOG.debug("Could not access env var " + envVar + ". " + e, e);
        }
        if (value != null) {
            value = value.trim().toLowerCase();
            return value.equals("true") || value.equals("yes");
        }
        return false;
    }

}
