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
package io.fabric8.forge.rest.dto;

import io.fabric8.forge.rest.dto.UICommands;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ShellifyTest {
    @Test
    public void testShellify() {
        assertShellify("foo-bar", "foo-bar");
        assertShellify("Foo Bar", "foo-bar");
        assertShellify("Foo: Bar", "foo-bar");
        assertShellify("Foo Bar Thingy", "foo-bar-thingy");
        assertShellify("Foo: Bar Thingy", "foo-bar-thingy");
        assertShellify("Foo: Bar: Thingy", "foo-bar-thingy");
    }

    @Test
    public void testUnshellify() {
        assertUnshellify("Foo Bar", "Foo Bar");
        assertUnshellify("Foo-Bar", "Foo-Bar");
        assertUnshellify("foo-bar", "Foo: Bar");
        assertUnshellify("foo-bar-thingy", "Foo: Bar Thingy");
    }

    public static void assertShellify(String text, String expected) {
        String actual = UICommands.shellifyName(text);
        assertThat(actual).isEqualTo(expected);
    }

    public static void assertUnshellify(String text, String expected) {
        String actual = UICommands.unshellifyName(text);
        assertThat(actual).isEqualTo(expected);
    }

}
