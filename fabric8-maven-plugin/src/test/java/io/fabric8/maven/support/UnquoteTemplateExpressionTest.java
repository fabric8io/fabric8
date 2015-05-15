/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven.support;

import io.fabric8.maven.AbstractFabric8Mojo;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


/**
 */
public class UnquoteTemplateExpressionTest {
    @Ignore("Escaping of expressions in maven <properties> element in pom.xml not supported properly by maven")
    public void testUnquote() throws Exception {
        assertUnquoteTemplate("${FOO}", "${FOO}");
        assertUnquoteTemplate("\\${FOO}", "${FOO}");
        assertUnquoteTemplate("Hey \\${FOO} there \\${FOO}!", "Hey ${FOO} there ${FOO}!");
    }

    public static void assertUnquoteTemplate(String original, String expected) {
        String actual = AbstractFabric8Mojo.unquoteTemplateExpression(original);
        assertEquals("Unquoting of " + original, expected, actual);
    }

}
