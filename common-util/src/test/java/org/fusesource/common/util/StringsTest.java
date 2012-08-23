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
package org.fusesource.common.util;

import org.junit.Test;

import static org.fusesource.common.util.Strings.*;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link org.fusesource.common.util.Strings}
 */
public class StringsTest {
    
    @Test
    public void testUnquote() {
        assertEquals("test", unquote("test"));
        assertEquals("test", unquote("\"test\""));
        assertEquals("te\"st", unquote("te\"st"));
        assertEquals("", unquote("\"\""));
        assertEquals(null, unquote(null));
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("one", defaultIfEmpty("one", "two"));
        assertEquals("two", defaultIfEmpty("", "two"));
        assertEquals("two", defaultIfEmpty(null, "two"));
    }
}
