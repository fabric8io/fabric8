/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.fab;

import java.util.logging.Logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DependencyTreeMarshalTest extends DependencyTestSupport {
    private static final transient Logger LOG = Logger.getLogger(DependencyTreeMarshalTest.class.getName());

    @Test
    public void testRoundTrip() throws Exception {
        assertRoundTrip(camel250_clogging_man);
    }

    protected void assertRoundTrip(DependencyTree expected) throws Exception {
        // lets add a dummy URL...
        String expectedUrl = "/tmp/pomegranate/" + getClass().getName() + "/" + expected.getDependencyId() + "/" + expected.getVersion();
        expected.setUrl(expectedUrl);

        String text = expected.marshal();

        LOG.fine("Generated: " + text);

        DependencyTree actual = DependencyTree.unmarshal(text);

        assertEquals("unmarshaled tree does not equal the original tree", expected, actual);
        assertEquals("unmarshalled url", expectedUrl, actual.getUrl());
    }



}
