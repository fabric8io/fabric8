/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec;

import org.fusesource.fusemq.amqp.codec.types.AmqpFragment;
import org.fusesource.fusemq.amqp.codec.types.AmqpTransfer;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.fusesource.fusemq.amqp.codec.CodecUtils.marshalUnmarshal;
import static org.fusesource.fusemq.amqp.codec.types.TypeFactory.*;

/**
 *
 */
public class CodecPerfTest {

    @Test
    public void transferPerfTest() throws Exception {
        final int max = 1000000;
        final AtomicLong i = new AtomicLong(0);
        final CountDownLatch latch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                while (i.incrementAndGet() < max) {
                    AmqpTransfer in = createAmqpTransfer();
                    in.setDeliveryTag(createAmqpDeliveryTag(new Buffer(UUID.randomUUID().toString().getBytes())));
                    in.setTransferId(i.get() + 1);
                    in.setHandle(0);
                    in.setFragments(createMultiple());
                    in.setMessageFormat(0);
                    AmqpFragment fragment = createAmqpFragment();
                    fragment.setFirst(true);
                    fragment.setLast(true);
                    fragment.setSectionNumber(0);
                    fragment.setSectionCode(3);
                    fragment.setPayload(new Buffer(("Message : " + i).getBytes()));
                    in.getFragments().setValue(fragment);

                    try {
                        AmqpTransfer out = marshalUnmarshal(in);
                        in.equals(out);
                    } catch (Exception e) {
                        latch.countDown();
                        e.printStackTrace();
                        return;
                    }
                }
                latch.countDown();
            }
        });

        int last = 0;
        while (latch.await(5, TimeUnit.SECONDS) == false) {
            int sample = i.intValue() - last;
            last = last + sample;
            System.out.println("msgs/s : " + (sample / 5));
        }
        Assert.assertTrue(i.get() == max);
    }
}
