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

import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class FixedWidthTypeTest {

    @Test
    public void testNull() throws Exception {
        Long in = null;
        //we lose the fact that this was a long type when decoding
        Object out = writeRead(new AMQPLong(in));
        assertNull(out);
    }

    @Test
    public void testBoolean() throws Exception {
        boolean values[] = new boolean[]{true, false};
        for ( boolean in : values ) {
            boolean out = writeRead(new AMQPBoolean(in)).getValue().booleanValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testByte() throws Exception {
        byte values[] = new byte[]{0, 1, 50, 127, -45};
        for ( byte in : values ) {
            byte out = writeRead(new AMQPByte(in)).getValue().byteValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testCharacter() throws Exception {
        char values[] = new char[]{'A', 'b', '%', '|'};
        for ( char in : values ) {
            char out = writeRead(new AMQPChar(in)).getValue().charValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testInt() throws Exception {
        int values[] = new int[]{0, 53, 253, 62323};
        for ( int in : values ) {
            int out = writeRead(new AMQPInt(in)).getValue().intValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testLong() throws Exception {
        long values[] = new long[]{0, 53, 253, 62323};
        for ( long in : values ) {
            long out = writeRead(new AMQPLong(in)).getValue().longValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testFloat() throws Exception {
        float values[] = new float[]{0, (float) 0.53, (float) 25.3, (float) 623.23};
        for ( float in : values ) {
            float out = writeRead(new AMQPFloat(in)).getValue().floatValue();
            assertEquals(in, out, 0);
        }
    }

    @Test
    public void testDouble() throws Exception {
        double values[] = new double[]{0, 0.53, 25.3, 623.23};
        for ( double in : values ) {
            double out = writeRead(new AMQPDouble(in)).getValue().doubleValue();
            assertEquals(in, out, 0);
        }
    }

    @Test
    public void testTimestamp() throws Exception {
        Date in = new Date();
        Date out = writeRead(new AMQPTimestamp(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testUByte() throws Exception {
        short in = (short) 0xFF;
        short out = writeRead(new AMQPUByte(in)).getValue().shortValue();
        assertEquals(in, out);
    }

    @Test
    public void testUShort() throws Exception {
        int in = 0xFFFF;
        int out = writeRead(new AMQPUShort(in)).getValue().intValue();
        assertEquals(in, out);
    }

    @Test
    public void testUInt() throws Exception {
        long values[] = new long[]{0, 5, 32, 1024, 8192, ((long) 0xFFFFFFFF)};
        for ( long in : values ) {
            long out = writeRead(new AMQPUInt(in)).getValue().longValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testULong() throws Exception {
        BigInteger values[] = new BigInteger[]{
                new BigInteger("0").abs(),
                new BigInteger("5").abs(),
                new BigInteger("512384").abs(),
                new BigInteger("3423423521352353234").abs()
        };
        for ( BigInteger in : values ) {
            BigInteger out = writeRead(new AMQPULong(in)).getValue();
            assertEquals(in, out);
        }
    }

    @Test
    public void testUUID() throws Exception {
        UUID in = UUID.randomUUID();
        UUID out = writeRead(new AMQPUUID(in)).getValue();
        assertEquals(in, out);
    }
}
