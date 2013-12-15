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
package io.fabric8.fab.osgi.internal;

import org.junit.Test;
import org.osgi.framework.Constants;

import static org.junit.Assert.fail;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

/**
 * Test cases for {@link BndUtils} and the custom Bnd plugins it encapsulates
 */
public class BndUtilsTest {

    @Test
    public void testActiveMQNamespaceElements() throws Exception {
        HashSet<String> actualImports = new HashSet<String>();

        InputStream stream =
                bundle().set(Constants.BUNDLE_SYMBOLICNAME, "some.bundle.symbolic.name")
                        .add("META-INF/spring/activemq-camel.xml", getClass().getResource("activemq-camel.xml"))
                        .build();

        Properties instructions = new Properties();
        instructions.put("Import-Package", "*");

        BndUtils.createBundle(stream, instructions, "some.url.like.thingy",
                              OverwriteMode.KEEP, Collections.EMPTY_MAP,  Collections.EMPTY_MAP,
                              actualImports, null );

        assertContainsAll(actualImports,
                          "org.apache.activemq.camel.component",
                          "org.apache.xbean.spring.context.v2",
                          "org.apache.activemq.broker",
                          "org.apache.activemq.xbean");
    }

    @Test
    public void testCXFSpringImports() throws Exception {
        HashSet<String> actualImports = new HashSet<String>();

        InputStream stream =
                bundle().set(Constants.BUNDLE_SYMBOLICNAME, "some.bundle.symbolic.name")
                        .add("META-INF/spring/cxf-example.xml", getClass().getResource("cxf-example.xml"))
                        .build();

        Properties instructions = new Properties();
        instructions.put("Import-Package", "*");

        BndUtils.createBundle(stream, instructions, "some.url.like.thingy",
                OverwriteMode.KEEP, Collections.EMPTY_MAP,  Collections.EMPTY_MAP,
                actualImports, null );

        assertContainsAll(actualImports, "META-INF.cxf", "org.apache.cxf.bus.spring");
    }

    private<T> void assertContainsAll(Collection<T> collection, T... items) {
        for (T item : items) {
            if (!collection.contains(item)) {
                fail(String.format("Item %s not found in collection %s", item, collection));
            }
        }
    }

}
