/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.api.SequenceMessage;

import java.io.DataOutput;
import java.util.List;

/**
 *
 */
public class SequenceMessageImpl extends BareMessageImpl<List<AMQPSequence>> implements SequenceMessage {

    public SequenceMessageImpl() {

    }

    public SequenceMessageImpl(List sequence) {
        data = sequence;
    }

    public long dataSize() {
        long rc = 0;
        for ( AMQPSequence s : data ) {
            if ( s != null ) {
                rc += s.size();
            }
        }
        return rc;
    }

    @Override
    public void dataWrite(DataOutput out) throws Exception {
        for ( AMQPSequence s : data ) {
            if ( s != null ) {
                s.write(out);
            }
        }
    }

}
