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

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeRegistry;
import org.fusesource.fabric.apollo.amqp.codec.types.Header;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CodecTest {

    @Test
    public void testTypeRegistry() throws Exception {

        TypeRegistry registry = TypeRegistry.instance();

        for ( Byte key : registry.getPrimitiveFormatCodeMap().keySet() ) {
            Class clazz = registry.getPrimitiveFormatCodeMap().get(key);
            //System.out.printf("0x%x = %s\n", key, clazz.getName());
        }

        for ( BigInteger key : registry.getFormatCodeMap().keySet() ) {
            Class clazz = registry.getFormatCodeMap().get(key);
            //System.out.printf("0x%x = %s\n", key, clazz.getName());
        }

        for ( Buffer key : registry.getSymbolicCodeMap().keySet() ) {
            Class clazz = registry.getSymbolicCodeMap().get(key);
            //System.out.printf("%s = %s\n", key.ascii(), clazz.getName());
        }

        assertTrue(registry.getPrimitiveFormatCodeMap().size() > 0);
        assertTrue(registry.getFormatCodeMap().size() > 0);
        assertTrue(registry.getSymbolicCodeMap().size() > 0);

        AMQPType type = (AMQPType) registry.getSymbolicCodeMap().get(new AsciiBuffer("amqp:header:list")).newInstance();

        //System.out.printf("Got type : %s", type.getClass().getName());

        assertSame("Received type does not match expected type!", type.getClass(), Header.class);
    }

}
