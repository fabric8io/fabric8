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
 * @openwire:marshaller code="124"
 */
public class BrokerId implements DataStructure {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.BROKER_ID;
    protected UTF8Buffer value;

    public BrokerId() {
    }

    public BrokerId(UTF8Buffer brokerId) {
        this.value = brokerId;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != BrokerId.class) {
            return false;
        }
        BrokerId id = (BrokerId)o;
        return value.equals(id.value);
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String toString() {
        return value.toString();
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getValue() {
        return value;
    }

    public void setValue(UTF8Buffer brokerId) {
        this.value = brokerId;
    }

    public boolean isMarshallAware() {
        return false;
    }
}
