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
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.springframework.util.FileCopyUtils;
import org.xml.sax.SAXParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class RouteXmlTestSupport {

    private String baseDir;
    protected File outDir = new File(getBaseDir(), "target/test-" + getClass().getSimpleName());
    protected RouteXml tool = new RouteXml();

    public String getBaseDir() {
        if (baseDir == null) {
            baseDir = System.getProperty("basedir", ".");
        }
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    protected XmlModel assertLoadModel(File file, int expected) throws Exception {
        System.out.println("Loading file: " + file);
        assertFileExists(file);

        XmlModel x = tool.unmarshal(file);

        System.out.println("Got: " + x);

        List<RouteDefinition> routes = x.getRouteDefinitionList();

        assertEquals("routes: " + routes, expected, routes.size());

        System.out.println("routes: " + routes);
        return x;
    }

    protected XmlModel assertRoutes(File file, int expected, String ns) throws Exception {
        if (ns == null || ns.trim().length() == 0) {
            ns = CamelNamespaces.springNS;
        }
        XmlModel x = assertLoadModel(file, expected);

        // now lets add a route and write it back again...
        DefaultCamelContext tmpContext = new DefaultCamelContext();
        tmpContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:newFrom").to("seda:newTo");
            }
        });
        x.getContextElement().getRoutes().addAll(tmpContext.getRouteDefinitions());

        List<RouteDefinition> routes = x.getRouteDefinitionList();
        assertEquals("routes: " + routes, expected + 1, routes.size());

        // now lets write to XML      model
        outDir.mkdirs();
        File outFile = new File(outDir, file.getName());
        System.out.println("Generating file: " + outFile);
        tool.marshal(outFile, x);

        assertFileExists(outFile);

        // lets check the file has the correct namespace inside it
        String text = FileCopyUtils.copyToString(new FileReader(outFile));
        assertTrue("Namespace " + ns + " not present in output file\n" + text, text.contains(ns));

        return x;
    }

    protected void assertFileExists(File file) {
        assertTrue("file should exist: " + file, file.exists());
    }

    protected void assertValid(XmlModel x) {
        ValidationHandler handler;
        try {
            handler = x.validate();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        List<SAXParseException> errors = handler.getErrors();
        assertTrue("errors were: " + errors, errors.size() == 0);
    }

}
