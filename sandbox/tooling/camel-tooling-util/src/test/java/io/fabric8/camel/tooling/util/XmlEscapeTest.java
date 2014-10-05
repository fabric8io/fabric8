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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class XmlEscapeTest {

    private static boolean verbose = true;

    @Test
    public void testXmlEscape() {
        roundTrip("this is plain text");
        roundTrip("<hello id=\"foo\">some text</hello>");
    }

    @Test
    public void testXmlUnescape() {
        String text = "&lt;hello id=&quot;a&quot;&gt;world!&lt;/hello&gt;";
        String actual = XmlHelper.unescape(text);
        System.out.println("text:     " + text);
        System.out.println("unescape: " + actual);
        assertThat(actual, equalTo("<hello id=\"a\">world!</hello>"));
    }

    protected void roundTrip(String text) {
        String escaped = XmlHelper.escape(text);
        String actual = XmlHelper.unescape(escaped);

        if (verbose) {
            System.out.println("text:     " + text);
            System.out.println("escape:   " + escaped);
            System.out.println("unescape: " + actual);
        }

        assertThat(text, equalTo(actual));
    }

}
