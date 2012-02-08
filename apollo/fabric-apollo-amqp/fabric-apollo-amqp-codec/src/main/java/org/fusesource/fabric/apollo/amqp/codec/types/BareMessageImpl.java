/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataOutput;

/**
 *
 */
public abstract class BareMessageImpl<K> implements BareMessage<K> {

    protected Properties properties;
    protected ApplicationProperties applicationProperties;
    protected K data;

    public BareMessageImpl() {

    }

    public K getData() {
        return data;
    }

    public void setData(K data) {
        this.data = data;
    }

    public Properties getProperties() {
        return properties;
    }

    public Properties getProperties(boolean create) {
        if ( properties == null && create ) {
            properties = new Properties();
        }
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }

    public ApplicationProperties getApplicationProperties(boolean create) {
        if ( applicationProperties == null && create ) {
            applicationProperties = new ApplicationProperties();
        }
        return applicationProperties;
    }

    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public Buffer toBuffer() throws Exception {
        long size = size();
        if ( size == 0 ) {
            return new Buffer(0);
        }
        DataByteArrayOutputStream out = new DataByteArrayOutputStream((int) size);
        write(out);
        return out.toBuffer();
    }

    public long size() {
        long rc = 0;
        if ( properties != null ) {
            rc += properties.size();
        }
        if ( applicationProperties != null ) {
            rc += properties.size();
        }
        if ( data != null ) {
            rc += dataSize();
        }
        return rc;
    }

    public void write(DataOutput out) throws Exception {
        if ( properties != null ) {
            properties.write(out);
        }
        if ( applicationProperties != null ) {
            applicationProperties.write(out);
        }
        if ( data != null ) {
            dataWrite(out);
        }
    }

    public abstract long dataSize();

    public abstract void dataWrite(DataOutput out) throws Exception;

    public String toString() {
        StringBuffer buf = new StringBuffer();

        if ( properties != null ) {
            buf.append("\n");
            buf.append(properties.toString());
        }

        if ( data != null ) {
            buf.append("\n");
            buf.append(data.toString());
        }

        if ( applicationProperties != null ) {
            buf.append("\n");
            buf.append(applicationProperties.toString());
        }

        return buf.toString();
    }

}
