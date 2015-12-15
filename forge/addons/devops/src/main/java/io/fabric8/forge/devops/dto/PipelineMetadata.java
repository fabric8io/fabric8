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
package io.fabric8.forge.devops.dto;

import java.util.List;

/**
 * Metadata about a Pipeline we store as a YAML file inside the Workflow library
 */
public class PipelineMetadata {
    private List<String> stages;
    private List<String> environments;

    public void configurePipeline(PipelineDTO pipeline) {
        if (stages != null) {
            pipeline.setStages(stages);
        }
        if (environments != null) {
            pipeline.setEnvironments(environments);
        }
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
