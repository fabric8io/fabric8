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
package io.fabric8.dosgi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import io.fabric8.dosgi.impl.Manager;
import org.osgi.framework.BundleContext;

public class Activator implements ConnectionStateListener {

    private BundleContext bundleContext;
    private Manager manager;
    private String uri;
    private String exportedAddress;
    private long timeout = TimeUnit.MINUTES.toMillis(5);
    private CuratorFramework curator;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setExportedAddress(String exportedAddress) {
        this.exportedAddress = exportedAddress;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void destroy() {
        destroyManager();
        curator = null;
    }

    protected void destroyManager() {
        if (manager != null) {
            Manager mgr = manager;
            manager = null;
            try {
                mgr.destroy();
            } catch (IOException e) {
                //ignore
            }
        }
    }


    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                this.curator = client;
                onConnected();
                break;
            default:
                onDisconnected();
        }
    }

    public void onConnected() {
        destroyManager();
        try {
            manager = new Manager(this.bundleContext, curator, uri, exportedAddress, timeout);
            manager.init();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start DOSGi service: " + e.getMessage(), e);
        }
    }

    public void onDisconnected() {
        destroyManager();
    }
}
