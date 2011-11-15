/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DependencyTreeMarshalTest extends DependencyTestSupport {
    private static final transient Log LOG = LogFactory.getLog(DependencyTreeMarshalTest.class);

    @Test
    public void testRoundTrip() throws Exception {
        assertRoundTrip(camel250_clogging_man);
    }

    protected void assertRoundTrip(DependencyTree expected) throws Exception {
        // lets add a dummy URL...
        String expectedUrl = "/tmp/pomegranate/" + getClass().getName() + "/" + expected.getDependencyId() + "/" + expected.getVersion();
        expected.setUrl(expectedUrl);

        String text = expected.marshal();

        LOG.debug("Generated: " + text);

        DependencyTree actual = DependencyTree.unmarshal(text);

        assertEquals("unmarshaled tree does not equal the original tree", expected, actual);
        assertEquals("unmarshalled url", expectedUrl, actual.getUrl());
    }



}
