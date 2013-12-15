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

package io.fabric8.fab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DependencyClassLoaderTest extends DependencyTestSupport {

    @Test
    public void testLoadSampleClassesInNormalPom() throws Exception {
        assertLoadSampleClasses("test-normal.pom");
    }

    @Test
    public void testLoadSampleClassesInOverrideClogging() throws Exception {
        assertLoadSampleClasses("test-override-clogging.pom");
    }

    @Test
    public void testLoadSampleClassesInOverrideSpring() throws Exception {
        assertLoadSampleClasses("test-override-spring.pom");
    }

    protected void assertLoadSampleClasses(String pomName) throws Exception {
        DependencyClassLoader classLoader = getClassLoaderForPom(pomName);

        assertLoadClasses(classLoader,
                "org.apache.camel.Service",
                "org.apache.camel.CamelContext",
                "org.apache.camel.impl.DefaultCamelContext",
                "org.apache.commons.logging.Log",
                "org.apache.commons.logging.LogFactory");
    }


}
