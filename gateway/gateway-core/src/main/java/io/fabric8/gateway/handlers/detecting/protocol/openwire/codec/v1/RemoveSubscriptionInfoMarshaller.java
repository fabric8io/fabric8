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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.codec.v1;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.codec.BooleanStream;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.codec.OpenWireFormat;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.ConnectionId;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.DataStructure;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.RemoveSubscriptionInfo;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import java.io.IOException;


/**
 * Marshalling code for Open Wire Format for RemoveSubscriptionInfoMarshaller
 *
 *
 * NOTE!: This file is auto generated - do not modify!
 *        Modify the 'apollo-openwire-generator' module instead.
 *
 */
public class RemoveSubscriptionInfoMarshaller extends BaseCommandMarshaller {

    /**
     * Return the type of Data Structure we marshal
     * @return short representation of the type data structure
     */
    public byte getDataStructureType() {
        return RemoveSubscriptionInfo.DATA_STRUCTURE_TYPE;
    }
    
    /**
     * @return a new object instance
     */
    public DataStructure createObject() {
        return new RemoveSubscriptionInfo();
    }

    /**
     * Un-marshal an object instance from the data input stream
     *
     * @param o the object to un-marshal
     * @param dataIn the data input stream to build the object from
     * @throws IOException
     */
    public void tightUnmarshal(OpenWireFormat wireFormat, Object o, DataByteArrayInputStream dataIn, BooleanStream bs) throws IOException {
        super.tightUnmarshal(wireFormat, o, dataIn, bs);

        RemoveSubscriptionInfo info = (RemoveSubscriptionInfo)o;
        info.setConnectionId((ConnectionId)tightUnmarsalCachedObject(wireFormat, dataIn, bs));
        info.setSubscriptionName(tightUnmarshalString(dataIn, bs));
        info.setClientId(tightUnmarshalString(dataIn, bs));

    }


    /**
     * Write the booleans that this object uses to a BooleanStream
     */
    public int tightMarshal1(OpenWireFormat wireFormat, Object o, BooleanStream bs) throws IOException {

        RemoveSubscriptionInfo info = (RemoveSubscriptionInfo)o;

        int rc = super.tightMarshal1(wireFormat, o, bs);
        rc += tightMarshalCachedObject1(wireFormat, (DataStructure)info.getConnectionId(), bs);
        rc += tightMarshalString1(info.getSubscriptionName(), bs);
        rc += tightMarshalString1(info.getClientId(), bs);

        return rc + 0;
    }

    /**
     * Write a object instance to data output stream
     *
     * @param o the instance to be marshaled
     * @param dataOut the output stream
     * @throws IOException thrown if an error occurs
     */
    public void tightMarshal2(OpenWireFormat wireFormat, Object o, DataByteArrayOutputStream dataOut, BooleanStream bs) throws IOException {
        super.tightMarshal2(wireFormat, o, dataOut, bs);

        RemoveSubscriptionInfo info = (RemoveSubscriptionInfo)o;
        tightMarshalCachedObject2(wireFormat, (DataStructure)info.getConnectionId(), dataOut, bs);
        tightMarshalString2(info.getSubscriptionName(), dataOut, bs);
        tightMarshalString2(info.getClientId(), dataOut, bs);

    }

    /**
     * Un-marshal an object instance from the data input stream
     *
     * @param o the object to un-marshal
     * @param dataIn the data input stream to build the object from
     * @throws IOException
     */
    public void looseUnmarshal(OpenWireFormat wireFormat, Object o, DataByteArrayInputStream dataIn) throws IOException {
        super.looseUnmarshal(wireFormat, o, dataIn);

        RemoveSubscriptionInfo info = (RemoveSubscriptionInfo)o;
        info.setConnectionId((ConnectionId)looseUnmarsalCachedObject(wireFormat, dataIn));
        info.setSubscriptionName(looseUnmarshalString(dataIn));
        info.setClientId(looseUnmarshalString(dataIn));

    }


    /**
     * Write the booleans that this object uses to a BooleanStream
     */
    public void looseMarshal(OpenWireFormat wireFormat, Object o, DataByteArrayOutputStream dataOut) throws IOException {

        RemoveSubscriptionInfo info = (RemoveSubscriptionInfo)o;

        super.looseMarshal(wireFormat, o, dataOut);
        looseMarshalCachedObject(wireFormat, (DataStructure)info.getConnectionId(), dataOut);
        looseMarshalString(info.getSubscriptionName(), dataOut);
        looseMarshalString(info.getClientId(), dataOut);

    }
}
