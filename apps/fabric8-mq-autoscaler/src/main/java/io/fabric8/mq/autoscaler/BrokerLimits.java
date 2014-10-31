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
package io.fabric8.mq.autoscaler;

import static io.fabric8.mq.autoscaler.EnvUtils.getEnv;

public class BrokerLimits {
    private int connectionsLimit = 10;
    private int destinationsLimit = 10;

    public BrokerLimits() {
        getEnv("connectionsLimit", 10);
        getEnv("destinationsLimit", 10);
    }

    public int getConnectionsLimit() {
        return connectionsLimit;
    }

    public void setConnectionsLimit(int connectionsLimit) {
        this.connectionsLimit = connectionsLimit;
    }

    public int getDestinationsLimit() {
        return destinationsLimit;
    }

    public void setDestinationsLimit(int destinationsLimit) {
        this.destinationsLimit = destinationsLimit;
    }

    public String toString() {
        return "BrokerLimits: connectionsLimit=" + getConnectionsLimit() + ", destinationsLimit=" + getDestinationsLimit();
    }
}
