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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A DTO to load/save JSON schema
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonSchema {
    private String id;
    @JsonProperty("$schema")
    private String uri;
    private String type = "object";
    private Map<String,JsonSchemaProperty> properties = new HashMap<>();
    private Set<String> required = new LinkedHashSet<>();

    public JsonSchemaProperty getOrCreateProperty(String name) {
        JsonSchemaProperty property = properties.get(name);
        if (property == null) {
            property = new JsonSchemaProperty();
            properties.put(name, property);
        }
        return property;
    }

    public void addRequired(String property) {
        required.add(property);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, JsonSchemaProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, JsonSchemaProperty> properties) {
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(Set<String> required) {
        this.required = new LinkedHashSet<>(required);
    }
}
