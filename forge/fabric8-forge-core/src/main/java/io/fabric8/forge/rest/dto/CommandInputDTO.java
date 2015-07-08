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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CommandInputDTO {
    private final CommandInfoDTO info;
    private Map<String,PropertyDTO> properties = new LinkedHashMap<>();
    private List<String> required = new ArrayList<>();

    public CommandInputDTO(CommandInfoDTO info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "CommandInputDTO{" +
                "info=" + info +
                ", properties=" + properties +
                '}';
    }

    public void addProperty(String key, PropertyDTO dto) {
        properties.put(key, dto);
        if (dto.isRequired()) {
            required.add(key);
        }
    }

    public CommandInfoDTO getInfo() {
        return info;
    }

    public Map<String, PropertyDTO> getProperties() {
        return properties;
    }

    public List<String> getRequired() {
        return required;
    }
}
