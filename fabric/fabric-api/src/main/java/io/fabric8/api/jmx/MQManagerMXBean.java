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
package io.fabric8.api.jmx;

import java.io.IOException;
import java.util.List;

/**
 * An MBean for working with the global A-MQ topology configuration inside the Fabric profiles
 */
public interface MQManagerMXBean {

    /**
     * Returns the current logical Fabric broker configuration
     */
    List<MQBrokerConfigDTO> loadBrokerConfiguration();

    /**
     * Saves the broker configuration as JSON
     */
    void saveBrokerConfigurationJSON(String json) throws IOException;

    /**
     * Returns the current runtime status of all the logical Fabric brokers
     */
    List<MQBrokerStatusDTO> loadBrokerStatus() throws Exception;
}
