/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.pipelines;

/**
 */
public class Pipeline {
    private final PipelineKind kind;
    private final String jobName;
    private PipelineConfiguration configuration;

    public Pipeline(PipelineKind kind, String jobName) {
        this.kind = kind;
        this.jobName = jobName;
    }

    public boolean isCd() {
        return kind == PipelineKind.CD;
    }

    public boolean isCi() {
        return kind == PipelineKind.CI;
    }

    public boolean isDeveloper() {
        return kind == PipelineKind.Developer;
    }

    public String getJobName() {
        return jobName;
    }

    public PipelineKind getKind() {
        return kind;
    }

    public void setConfiguration(PipelineConfiguration configuration) {
        this.configuration = configuration;
    }

    public PipelineConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = new PipelineConfiguration();
        }
        return configuration;
    }
}
