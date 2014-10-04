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


import io.fabric8.gateway.handlers.detecting.protocol.openwire.IntrospectionSupport;

/**
 * @openwire:marshaller code="10"
 */
public class KeepAliveInfo extends BaseCommand {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.KEEP_ALIVE_INFO;

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public boolean isResponse() {
        return false;
    }

    public boolean isMessageDispatch() {
        return false;
    }

    public boolean isMessage() {
        return false;
    }

    public boolean isMessageAck() {
        return false;
    }

    public boolean isBrokerInfo() {
        return false;
    }

    public boolean isWireFormatInfo() {
        return false;
    }

    public boolean isMarshallAware() {
        return false;
    }

    public boolean isMessageDispatchNotification() {
        return false;
    }

    public boolean isShutdownInfo() {
        return false;
    }

    public String toString() {
        return IntrospectionSupport.toString(this, KeepAliveInfo.class);
    }
}
