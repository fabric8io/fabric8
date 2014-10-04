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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.command;

import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * Represents a discovery event containing the details of the service
 * 
 * @openwire:marshaller code="40"
 * @version $Revision:$
 */
public class DiscoveryEvent implements DataStructure {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.DISCOVERY_EVENT;

    protected UTF8Buffer serviceName;
    protected UTF8Buffer brokerName;

    public DiscoveryEvent() {
    }

    public DiscoveryEvent(UTF8Buffer serviceName) {
        this.serviceName = serviceName;
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getServiceName() {
        return serviceName;
    }

    public void setServiceName(UTF8Buffer serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(UTF8Buffer name) {
        this.brokerName = name;
    }

    public boolean isMarshallAware() {
        return false;
    }
}
