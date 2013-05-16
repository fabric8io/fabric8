
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
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CuratorStateChangeListenerTracker implements ServiceTrackerCustomizer {


    private final BundleContext bundleContext;
    private final CuratorFramework curator;
    private final ExecutorService executor;

    public CuratorStateChangeListenerTracker(BundleContext bundleContext, CuratorFramework curator, ExecutorService executor) {
        this.bundleContext = bundleContext;
        this.curator = curator;
        this.executor = executor;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = bundleContext.getService(reference);

        if (ConnectionStateListener.class.isAssignableFrom(service.getClass())) {
            final ConnectionStateListener listener = (ConnectionStateListener) service;
            if (curator.getZookeeperClient().isConnected()) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        listener.stateChanged(curator, ConnectionState.CONNECTED);
                    }
                });
            }
            curator.getConnectionStateListenable().addListener(listener, executor);

        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        if (ConnectionStateListener.class.isAssignableFrom(service.getClass())) {
            curator.getConnectionStateListenable().addListener((ConnectionStateListener) service, executor);
        }
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        if (ConnectionStateListener.class.isAssignableFrom(service.getClass())) {
            curator.getConnectionStateListenable().removeListener((ConnectionStateListener) service);
        }
    }
}