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
package org.fusesource.camel.tooling.util;

import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.FilterDefinition;
import org.apache.camel.model.MarshalDefinition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class CamelModelUtilsTest {
    @Test
    public void testCanImport() throws Exception {
        assertCanAcceptInput(MarshalDefinition.class, false);
        assertCanAcceptInput(BeanDefinition.class, false);
        assertCanAcceptInput(FilterDefinition.class, true);
    }

    protected void assertCanAcceptInput(Class<?> aClass, boolean expected) {
        String name = aClass.getName();
        boolean actual = CamelModelUtils.canAcceptInput(name);
        assertEquals("canAcceptInput for " + name, expected, actual);
    }

}
