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
 * @openwire:marshaller code="123"
 */
public class ProducerId implements DataStructure {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.PRODUCER_ID;

    protected UTF8Buffer connectionId;
    protected long sessionId;
    protected long value;

    protected transient int hashCode;
    protected transient String key;
    protected transient SessionId parentId;

    public ProducerId() {
    }

    public ProducerId(SessionId sessionId, long producerId) {
        this.connectionId = sessionId.getConnectionId();
        this.sessionId = sessionId.getValue();
        this.value = producerId;
    }

    public ProducerId(ProducerId id) {
        this.connectionId = id.getConnectionId();
        this.sessionId = id.getSessionId();
        this.value = id.getValue();
    }

    public ProducerId(String producerKey) {
        // Parse off the producerId
        int p = producerKey.lastIndexOf(":");
        if (p >= 0) {
            value = Long.parseLong(producerKey.substring(p + 1));
            producerKey = producerKey.substring(0, p);
        }
        setProducerSessionKey(producerKey);
    }

    public SessionId getParentId() {
        if (parentId == null) {
            parentId = new SessionId(this);
        }
        return parentId;
    }

    public int hashCode() {
        if (hashCode == 0) {
            hashCode = connectionId.hashCode() ^ (int)sessionId ^ (int)value;
        }
        return hashCode;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != ProducerId.class) {
            return false;
        }
        ProducerId id = (ProducerId)o;
        return sessionId == id.sessionId && value == id.value && connectionId.equals(id.connectionId);
    }

    /**
     * @param sessionKey
     */
    private void setProducerSessionKey(String sessionKey) {
        // Parse off the value
        int p = sessionKey.lastIndexOf(":");
        if (p >= 0) {
            sessionId = Long.parseLong(sessionKey.substring(p + 1));
            sessionKey = sessionKey.substring(0, p);
        }
        // The rest is the value
        connectionId = new UTF8Buffer(sessionKey);
    }

    public String toString() {
        if (key == null) {
            key = connectionId + ":" + sessionId + ":" + value;
        }
        return key;
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    /**
     * @openwire:property version=1 cache=true
     */
    public UTF8Buffer getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UTF8Buffer connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * @openwire:property version=1
     */
    public long getValue() {
        return value;
    }

    public void setValue(long producerId) {
        this.value = producerId;
    }

    /**
     * @openwire:property version=1
     */
    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isMarshallAware() {
        return false;
    }
}
