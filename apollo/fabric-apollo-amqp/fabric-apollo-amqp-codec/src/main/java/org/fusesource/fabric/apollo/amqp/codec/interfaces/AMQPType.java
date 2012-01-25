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

package org.fusesource.fabric.apollo.amqp.codec.interfaces;

import java.io.DataInput;
import java.io.DataOutput;

public interface AMQPType {

    public Object getArrayConstructor();

    public void write(DataOutput out) throws Exception;
    public byte writeConstructor(DataOutput out) throws Exception;
    public void writeBody(byte formatCode, DataOutput out) throws Exception;

    public void read(byte formatCode, DataInput in) throws Exception;

    /*
    public void encodeTo(Buffer buffer, int offset) throws Exception;
    public void decodeFrom(byte formatCode, Buffer buffer, int offset) throws Exception;
    */

    public long size();
    public long sizeOfConstructor();
    public long sizeOfBody();

}
