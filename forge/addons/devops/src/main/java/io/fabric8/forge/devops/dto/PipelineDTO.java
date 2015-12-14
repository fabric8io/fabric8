/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.devops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PipelineDTO {
    private String label;
    private String value;
    private String descriptionMarkdown;
    private String builder;
    private List<String> stages;
    private List<String> environments;

    public PipelineDTO() {
    }

    public PipelineDTO(String value, String label, String builder, String descriptionMarkdown) {
        this.value = value;
        this.label = label;
        this.builder = builder;
        this.descriptionMarkdown = descriptionMarkdown;
    }

    @Override
    public String toString() {
        return "PipelineDTO{" +
                "value='" + value + '\'' +
                '}';
    }

    public String getBuilder() {
        return builder;
    }

    public void setBuilder(String builder) {
        this.builder = builder;
    }

    public String getDescriptionMarkdown() {
        return descriptionMarkdown;
    }

    public void setDescriptionMarkdown(String descriptionMarkdown) {
        this.descriptionMarkdown = descriptionMarkdown;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<String> environments) {
        this.environments = environments;
    }

    public List<String> getStages() {
        return stages;
    }

    public void setStages(List<String> stages) {
        this.stages = stages;
    }
}
