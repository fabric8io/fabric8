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
package io.fabric8.service;

import io.fabric8.api.DefaultRuntimeProperties;
import io.fabric8.api.RuntimeProperties;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.fabric8.service.ComponentConfigurer.*;
import static org.junit.Assert.*;

public class ComponentConfigurerTest {
    @Test
    public void testSubstitution() {


        Properties properties = new Properties();
        properties.put("key1", "runtime1");
        properties.put("key3", "runtime3");
        properties.put("key5", "runtime5");
        properties.put("key4", "keys\\key4");

        RuntimeProperties runtime = new DefaultRuntimeProperties(properties);
        assertEquals("", substitute("", runtime));
        assertEquals("runtime1", substitute("${key1}", runtime));
        assertEquals("", substitute("${key2}", runtime));
        assertEquals("runtime1-", substitute("${key1}-${key2}", runtime));
        assertEquals("keys\\key4", substitute("${key4}", runtime));
        assertEquals("", substitute("${key6}", runtime));

    }

}
