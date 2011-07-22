/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.junit.Test;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.BitUtils.unsigned;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class BitUtilsTest {

    @Test
    public void testUnsignedByte() throws Exception {
        byte wrapped = -96;
        short value = unsigned(wrapped);

        System.out.printf("in : %X out : %X\n", wrapped, value);

        assertEquals(wrapped, (byte) value);


    }
}
