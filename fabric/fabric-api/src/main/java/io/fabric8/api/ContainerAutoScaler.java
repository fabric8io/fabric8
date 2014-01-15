/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import java.util.List;

/**
 * Provides the mechanism to create or destroy containers
 * using a cloud which supports scaling such as OpenShift
 */
public interface ContainerAutoScaler {

    /**
     * Returns a weighting to help choose which auto-scaler to use.
     *
     * e.g. if OpenShift is available it should be used; then docker, then finally if there is no other option, child.
     */
    int getWeight();


    /**
     * Creates the given number of containers of the given profile
     */
    void createContainers(String version, String profile, int count) throws Exception;

    /**
     * Destroy a number of containers from the given list of containers
     */
    void destroyContainers(String profile, int count, List<Container> containers) throws Exception;
}
