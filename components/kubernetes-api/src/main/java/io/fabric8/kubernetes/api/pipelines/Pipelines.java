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

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.util.Arrays;
import java.util.Map;

import static io.fabric8.kubernetes.api.environments.Environments.findSpaceNamespace;

/**
 */
public class Pipelines {
    public static final String PIPELINE_KIND = "PIPELINE_KIND";
    public static final String JOB_NAME = "JOB_NAME";

    private static final transient Logger LOG = LoggerFactory.getLogger(Pipelines.class);

    /**
     * Looks up the pipeline kind based on the configuration in the given kubernetes namespace.
     * <p>
     * <b>NOTE</b> that you should pass in the <code>BRANCH_NAME</code> and <code>GIT_URL</code> environment variables
     * so that this function can properly detect if a build should be a <code>CD</code> build or not!
     */
    public static Pipeline getPipeline(Map<String, String> jobEnvironment) throws IntrospectionException {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            String namespace = findSpaceNamespace(kubernetesClient);
            return getPipeline(kubernetesClient, namespace, jobEnvironment);
        }
    }
    /**
     * Looks up the pipeline kind based on the configuration in the given kubernetes namespace.
     * <p>
     * <b>NOTE</b> that you should pass in the <code>BRANCH_NAME</code> and <code>GIT_URL</code> environment variables
     * so that this function can properly detect if a build should be a <code>CD</code> build or not!
     */
    public static Pipeline getPipeline(String namespace, Map<String, String> jobEnvironment) throws IntrospectionException {
        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            return getPipeline(kubernetesClient, namespace, jobEnvironment);
        }
    }

    /**
     * Looks up the pipeline kind based on the configuration in the given kubernetes namespace.
     * <p>
     * <b>NOTE</b> that you should pass in the <code>BRANCH_NAME</code> and <code>GIT_URL</code> environment variables
     * so that this function can properly detect if a build should be a <code>CD</code> build or not!
     */
    public static Pipeline getPipeline(KubernetesClient kubernetesClient, String namespace, Map<String, String> jobEnvironment) throws IntrospectionException {
        String kind = jobEnvironment.get(PIPELINE_KIND);
        String jobName = jobEnvironment.get(JOB_NAME);
        if (Strings.isNotBlank(jobName) && Strings.isNotBlank(kind)) {
            try {
                PipelineKind pipelineKind = PipelineKind.valueOf(kind);
                return new Pipeline(pipelineKind, jobName);
            } catch (IllegalArgumentException e) {
                LOG.warn("$PIPELINE_KIND has a value of " + kind +
                        " which is not a valid value. Available values are: " + Arrays.asList(PipelineKind.values()) + ". " + e, e);
            }
        }
        PipelineConfiguration configuration = PipelineConfiguration.loadPipelineConfiguration(kubernetesClient, namespace);
        Pipeline pipeline = configuration.getPipeline(jobEnvironment);

        // lets update the environment with the new pipeline so we can avoid querying the ConfigMap next time we try create this object
        jobEnvironment.put(PIPELINE_KIND, pipeline.getKind().toString());
        return pipeline;
    }

    /**
     * Looks up the pipeline kind based on the configuration in the given kubernetes namespace.
     * <p>
     * <b>NOTE</b> that you should pass in the <code>BRANCH_NAME</code> and <code>GIT_URL</code> environment variables
     * so that this function can properly detect if a build should be a <code>CD</code> build or not!
     */
    public static Pipeline getPipeline(KubernetesClient kubernetesClient, String namespace, JobEnvironment jobEnvironment) throws IntrospectionException {
        PipelineConfiguration configuration = PipelineConfiguration.loadPipelineConfiguration(kubernetesClient, namespace);
        return configuration.getPipeline(jobEnvironment);
    }
}
