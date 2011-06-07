/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * Helper methods for loading sample files
 */
public class SampleDataFiles {
    
    public static InputStream sampleTransactionsFile() {
        return resourceAsStream("Transactions.txt");
    }

    public static InputStream resourceAsStream(String uri) {
        InputStream in = SampleDataFiles.class.getClassLoader().getResourceAsStream(uri);
        assertNotNull("Should have found valid data file on class loader for: " + uri, in);
        return in;
    }
}
