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
 * @openwire:marshaller code="120"
 */
public class ConnectionId implements DataStructure, Comparable<ConnectionId> {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.CONNECTION_ID;

    protected UTF8Buffer value;

    public ConnectionId() {
    }

    public ConnectionId(UTF8Buffer connectionId) {
        this.value = connectionId;
    }

    public ConnectionId(ConnectionId id) {
        this.value = id.getValue();
    }

    public ConnectionId(SessionId id) {
        this.value = id.getConnectionId();
    }

    public ConnectionId(ProducerId id) {
        this.value = id.getConnectionId();
    }

    public ConnectionId(ConsumerId id) {
        this.value = id.getConnectionId();
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != ConnectionId.class) {
            return false;
        }
        ConnectionId id = (ConnectionId)o;
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

    public void setValue(UTF8Buffer connectionId) {
        this.value = connectionId;
    }

    public boolean isMarshallAware() {
        return false;
    }

    public int compareTo(ConnectionId o) {
        return value.compareTo(o.value);
    }
}
