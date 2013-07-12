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

package org.fusesource.fabric.api;

public final class CreateContainerOptionsBuilder {

    private CreateContainerOptionsBuilder() {
        //Utility Class
    }

    public static CreateJCloudsContainerOptions.Builder jclouds() {
        return CreateJCloudsContainerOptions.builder();
    }

    public static CreateSshContainerOptions.Builder ssh() {
        return CreateSshContainerOptions.builder();
    }

    public static CreateChildContainerOptions.Builder child() {
        return CreateChildContainerOptions.builder();
    }

    public static CreateContainerBasicOptions.Builder basic() {
        return CreateContainerBasicOptions.builder();
    }

    public static ContainerOptions.Builder type(String type) {
        if ("child".equals(type)) {
            return child();
        } else if ("ssh".equals(type)) {
            return ssh();
        } else if ("jclouds".equals(type)) {
            return jclouds();
        } else {
            return basic();
        }
    }
}
