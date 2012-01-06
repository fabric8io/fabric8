/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zeroconf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class ZeroConfTest {
    protected ZeroConfBridge activator = new ZeroConfBridge();

    @Before
    public void start() throws Exception {
        activator.start();
    }

    @After
    public void stop() throws Exception {
        activator.stop();
    }

    @Test
    public void viewZeroConf() throws Exception {
        System.out.println("Waiting for ZK");
        Thread.sleep(15 * 1000);
        System.out.println("Done!");

    }
}
