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

import org.fusesource.fabric.apollo.amqp.codec.api.ValueMessage;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;

import java.io.DataOutput;

/**
 *
 */
public class ValueMessageImpl extends BareMessageImpl<AMQPValue> implements ValueMessage {

    public ValueMessageImpl() {

    }

    public ValueMessageImpl(AMQPType data) {
        this.data = new AMQPValue(data);
    }

    public ValueMessageImpl(AMQPValue data) {
        this.data = data;
    }

    public void setData(AMQPType data) {
        this.data = new AMQPValue(data);
    }

    @Override
    public long dataSize() {
        return data.size();
    }

    @Override
    public void dataWrite(DataOutput out) throws Exception {
        data.write(out);
    }
}
