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
package io.fabric8.process.spring.boot.registry;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.EMPTY_MAP;

public class InMemoryProcessRegistry implements ProcessRegistry {

    private final Map<String,Object> registry;

    public InMemoryProcessRegistry(Map<String,Object> registryContents) {
        registry = newHashMap(registryContents);
    }

    public InMemoryProcessRegistry() {
        this(EMPTY_MAP);
    }

    @Override
    public String readProperty(String key) {
        String property = System.getProperty(key);
        if(property != null) {
            return property;
        }
        Object value = registry.get(key);
        if(value == null) {
            return null;
        }
        return value.toString();
    }

}
