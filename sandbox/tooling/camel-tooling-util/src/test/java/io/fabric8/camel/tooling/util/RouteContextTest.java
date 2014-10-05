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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteContextTest extends RouteXmlTestSupport {

    @Test
    public void testParsesRouteContextSpringXmlFile() throws Exception {
        assertRoutes(new File(getBaseDir(), "src/test/resources/routeContext.xml"), 1, null);
    }

    @Test
    public void testParsesRouteContextBlueprintXmlFile() throws Exception {
        assertRoutes(new File(getBaseDir(), "src/test/resources/routeContextBP.xml"), 1, CamelNamespaces.blueprintNS);
    }

    @Test
    public void saveRouteContext() throws Exception {
        XmlModel x = assertLoadModel(new File(getBaseDir(), "src/test/resources/routeContext.xml"), 1);
        int rc = x.getRouteDefinitionList().size();

        // now lets add a route and write it back again...
        DefaultCamelContext tmpContext = new DefaultCamelContext();
        tmpContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:newFrom").to("seda:newTo");
            }
        });
        x.getContextElement().getRoutes().addAll(tmpContext.getRouteDefinitions());

        String xmlText = tool.marshalToText(x);

        XmlModel y = tool.unmarshal(xmlText);

        assertTrue(y.getRouteDefinitionList().size() == x.getRouteDefinitionList().size());
        assertFalse(y.getRouteDefinitionList().size() == rc);
    }

    @Test
    public void saveRouteContextBP() throws Exception {
        XmlModel x = assertLoadModel(new File(getBaseDir(), "src/test/resources/routeContextBP.xml"), 1);
        System.err.println(tool.marshalToText(x));
    }

}
