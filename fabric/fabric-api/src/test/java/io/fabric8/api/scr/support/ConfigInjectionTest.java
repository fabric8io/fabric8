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
package io.fabric8.api.scr.support;

import org.junit.Test;
import static org.junit.Assert.*;
import static io.fabric8.api.scr.support.ConfigInjection.*;
public class ConfigInjectionTest {

    @Test
    public void testNormalizeName() {
        assertEquals(null, normalizePropertyName(null));
        assertEquals("", normalizePropertyName(""));
        assertEquals("", normalizePropertyName("."));
        assertEquals("t", normalizePropertyName("t"));
        assertEquals("test", normalizePropertyName("test"));
        assertEquals("test", normalizePropertyName(".test"));
        assertEquals("test", normalizePropertyName("test."));
        assertEquals("test", normalizePropertyName(".test."));
        assertEquals("myTest", normalizePropertyName("my.test"));
        assertEquals("my", normalizePropertyName("my."));
    }
}
