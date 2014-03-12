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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.internal.ContainerImpl;

import java.io.Closeable;
import java.io.IOException;

public class ContainerProxy extends ContainerImpl implements Closeable {

    private ServiceProxy<FabricService> proxy;

    public static ContainerProxy wrap(Container container, ServiceProxy<FabricService> proxy) {
        if (container == null) {
            return null;
        }
        ContainerProxy parentProxy = wrap(container.getParent(), proxy);
        return new ContainerProxy(parentProxy, container.getId(), proxy);
    }

    private ContainerProxy(ContainerProxy parent, String id, ServiceProxy<FabricService> proxy) {
        super(parent, id, proxy.getService());
        this.proxy = proxy;

    }

    @Override
    public void stop(boolean force) {
        try {
            super.stop(force);
        } finally {
            proxy.close();
        }
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }
}
