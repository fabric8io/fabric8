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
package io.fabric8.forge.rest.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class Models {
    private static ObjectMapper objectMapper = createObjectMapper();

    /**
     * Creates a configured Jackson object mapper for parsing JSON
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Saves the json object to the given file
     */
    public static void saveJson(File json, Object object) throws IOException {
        objectMapper.writer().writeValue(json, object);
    }

    /**
     * Saves the json object to the given file
     */
    public static <T> List<T> loadJsonValues(File json, Class<T> clazz) throws IOException {
        List<T> answer = new ArrayList<>();
        if (json.exists() && json.isFile()) {
            MappingIterator<T> iter = objectMapper.readerFor(clazz).readValues(json);
            while (iter.hasNext()) {
                answer.add(iter.next());
            }
        }
        return answer;
    }

    public static String toJson(Object dto) throws JsonProcessingException {
        Class<?> clazz = dto.getClass();
        return objectMapper.writerFor(clazz).writeValueAsString(dto);
    }

}
