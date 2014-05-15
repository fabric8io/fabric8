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
import java.util.List;

import org.apache.camel.model.RouteDefinition;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class WhitespacePreserveTest extends RouteXmlTestSupport {

    public void testWhitespaceXmlFile(String name) throws Exception {
        File file = new File(getBaseDir(), name);
        XmlModel x = assertRoutes(file, 1, null);

        // now lets modify the xml
        List<RouteDefinition> definitionList = x.getRouteDefinitionList();
        RouteDefinition route = new RouteDefinition().from("file:foo").to("file:bar");
        definitionList.add(route);

        System.out.println("Routes now: " + x.getRouteDefinitionList());

        RouteXml helper = new RouteXml();
        String newText = helper.marshalToText(x);

        System.out.println("newText: " + newText);

        assertTrue(newText.contains("a comment before root element"));
    }

    @Test
    public void testUpdateXmlAndKeepWhiteSpace() throws Exception {
        String name = "src/test/resources/whitespaceRoute.xml";
        testWhitespaceXmlFile(name);
    }

    @Test
    public void testUpdateXmlUsingNamespacePrefixesAndKeepWhitespace() throws Exception {
        String name = "src/test/resources/whitespaceRouteAndPrefix.xml";
        testWhitespaceXmlFile(name);
    }

}
