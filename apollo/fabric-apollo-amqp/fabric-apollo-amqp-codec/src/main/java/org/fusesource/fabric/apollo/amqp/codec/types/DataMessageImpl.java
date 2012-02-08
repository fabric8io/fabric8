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

import org.fusesource.fabric.apollo.amqp.codec.api.DataMessage;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DataMessageImpl extends BareMessageImpl<List<Data>> implements DataMessage {

    public DataMessageImpl() {

    }

    public DataMessageImpl(Data data) {
        this.data = new ArrayList<Data>();
        this.data.add(data);
    }

    public DataMessageImpl(Buffer data) {
        this.data = new ArrayList<Data>();
        this.data.add(new Data(data));
    }

    public DataMessageImpl(List data) {
        this.data = new ArrayList<Data>();
        for ( Object obj : data ) {
            if ( obj instanceof Buffer ) {
                this.data.add(new Data((Buffer) obj));
            } else if ( obj instanceof Data ) {
                this.data.add((Data) obj);
            }
        }
    }

    @Override
    public long dataSize() {
        long rc = 0;
        for ( Data d : data ) {
            if ( d != null ) {
                rc += d.size();
            }
        }
        return rc;
    }

    @Override
    public void dataWrite(DataOutput out) throws Exception {
        if ( data != null ) {
            for ( Data d : data ) {
                if ( d != null ) {
                    d.write(out);
                }
            }
        }
    }

}
