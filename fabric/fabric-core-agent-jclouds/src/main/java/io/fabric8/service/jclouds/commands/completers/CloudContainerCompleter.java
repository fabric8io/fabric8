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

package io.fabric8.service.jclouds.commands.completers;

import io.fabric8.api.Container;
import io.fabric8.boot.commands.support.ContainerCompleter;

public class CloudContainerCompleter extends ContainerCompleter {

    @Override
    public boolean apply(Container container) {
        if (container != null && container.getMetadata() != null
                && container.getMetadata().getCreateOptions() != null
                && container.getMetadata().getCreateOptions().getProviderType().equals("jclouds")) {
            return true;
        }
        else return false;
    }
}
