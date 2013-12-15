/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.service.jclouds.functions;

import com.google.common.base.Optional;
import io.fabric8.service.jclouds.CreateJCloudsContainerMetadata;
import io.fabric8.service.jclouds.CreateJCloudsContainerOptions;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.LoginCredentials;

public class ToRunScriptOptions {

    private final ComputeService computeService;

    public ToRunScriptOptions(ComputeService computeService) {
        this.computeService = computeService;
    }

    public static ToRunScriptOptions withComputeService(ComputeService computeService) {
        return new ToRunScriptOptions(computeService);
    }

    public Optional<RunScriptOptions> apply(CreateJCloudsContainerMetadata containerMetadata) {
        CreateJCloudsContainerOptions options = containerMetadata.getCreateOptions();
        NodeMetadata nodeMetadata = computeService.getNodeMetadata(containerMetadata.getNodeId());
        LoginCredentials credentials = nodeMetadata.getCredentials();

        LoginCredentials.Builder loginBuilder;
        if (options.getUser() != null) {

            if (credentials == null) {
                loginBuilder = LoginCredentials.builder();
            } else {
                loginBuilder = credentials.toBuilder();
            }
            if (options.getPassword() != null) {
                credentials = loginBuilder.user(options.getUser()).password(options.getPassword()).build();
            } else {
                credentials = loginBuilder.user(options.getUser()).build();
            }
        }

        if (credentials != null) {
            return Optional.of(RunScriptOptions.Builder.overrideLoginCredentials(credentials).runAsRoot(false));
        } else {
            return Optional.absent();
        }
    }
}
