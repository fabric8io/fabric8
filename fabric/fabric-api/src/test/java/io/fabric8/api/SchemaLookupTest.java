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
package io.fabric8.api;

import io.hawt.jsonschema.SchemaLookup;
import org.junit.Test;

/**
 * Asserts we can lookup schemas for example for tools like hawtio
 */
public class SchemaLookupTest {
    protected static SchemaLookup schemaLookup = SchemaLookup.getSingleton();

    @Test
    public void testLookupSchemas() throws Exception {
        assertLookupSchemaClasses(CreateChildContainerOptions.class);
    }

    public static void assertLookupSchemaClasses(Class<?>... clazzes) {
        for (Class<?> clazz : clazzes) {
            String json = schemaLookup.getSchemaForClass(clazz);
            System.out.println("Lookup schema for : " + clazz.getCanonicalName());
            System.out.println(json);
            System.out.println();
        }
    }

}
