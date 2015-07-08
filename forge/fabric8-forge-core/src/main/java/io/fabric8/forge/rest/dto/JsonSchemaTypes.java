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

import org.jboss.forge.addon.projects.ProjectProvider;
import org.jboss.forge.addon.projects.ProjectType;
import org.jboss.forge.addon.resource.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Java types to JSON Schema types
 */
public class JsonSchemaTypes {
    protected static Map<String,String> javaToJsonSchemaTypes = new HashMap<>();

    static {
        addTypeAliases("boolean", Boolean.class.getName(), "boolean");
        addTypeAliases("integer", Byte.class.getName(), Character.class.getName(),
                Short.class.getName(), Integer.class.getName(), Long.class.getName(),
                "byte", "char", "short", "int", "long");
        addTypeAliases("number", Float.class.getName(), Double.class.getName(), "float", "double");
        addTypeAliases("string", String.class.getName());
    }


    protected static void addTypeAliases(String jsonSchemaType, String... javaTypeNames) {
        for (String javaTypeName : javaTypeNames) {
            javaToJsonSchemaTypes.put(javaTypeName, jsonSchemaType);
        }
    }

    public static String getJsonSchemaTypeName(Class clazz) {
        if (clazz == null) {
            return null;
        }
        if (ProjectProvider.class.isAssignableFrom(clazz) || ProjectType.class.isAssignableFrom(clazz)) {
            return "string";
        }
        if (Resource.class.isAssignableFrom(clazz)) {
            // TODO return file?
            return "string";
        }
        return getJsonSchemaTypeName(clazz.getName());
    }

    public static String getJsonSchemaTypeName(String javaTypeName) {
        String answer =  javaToJsonSchemaTypes.get(javaTypeName);
        return answer != null ? answer : "string";
    }

}
