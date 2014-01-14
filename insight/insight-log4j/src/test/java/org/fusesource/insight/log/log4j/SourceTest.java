/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.insight.log.log4j;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SourceTest {
    private Log4jLogQuery logQuery = new Log4jLogQuery();
    protected String mavenCoords = "org.apache.camel:camel-core:2.10.3";

    @Before
    public void init() {
        logQuery.start();
    }

    @After
    public void destroy() {
        logQuery.stop();
    }

    @Test
    public void testSourceDownload() throws Exception {
        String expectedContent = "CamelContext";
        assertSourceContains(mavenCoords, "org.apache.camel.CamelContext", "/org/apache/camel/CamelContext.java", expectedContent);
        assertSourceContains(mavenCoords, "org.apache.camel.CamelContext", "CamelContext.java", expectedContent);
        assertSourceContains(mavenCoords, "org.apache.camel.CamelContext", "", expectedContent);

        // now lets try a space separated version
        String paxCoords = "org.ops4j.base:ops4j-base-lang:1.2.3 org.ops4j.base:ops4j-base-util-collections:1.2.3 org.ops4j.base:ops4j-base-util-property:1.2.3 org.ops4j.base:ops4j-base-util-xml:1.2.3 org.ops4j.pax.swissbox:pax-swissbox-core:1.4.0 org.ops4j.pax.swissbox:pax-swissbox-lifecycle:1.4.0 org.ops4j.pax.swissbox:pax-swissbox-optional-jcl:1.4.0 org.ops4j.pax.swissbox:pax-swissbox-property:1.4.0 org.ops4j.pax.web:pax-web-api:1.1.11 org.ops4j.pax.web:pax-web-runtime:1.1.11 org.ops4j.pax.web:pax-web-spi:1.1.11";

        String content = assertSourceContains(paxCoords, "org.ops4j.pax.web.service.internal.HttpServiceFactoryImpl", "HttpServiceFactoryImpl.java", "HttpServiceFactoryImpl");
        //System.out.println(content);


        // now lets get an index of the files
        assertSourceContains(mavenCoords, "", "/", "org/apache/camel/CamelContext.java");
    }

    @Test
    public void testJavaDocDownload() throws Exception {
        assertJavaDocContains(mavenCoords, "index.html", "Package, class and interface descriptions");
        assertJavaDocContains(mavenCoords, "org/apache/camel/CamelContext.html", "CamelContext");
    }


    protected String assertSourceContains(String mavenCoords, String className, String path, String expectedContent) throws IOException {
        String content = logQuery.getSource(mavenCoords, className, path);
        //System.out.println("Found content: " + content);
        assertTrue("content should contain '" + expectedContent + "' but was: " + content, content.indexOf(expectedContent) > 0);
        return content;
    }

    protected String assertJavaDocContains(String mavenCoords, String path, String expectedContent) throws IOException {
        String content = logQuery.getJavaDoc(mavenCoords, path);
        //System.out.println("Found content: " + content);
        assertTrue("content should contain '" + expectedContent + "' but was: " + content, content.indexOf(expectedContent) > 0);
        return content;
    }
}
