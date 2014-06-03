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
package io.fabric8.deployer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.insight.log.support.Strings;

/**
 * A helper class for working with the DTO marshalling with JSON
 */
public class DtoHelper {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    /**
     * Returns the object mapper used for the DTOs
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Returns the file name of the JSON file stored in the profile for the dependencies
     */
    public static String getRequirementsConfigFileName(ProjectRequirements requirements) {
        StringBuilder builder = new StringBuilder("dependencies/");
        String groupId = requirements.getGroupId();
        if (!Strings.isEmpty(groupId)) {
            builder.append(groupId);
            builder.append("/");
        }
        String artifactId = requirements.getArtifactId();
        if (!Strings.isEmpty(artifactId)) {
            builder.append(artifactId);
            builder.append("-");
        }
        builder.append("requirements.json");
        return builder.toString();
    }
}
