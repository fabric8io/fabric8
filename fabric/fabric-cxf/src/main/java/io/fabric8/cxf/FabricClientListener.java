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
package io.fabric8.cxf;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleListener;

public class FabricClientListener implements ClientLifeCycleListener {
    private final FabricLoadBalancerFeature feature;

    public FabricClientListener(FabricLoadBalancerFeature feature) {
        this.feature = feature;
    }

    @Override
    public void clientCreated(Client client) {
        feature.setupClientConduitSelector(client);
    }

    @Override
    public void clientDestroyed(Client client) {
        // we don't need to anything here
    }
}
