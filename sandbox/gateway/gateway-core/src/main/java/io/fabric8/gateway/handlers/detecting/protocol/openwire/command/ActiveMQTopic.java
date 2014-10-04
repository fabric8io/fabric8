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
 * @openwire:marshaller code="101"
 * @version $Revision: 1.5 $
 */
public class ActiveMQTopic extends ActiveMQDestination {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_TOPIC;

    public ActiveMQTopic() {
    }

    public ActiveMQTopic(String name) {
        super(strip(name));
    }
    private static String strip(String name) {
        if(name.startsWith(TOPIC_QUALIFIED_PREFIX)) {
            return name.substring(TOPIC_QUALIFIED_PREFIX.length());
        } else {
            return name;
        }
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String getQualifiedPrefix() {
        return TOPIC_QUALIFIED_PREFIX;
    }

    public boolean isTopic() {
        return true;
    }

    public byte getDestinationType() {
        return TOPIC_TYPE;
    }

}
