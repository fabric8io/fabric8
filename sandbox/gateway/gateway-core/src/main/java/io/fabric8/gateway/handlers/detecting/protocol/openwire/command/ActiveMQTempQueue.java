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

/**
 * @openwire:marshaller code="102"
 * @version $Revision: 1.6 $
 */
public class ActiveMQTempQueue extends ActiveMQTempDestination {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_TEMP_QUEUE;

    public ActiveMQTempQueue() {
    }

    public ActiveMQTempQueue(String name) {
        super(strip(name));
    }

    private static String strip(String name) {
        if(name.startsWith(TEMP_QUEUE_QUALIFED_PREFIX)) {
            return name.substring(TEMP_QUEUE_QUALIFED_PREFIX.length());
        } else {
            return name;
        }
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String getQualifiedPrefix() {
        return TEMP_QUEUE_QUALIFED_PREFIX;
    }

    public boolean isQueue() {
        return true;
    }

    public byte getDestinationType() {
        return TEMP_QUEUE_TYPE;
    }

}
