/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.sample.camel;

import org.junit.Test;

public class CamelTest {
    @Test
    public void testActivator() throws Exception {
        Activator activator = new Activator();
        activator.startCamel();

        Thread.sleep(2000);

        activator.stopCamel();
    }
}
