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
package io.fabric8.fab.osgi.commands.fab;

import org.junit.Test;

import java.util.List;

import static io.fabric8.fab.osgi.commands.fab.InfoCommand.getClassPathElements;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests cases for the helper method in {@link InfoCommand}
 */
public class InfoCommandTest {

    private static final String BUNDLE_CLASSPATH =
            ".,org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-httpclient.jar," +
            "commons-logging.commons-logging.jar," +
            "org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-codec.jar";

    @Test
    public void testGetClassPathElements() {
        List<String> elements = getClassPathElements(BUNDLE_CLASSPATH);
        assertEquals(3, elements.size());
        assertTrue(elements.contains("org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-httpclient.jar"));
        assertTrue(elements.contains("commons-logging.commons-logging.jar"));
        assertTrue(elements.contains("org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-codec.jar"));
    }



}
