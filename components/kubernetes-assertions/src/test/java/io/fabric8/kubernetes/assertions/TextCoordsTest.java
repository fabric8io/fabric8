/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.kubernetes.assertions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class TextCoordsTest {
    @Test
    public void testCoords() throws Exception {
        assertTextCoords("a", "1:2");
        assertTextCoords("a\n", "2:1");
        assertTextCoords("a\nabc\ndef", "3:4");
    }

    public static void assertTextCoords(String text, String expected) {
        String actual = PodLogsAssert.textCoords(text);
        assertEquals("textCoords(" + text + ")", expected, actual);
    }

}
