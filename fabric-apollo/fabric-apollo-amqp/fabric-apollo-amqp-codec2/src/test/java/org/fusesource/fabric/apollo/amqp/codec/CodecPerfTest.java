/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.types.Transfer;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;

/**
 *
 */
public class CodecPerfTest {

    @Test
    public void transferPerfTest() throws Exception {
        final int max = 10000000;
        final AtomicLong i = new AtomicLong(0);
        final CountDownLatch latch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                System.out.println("Starting loop");
                while (i.incrementAndGet() < max) {
                    Transfer in = new Transfer();
                    in.setDeliveryTag(new Buffer(("" + i.get()).getBytes()));
                    in.setDeliveryID(i.get() + 1);
                    in.setHandle(0L);
                    in.setMessageFormat(0L);
                    try {
                        Transfer out = writeRead(in, false);
                    } catch (Exception e) {
                        latch.countDown();
                        e.printStackTrace();
                        return;
                    }
                }
                System.out.println("Finished loop");
                latch.countDown();
            }
        });

        int last = 0;
        while (latch.await(1, TimeUnit.SECONDS) == false) {
            last = printMsgsPerSec(i.intValue(), last);
        }
        last = printMsgsPerSec(i.intValue(), last);
        Assert.assertTrue(i.get() == max);
    }

    public int printMsgsPerSec(int current, int last) {
        int sample = current - last;
        System.out.println("msgs/s : " + (sample));
        return last + sample;
    }
}
