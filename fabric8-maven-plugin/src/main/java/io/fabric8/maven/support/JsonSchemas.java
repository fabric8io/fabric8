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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

/**
 */
public class JsonSchemas {
    public static final String ENVIRONMENT_SCHEMA_FILE = "io/fabric8/environment/schema.json";
    protected static ObjectMapper objectMapper = createObjectMapper();

    /**
     * Finds all of the environment json schemas and combines them together
     */
    public static JsonSchema loadEnvironmentSchemas(ClassLoader classLoader, String... folderPaths) throws IOException {
        JsonSchema answer = null;
        Enumeration<URL> resources = classLoader.getResources(ENVIRONMENT_SCHEMA_FILE);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            JsonSchema schema = loadSchema(url);
            answer = combineSchemas(answer, schema);
        }
        for (String folderPath : folderPaths) {
            File file = new File(folderPath, ENVIRONMENT_SCHEMA_FILE);
            if (file.isFile()) {
                JsonSchema schema = loadSchema(file);
                answer = combineSchemas(answer, schema);
            }
        }
        return answer;
    }

    protected static JsonSchema combineSchemas(JsonSchema schema1, JsonSchema schema2) {
        if (schema1 == null) {
            return schema2;
        }
        if (schema2 != null) {
            Map<String, JsonSchemaProperty> properties2 = schema2.getProperties();
            Map<String, JsonSchemaProperty> properties1 = schema1.getProperties();
            if (properties2 != null) {
                if (properties1 == null) {
                    return schema2;
                } else {
                    properties1.putAll(properties2);
                }
            }
        }
        return schema1;
    }

    public static JsonSchema loadSchema(URL url) throws IOException {
        return objectMapper.reader(JsonSchema.class).readValue(url);
    }

    public static JsonSchema loadSchema(File file) throws IOException {
        return objectMapper.reader(JsonSchema.class).readValue(file);
    }

    public static JsonSchema loadSchema(InputStream inputStream) throws IOException {
        return objectMapper.reader(JsonSchema.class).readValue(inputStream);
    }

    public static JsonSchema loadSchema(byte[] data) throws IOException {
        return objectMapper.reader(JsonSchema.class).readValue(data);
    }

    /**
     * Creates a configured Jackson object mapper for parsing JSON
     */
    protected static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

}
