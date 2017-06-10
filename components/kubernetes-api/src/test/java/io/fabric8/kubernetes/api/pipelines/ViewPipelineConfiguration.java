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

import java.beans.IntrospectionException;

/**
 */
public class ViewPipelineConfiguration {
    public static void main(String[] args) {
        try {
            PipelineConfiguration pipeline;
            if (args.length > 0) {
                pipeline = PipelineConfiguration.loadPipelineConfiguration(args[0]);
            } else {
                pipeline = PipelineConfiguration.loadPipelineConfiguration();
            }

            System.out.println("Main namespace:                      " + pipeline.getSpaceNamespace());
            System.out.println("Job Name to kind map:                " + pipeline.getJobNameToKindMap());
            System.out.println("CI branch patterns:                  " + pipeline.getCiBranchPatterns());
            System.out.println("CD branch patterns:                  " + pipeline.getCdBranchPatterns());
            System.out.println("CD git organisation branch patterns: " + pipeline.getCdGitHostAndOrganisationToBranchPatterns());
            System.out.println("Disable ITests for CD:               " + pipeline.isDisableITestsCD());
            System.out.println("Disable ITests for CI:               " + pipeline.isDisableITestsCI());
        } catch (Exception e) {
            System.out.println("Failed with: " + e);
            e.printStackTrace();
        }

    }
}
