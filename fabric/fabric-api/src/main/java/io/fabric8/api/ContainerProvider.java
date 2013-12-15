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

/**
 * A Factory that creates {@link Container}.
 */
public interface ContainerProvider<O extends CreateContainerOptions, M extends CreateContainerMetadata> {

    /**
     * Creates a container using a set of arguments
     * @param options   The {@link CreateContainerOptions} that will be used to build the container.
     * @return          A set of {@link CreateContainerMetadata}, which contains information about the created container.
     * @throws Exception
     */
    M create(O options, CreationStateListener listener) throws Exception;

    /**
     * Start the container
     * @param container
     * @throws Exception
     */
    void start(Container container);

    /**
     * Stop the container
     * @param container
     * @throws Exception
     */
    void stop(Container container);

    /**
     * Destroy the container
     * @param container
     * @throws Exception
     */
    void destroy(Container container);


    String getScheme();

    /**
     * Returns the options
     * @return
     */
    Class<O> getOptionsType();
    Class<M> getMetadataType();


    /**
     * Helper method to create a new builder object
     */
    CreateContainerBasicOptions.Builder newBuilder();
}
