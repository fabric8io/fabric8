/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven.support;

import io.fabric8.maven.JsonMojo;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class SecretNameParserTest {
    @Test
    public void testSecretParser() throws Exception {
        assertParseSecretNames("foo[a,b],bar[c,d]", "foo", "bar");
        assertParseSecretNames("cheese,foo[a,b],bar[c,d],another", "cheese", "foo", "bar", "another");
        assertParseSecretNames("foo", "foo");
        assertParseSecretNames("foo,bar", "foo", "bar");
        assertParseSecretNames("foo,bar,xyz", "foo", "bar", "xyz");
        assertParseSecretNames("foo[bar,xyz]", "foo");
    }

    public static void assertParseSecretNames(String text, String... values) {
        List<String> expected = Arrays.asList(values);
        List<String> actual = JsonMojo.parseSecretNames(text);
        assertThat(actual).describedAs("parsing '" + text + "'").isEqualTo(expected);
    }
}
