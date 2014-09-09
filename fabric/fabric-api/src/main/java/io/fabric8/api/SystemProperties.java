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


public final class SystemProperties {

    public static final String ZOOKEEPER_PASSWORD = "zookeeper.password";
    public static final String PROFILE = "profile";
    public static final String KARAF_BASE = "karaf.base";
    public static final String KARAF_NAME = "karaf.name";
    public static final String JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";
    public static final String FABRIC_ENVIRONMENT = "fabric.environment";

    /**
     * The current effective profile from the current set of profiles
     */
    public static final String FABRIC_PROFILE_ENVIRONMENT = "fabric.profile.environment";


    private SystemProperties() {
        //Utility Class
    }

}
