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
package io.fabric8.camel.tooling.util;

import java.io.File;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlueprintXmlTest extends RouteXmlTestSupport {

    @Test
    public void testParsesValidXmlFile() throws Exception {
        XmlModel x = assertRoutes(new File(getBaseDir(), "src/test/resources/blueprint.xml"), 1, CamelNamespaces.blueprintNS);

        Set<String> uris = x.endpointUris();

        assertEquals("endpoint uris " + uris, 1, uris.size());
        assertTrue(uris.contains("seda:myConfiguredEndpoint"));
    }

    @Test
    public void testParsesEmptyXmlFile() throws Exception {
        assertRoutes(new File(getBaseDir(), "src/main/resources/io/fabric8/camel/tooling/blueprint-exemplar.xml"), 0, CamelNamespaces.blueprintNS);
    }

}
