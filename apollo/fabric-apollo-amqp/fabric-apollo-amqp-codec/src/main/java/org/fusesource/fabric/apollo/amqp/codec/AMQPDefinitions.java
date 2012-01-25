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

import org.fusesource.hawtbuf.AsciiBuffer;

/**
 * Definitions for the AMQP protocol
 */
public interface AMQPDefinitions {

    /**
     * Protocol magic identifier
     */
    public static final byte[] MAGIC = new AsciiBuffer("AMQP").getData();

    /**
     * Default IANA assigned AMQP port as an int
     */
    public static final int PORT = Integer.parseInt(Definitions.PORT);

    /**
     * Default IANA assigned secure AMQP (amqps) port as an int
     */
    public static final int SECURE_PORT = Integer.parseInt(Definitions.SECURE_PORT);

    /**
     * Protocol ID, only used in the preamble, is defined to just be 0
     */
    public static final byte PROTOCOL_ID = 0x0;

    /**
     * SASL Protocol ID
     */
    public static final byte SASL_PROTOCOL_ID = 0x3;

    /**
     * Major as a byte
     */
    public static final byte MAJOR = Byte.parseByte(Definitions.MAJOR);

    /**
     * Minor as a byte
     */
    public static final byte MINOR = Byte.parseByte(Definitions.MINOR);

    /**
     * Revision as a byte
     */
    public static final byte REVISION = Byte.parseByte(Definitions.REVISION);

    public static final int MIN_MAX_FRAME_SIZE = Integer.parseInt(Definitions.MIN_MAX_FRAME_SIZE);

}
