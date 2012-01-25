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

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPBinary;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPString;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class VariableWidthTypeTest {

    @Test
    public void testVBin8() throws Exception {
        Buffer in = new Buffer("Hello World!".getBytes());
        Buffer out = writeRead(new AMQPBinary(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testVBin32() throws Exception {
        Buffer in = new Buffer(1024);
        Buffer out = writeRead(new AMQPBinary(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testString8() throws Exception {
        String in = "Hello world!";
        String out = writeRead(new AMQPString(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testString32() throws Exception {
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < 2048; i++ ) {
            builder.append((char) ((Math.random() * 52) + 65));
        }
        String in = builder.toString();
        String out = writeRead(new AMQPString(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testSym8() throws Exception {
        Buffer in = new Buffer("Hello world!".getBytes());
        Buffer out = writeRead(new AMQPSymbol(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testSym32() throws Exception {
        Buffer in = new Buffer(2048);
        Buffer out = writeRead(new AMQPSymbol(in)).getValue();
        assertEquals(in, out);
    }
}
