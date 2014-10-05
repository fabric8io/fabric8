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
import java.io.FileReader;
import java.util.Arrays;

import org.apache.camel.model.RouteDefinition;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.assertTrue;

public class StandAloneRoutesXmlMarshalToTextTest extends RouteXmlTestSupport {

    @Test
    public void testMarshalToText() throws Exception {
        String text = FileCopyUtils.copyToString(new FileReader(new File(getBaseDir(), "src/test/resources/routes.xml")));

        RouteDefinition route = new RouteDefinition();
        route.from("seda:new.in").to("seda:new.out");

        String actual = tool.marshalToText(text, Arrays.asList(route));
        System.out.println("Got " + actual);

        assertTrue("Missing seda:new.in for: " + actual, actual.contains("seda:new.in"));
    }

}
