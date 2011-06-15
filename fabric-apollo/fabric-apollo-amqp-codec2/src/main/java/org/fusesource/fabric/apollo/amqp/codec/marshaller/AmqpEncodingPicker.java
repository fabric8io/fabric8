/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.EncodingPicker;
import org.fusesource.hawtbuf.Buffer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class AmqpEncodingPicker implements EncodingPicker {

    private static final AmqpEncodingPicker SINGLETON = new AmqpEncodingPicker();

    public static AmqpEncodingPicker instance() {
        return SINGLETON;
    }

    public byte chooseArrayEncoding(Object[] value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseBinaryEncoding(Buffer value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseBooleanEncoding(Boolean value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseByteEncoding(Byte value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseCharEncoding(Character value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseDecimal128Encoding(BigDecimal value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseDecimal32Encoding(BigDecimal value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseDecimal64Encoding(BigDecimal value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseDoubleEncoding(Double value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseFloatEncoding(Float value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseIntEncoding(Integer value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseListEncoding(List value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseLongEncoding(Long value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseMapEncoding(Map value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseShortEncoding(Short value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseStringEncoding(String value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseSymbolEncoding(Buffer value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseTimestampEncoding(Date value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseUByteEncoding(Byte value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseUIntEncoding(Integer value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseULongEncoding(Long value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseUShortEncoding(Short value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte chooseUUIDEncoding(UUID value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
