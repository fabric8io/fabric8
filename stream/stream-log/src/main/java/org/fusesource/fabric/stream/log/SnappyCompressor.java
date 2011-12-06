/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.stream.log;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.iq80.snappy.Snappy;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class SnappyCompressor implements Processor {

    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        if (in.getBody() != null) {
            byte[] data = in.getMandatoryBody(byte[].class);
            byte[] compressed = compress(data);

            if (exchange.getPattern().isOutCapable()) {
                Message out = exchange.getOut();
                out.copyFrom(in);
                out.setBody(compressed);
            } else {
                in.setBody(compressed);
            }
        }
    }

    private byte[] compress(byte[] data) {
        byte[] compressed = new byte[Snappy.maxCompressedLength(data.length)];
        int len = Snappy.compress(data, 0, data.length, compressed, 0);
        byte[] result = new byte[len];
        System.arraycopy(compressed, 0, result, 0, len);
        return result;
    }

}
