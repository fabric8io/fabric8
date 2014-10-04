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
package io.fabric8.test.zookeeper.jgroups;

import io.fabric8.zookeeper.jgroups.ConfigurableZooKeeperPing;
import org.jgroups.stack.Protocol;

public class ConfigurableZKPingTest extends PingTestBase {

    protected Protocol createPing() {
        ConfigurableZooKeeperPing zkPing = new ConfigurableZooKeeperPing();
        zkPing.setConnection("localhost:2181");
        return zkPing;
    }

}
