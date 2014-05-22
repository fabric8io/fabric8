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

import org.apache.camel.model.DescriptionDefinition;
import org.apache.camel.model.RouteDefinition;
import org.junit.Test;

import static org.junit.Assert.*;

public class PreserveCommentTest extends RouteXmlTestSupport {

    @Test
    public void testCommentsBeforeRoutePreserved() throws Exception {
        XmlModel x = assertRoundTrip("src/test/resources/commentBeforeRoute.xml", 2);

        RouteDefinition route1 = x.getRouteDefinitionList().get(1);
        assertEquals("route4", route1.getId());
        DescriptionDefinition desc = route1.getDescription();
        assertNotNull(desc);
        assertEquals("route4 description\ncomment about route4", desc.getText());
    }

    @Test
    public void testCommentsInCamelContextPreserved() throws Exception {
        XmlModel x = assertRoundTrip("src/test/resources/commentInCamelContext.xml", 1);

        RouteXml helper = new RouteXml();
        String newText = helper.marshalToText(x);

        System.out.println("newText: " + newText);

        assertTrue(newText.contains("comment in camelContext"));
    }

    @Test
    public void testCommentsInRoutePreserved() throws Exception {
        XmlModel x = assertRoundTrip("src/test/resources/commentInRoute.xml", 1);

        RouteDefinition route1 = x.getRouteDefinitionList().get(0);
        DescriptionDefinition desc = route1.getDescription();
        assertNotNull(desc);
        assertEquals("route3 comment", desc.getText());
    }

    @Test
    public void testCommentsInRouteWithDescriptionPreserved() throws Exception {
        XmlModel x = assertRoundTrip("src/test/resources/commentInRouteWithDescription.xml", 1);

        RouteDefinition route1 = x.getRouteDefinitionList().get(0);
        DescriptionDefinition desc = route1.getDescription();
        assertNotNull(desc);
        assertEquals("previous description\nnew comment added to previous one", desc.getText());
    }

    private XmlModel assertRoundTrip(String name, int count) throws Exception {
        File file = new File(getBaseDir(), name);
        XmlModel x = assertRoutes(file, count, null);

        // now lets modify the xml
        List<RouteDefinition> definitionList = x.getRouteDefinitionList();
        RouteDefinition route = new RouteDefinition().from("file:foo").to("file:bar");
        definitionList.add(route);

        System.out.println("Round tripped to: " + x);
        return x;
    }

}
