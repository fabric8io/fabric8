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
package io.fabric8.api;

import java.util.Map;

public interface MQService {

    static final String MQ_PROFILE_BASE = "mq-base";
    static final String MQ_PROFILE_REPLICATED = "mq-replicated";

    static final String MQ_PID_TEMPLATE = "org.fusesource.mq.fabric.template";
    static final String MQ_CONNECTION_FACTORY_PID = "org.fusesource.mq.fabric.cf";

    static final String MQ_FABRIC_SERVER_PID_PREFIX = "org.fusesource.mq.fabric.server-";

    /**
     * Creates or updates the profile for the given broker and configuration
     *
     * @return the updated or created profile
     */
    Profile createOrUpdateMQProfile(String version, String profile, String brokerName, Map<String, String> configs, boolean replicated);

    /**
     * Creates of updates the profile for clients to connec to the given broker group
     *
     * @return the updated or created profile
     */
    Profile createOrUpdateMQClientProfile(String versionId, String profile, String group, String parentProfileName);

    String getConfig(String version, String config);

    /**
     * Keys for the broker specific PID file inside the profile
     */
    public interface Config {
        public static final String CONNECTORS = "connectors";
        public static final String CONFIG_URL = "config";
        public static final String DATA = "data";
        public static final String GROUP = "group";
        public static final String KIND = "kind";
        public static final String MINIMUM_INSTANCES = "minimumInstances";
        public static final String NETWORKS = "network";
        public static final String NETWORK_USER_NAME = "network.userName";
        public static final String NETWORK_PASSWORD = "network.password";
        public static final String PARENT = "parent";
        public static final String STANDBY_POOL = "standby.pool";
        public static final String REPLICAS = "replicas";

    }
}
