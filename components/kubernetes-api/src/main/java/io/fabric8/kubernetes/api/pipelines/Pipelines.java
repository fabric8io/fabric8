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

import io.fabric8.kubernetes.client.KubernetesClient;

import java.beans.IntrospectionException;
import java.util.Map;

/**
 */
public class Pipelines {

    /**
     * Looks up the pipeline kind based on the configuration in the given kubernetes namespace.
     * <p>
     * <b>NOTE</b> that you should pass in the <code>BRANCH_NAME</code> and <code>GIT_URL</code> environment variables
     * so that this function can properly detect if a build should be a <code>CD</code> build or not!
     */
    public static Pipeline getPipeline(KubernetesClient kubernetesClient, String namespace, Map<String, String> jobEnvironment) throws IntrospectionException {
        PipelineConfiguration configuration = PipelineConfiguration.getPipelineConfiguration(kubernetesClient, namespace);
        return configuration.getPipeline(jobEnvironment);
    }

    /**
     * Looks up the pipeline kind based on the configuration in the given kubernetes namespace.
     * <p>
     * <b>NOTE</b> that you should pass in the <code>BRANCH_NAME</code> and <code>GIT_URL</code> environment variables
     * so that this function can properly detect if a build should be a <code>CD</code> build or not!
     */
    public static Pipeline getPipeline(KubernetesClient kubernetesClient, String namespace, JobEnvironment jobEnvironment) throws IntrospectionException {
        PipelineConfiguration configuration = PipelineConfiguration.getPipelineConfiguration(kubernetesClient, namespace);
        return configuration.getPipeline(jobEnvironment);
    }
}
