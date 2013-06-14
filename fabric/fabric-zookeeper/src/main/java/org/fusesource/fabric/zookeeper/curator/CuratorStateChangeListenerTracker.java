
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fusesource.fabric.zookeeper.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.concurrent.ExecutorService;

public class CuratorStateChangeListenerTracker implements ServiceTrackerCustomizer<ConnectionStateListener, ConnectionStateListener> {


    private final BundleContext bundleContext;
    private final CuratorFramework curator;
    private final ExecutorService executor;

    public CuratorStateChangeListenerTracker(BundleContext bundleContext, CuratorFramework curator, ExecutorService executor) {
        this.bundleContext = bundleContext;
        this.curator = curator;
        this.executor = executor;
    }

    @Override
    public ConnectionStateListener addingService(ServiceReference<ConnectionStateListener> reference) {
        final ConnectionStateListener listener = bundleContext.getService(reference);
        if (curator.getZookeeperClient().isConnected()) {
            listener.stateChanged(curator, ConnectionState.CONNECTED);
        }
        curator.getConnectionStateListenable().addListener(listener, executor);
        return listener;
    }

    @Override
    public void modifiedService(ServiceReference<ConnectionStateListener> reference, ConnectionStateListener service) {
    }

    @Override
    public void removedService(ServiceReference<ConnectionStateListener> reference, final ConnectionStateListener service) {
        if (curator.getZookeeperClient().isConnected()) {
            service.stateChanged(curator, ConnectionState.LOST);
        }
        curator.getConnectionStateListenable().removeListener(service);
    }
}