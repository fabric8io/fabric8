/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.profiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ProfilesHelpers {
    public static final String DELETED = "#deleted#";
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static Properties readPropertiesFile(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            properties.load(is);
        }
        return properties;
    }


    public static JsonNode readJsonFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return JSON_MAPPER.readTree(is);
        }
    }

    public static JsonNode readYamlFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return YAML_MAPPER.readTree(is);
        }
    }

    public static byte[] toBytes(Properties properties) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            properties.store(os, null);
            return os.toByteArray();
        }
    }

    public static byte[] toYamlBytes(JsonNode yaml) throws IOException {
        return YAML_MAPPER.writeValueAsBytes(yaml);
    }

    public static byte[] toJsonBytes(JsonNode yaml) throws IOException {
        return JSON_MAPPER.writeValueAsBytes(yaml);
    }

    public static void recusivelyCollectFileListing(ArrayList<String> rc, Path base, Path directory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    recusivelyCollectFileListing(rc, base, path);
                } else {
                    rc.add(base.relativize(path).toString());
                }
            }
        }
    }

    public static void merge(Properties target, Properties source) {
        if( source.contains(DELETED) ) {
            target.clear();
        } else {
            for (Map.Entry<Object, Object> entry : source.entrySet()) {
                if (DELETED.equals(entry.getValue())) {
                    target.remove(entry.getKey());
                } else {
                    target.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static JsonNode merge(JsonNode target, JsonNode source) {
        if( target == null ) {
            return source;
        }
        if( target.isArray() && source.isArray() ) {
            // we append values from the source.
            ArrayNode copy = (ArrayNode) target.deepCopy();
            for (JsonNode n : source) {
                if( (n.isTextual() && DELETED.equals(n.textValue())) ) {
                    copy = JsonNodeFactory.instance.arrayNode();
                } else {
                    copy.add(n);
                }
            }
            return copy;
        } else if ( target.isObject() && source.isObject() ) {
            ObjectNode copy = (ObjectNode) target.deepCopy();
            if( source.get(DELETED)!=null ) {
                copy = JsonNodeFactory.instance.objectNode();
            } else {
                Iterator<String> iterator = source.fieldNames();
                while (iterator.hasNext()) {
                    String key =  iterator.next();
                    if( !DELETED.equals(key) ) {
                        JsonNode value = source.get(key);
                        if( (value.isTextual() && DELETED.equals(value.textValue())) ) {
                            copy.remove(key);
                        } else {
                            JsonNode original = target.get(key);
                            value = merge(original, value);
                            copy.set(key, value);
                        }
                    }
                }
            }
            return copy;
        } else {
            return source;
        }

    }

}
