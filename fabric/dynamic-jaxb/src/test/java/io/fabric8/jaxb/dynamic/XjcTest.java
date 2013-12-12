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
package io.fabric8.jaxb.dynamic;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.bind.JAXBContext;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class XjcTest  {
    private static final transient Logger LOG = LoggerFactory.getLogger(XjcTest.class);

    protected DynamicXJC xjc = new DynamicXJC(XjcTest.class.getClassLoader());

    @Test
    public void testGeneratesClassesForXsd() throws Exception {
        String url = getSchemaURL("xsds/invoice.xsd");

        xjc.addSchemaUrl(url);

        CompileResults results = xjc.compileSchemas();
        JAXBContext jaxbContext = results.getJAXBContext();
        LOG.info("Got a JAXBContext! " + jaxbContext);

        ClassLoader classLoader = results.getClassLoader();
        assertLoadClasses(classLoader, "org.apache.invoice.Invoice", "org.apache.invoice.Address");
    }

    public static String getSchemaURL(String path) throws MalformedURLException {
        String url;
        URL resource = XjcTest.class.getClassLoader().getResource(path);
        if (resource != null) {
            url = resource.toString();
        } else {
            url = new File("src/test/resources/" + path).toURI().toURL().toString();
        }
        LOG.info("Using system ID for schema: " + url);
        return url;
    }

    public static void assertLoadClasses(ClassLoader classLoader, String... classNames) {
        for (String className : classNames) {
            try {
                Class<?> aClass = classLoader.loadClass(className);
                LOG.info("Loaded class " + className + " value: " + aClass + " from class loader: " + classLoader);
                assertNotNull("No class returned for " + className + " in class loader " + classLoader,
                        aClass);
            } catch (ClassNotFoundException e) {
                fail("Could not load class " + className + " from class loader " + classLoader);
            }
        }
    }
}
