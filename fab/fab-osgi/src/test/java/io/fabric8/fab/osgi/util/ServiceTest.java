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
package io.fabric8.fab.osgi.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for {@link Service}
 */
public class ServiceTest {

    @Test
    public void testGetFilter() {
        Service noprops = new Service("some.class.Name");
        assertNull("Filter should be null", noprops.getFilter());

        Service oneprops = new Service("some.class.Name", Services.createProperties("key", "value"));
        assertEquals("(key=value)", oneprops.getFilter());

        Service twoprops = new Service("some.class.Name", Services.createProperties("key1", "value1", "key2", "value2"));
        assertEquals("(&(key1=value1)(key2=value2))", twoprops.getFilter());
    }
}
