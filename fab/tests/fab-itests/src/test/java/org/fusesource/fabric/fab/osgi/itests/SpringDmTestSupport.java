/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;

/**
 */
public abstract class SpringDmTestSupport extends FabIntegrationTestSupport {

    @Configuration
    public static Option[] configuration() throws Exception {
        return configuration(true);
    }

}
