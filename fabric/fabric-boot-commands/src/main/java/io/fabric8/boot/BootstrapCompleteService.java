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
package io.fabric8.boot;

import io.fabric8.api.BootstrapComplete;
import io.fabric8.api.ManagedCuratorFrameworkAvailable;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.git.GitService;
import io.fabric8.zookeeper.ACLManager;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component(immediate = true)
@Service({ BootstrapComplete.class })
public final class BootstrapCompleteService extends AbstractComponent implements BootstrapComplete {

    @Reference
    private ACLManager aclManagerAvailable;
    @Reference
    private GitService gitServiceAvailable;
    @Reference
    private ManagedCuratorFrameworkAvailable managedCuratorAvailable;
    @Reference
    private ZooKeeperClusterBootstrap zookeeperBootstrapAvailable;

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }
}
