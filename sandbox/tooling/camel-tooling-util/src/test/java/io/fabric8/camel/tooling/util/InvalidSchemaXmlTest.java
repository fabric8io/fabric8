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

import org.junit.Test;
import org.xml.sax.SAXParseException;

import static org.junit.Assert.assertEquals;

public class InvalidSchemaXmlTest extends RouteXmlTestSupport {

    @Test
    public void testParsesInvalidXmlSchemaFile() throws Exception {
        XmlModel m = assertLoadModel(new File(getBaseDir(), "src/test/resources/invalidSchemaRoute.xml"), 1);
        ValidationHandler status = m.validate();
        for (SAXParseException e : status.getErrors()) {
            System.out.println("Error: " + e.getMessage());
        }
        for (SAXParseException e : status.getFatalErrors()) {
            System.out.println("Fatal Error: " + e.getMessage());
        }
        assertEquals("Should have validation errors: " + status, true, status.hasErrors());
    }

}
