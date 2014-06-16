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
     * The default docker image to create
     */
    public static final String DEFAULT_IMAGE = "fabric8/fabric8";

    /**
     * The start of the ports created to forward from the host to the internal docker port
     */
    public static final int DEFAULT_EXTERNAL_PORT = 49000;

    /**
     * The name of the docker provider profile which hosts the default docker configuration if there is
     * no docker specific configuration in the given profiles which are being created.
     */
    public static final String DOCKER_PROVIDER_PROFILE_ID = "docker";

    /**
     * Config Admin property keys of the {@link #DOCKER_PROVIDER_PID}
     */
    public static class PROPERTIES {
        public static final String IMAGE = "image";
        public static final String CMD = "cmd";
        public static final String JAVA_LIBRARY_PATH = "javaLibraryPath";
        public static final String HOME_PATH = "homePath";
        public static final String IMAGE_REPOSITORY = "imageRepository";
        public static final String IMAGE_ENTRY_POINT = "imageEntryPoint";
    }

    /**
     * Environment variables
     */
    public static class EnvironmentVariables {


        /**
         * Environment variable to define the default container image to be used if none is explicitly configured
         * in the profile being created.
         *
         * This is the easiest way to change the docker container image; for example if you wish to use a local build
         * of your own docker container changes.
         *
         * If you have a local build of the fabric8/fabric8 container image then set this variable to "fabric8:fabric8" to enable
         * your local build to be used instead of the public distro.
         */
        public static final String FABRIC8_DOCKER_DEFAULT_IMAGE = "FABRIC8_DOCKER_DEFAULT_IMAGE";
    }

}
