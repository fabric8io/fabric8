/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class SafeKubernetesNameTest {
    @Test
    public void testSafeNamesWithDots() throws Exception {
        assertSafeKubernetesName("Foo.Bar", "foo.bar", true);
        assertSafeKubernetesName("Foo-Bar", "foo-bar", true);
        assertSafeKubernetesName("-Foo-Bar", "foo-bar", true);
        assertSafeKubernetesName(".-Foo-Bar", "foo-bar", true);
        assertSafeKubernetesName("foo-bar/whatnot", "foo-bar.whatnot", true);
        assertSafeKubernetesName("foo-bar123/whatnot1234", "foo-bar123.whatnot1234", true);
        assertSafeKubernetesName("foo-bar//whatnot", "foo-bar.whatnot", true);
        assertSafeKubernetesName("foo-bar/whatnot_", "foo-bar.whatnot", true);
        assertSafeKubernetesName("_*foo-bar/*wh!atnot)", "foo-bar.wh-atnot", true);
    }

    @Test
    public void testSafeNamesWithoutDots() throws Exception {
        assertSafeKubernetesName("Foo.Bar", "foo-bar", false);
        assertSafeKubernetesName("Foo-Bar", "foo-bar", false);
        assertSafeKubernetesName("-Foo-Bar", "foo-bar", false);
        assertSafeKubernetesName(".-Foo-Bar", "foo-bar", false);
        assertSafeKubernetesName("foo-bar/whatnot", "foo-bar-whatnot", false);
        assertSafeKubernetesName("foo-bar123/whatnot1234", "foo-bar123-whatnot1234", false);
        assertSafeKubernetesName("foo-bar//whatnot", "foo-bar-whatnot", false);
        assertSafeKubernetesName("foo-bar/whatnot_", "foo-bar-whatnot", false);
        assertSafeKubernetesName("_*foo-bar/*wh!atnot)", "foo-bar-wh-atnot", false);
    }

    public static void assertSafeKubernetesName(String text, String expected, boolean allowDots) {
        String actual = KubernetesNames.convertToKubernetesName(text, allowDots);
        //System.out.println("Converted `" + text + "` => `" + actual + "`");
        assertEquals("Safe name for `" + text + "`", expected, actual);
    }

}
