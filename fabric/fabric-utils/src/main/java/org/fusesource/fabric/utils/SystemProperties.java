/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.utils;


public final class SystemProperties {

    public static final String AGENT_AUTOSTART = "agent.auto.start";
    public static final String ENSEMBLE_AUTOSTART = "ensemble.auto.start";
    public static final String PROFILES_AUTOIMPORT_PATH = "profiles.auto.import.path";
    public static final String ZOOKEEPER_PASSWORD = "zookeeper.password";
    public static final String PROFILE = "profile";

    public static final String KARAF_NAME = "karaf.name";


    private SystemProperties() {
        //Utility Class
    }

}
