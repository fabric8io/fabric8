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
package io.fabric8.forge.camel.commands.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProjectDto {
    private List<ComponentDto> components = new ArrayList<>();
    private List<EndpointDto> endpoints = new ArrayList<>();
    private List<LanguageDto> languages = new ArrayList<>();

    public void addEndpoints(Iterable<CamelEndpointDetails> details) {
        for (CamelEndpointDetails detail : details) {
            endpoints.add(new EndpointDto(detail));
        }
    }

    public List<EndpointDto> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointDto> endpoints) {
        this.endpoints = endpoints;
    }

    public List<ComponentDto> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentDto> components) {
        this.components = components;
    }

    public List<LanguageDto> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageDto> languages) {
        this.languages = languages;
    }
}
