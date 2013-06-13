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
package org.fusesource.fabric.camel;

import org.apache.camel.impl.DefaultComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.fusesource.fabric.groups2.Group;
import org.fusesource.fabric.groups2.internal.DelegateZooKeeperGroup;
import org.fusesource.fabric.groups2.internal.ZooKeeperGroup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public abstract class ZKComponentSupport extends DefaultComponent {
    private static final transient Log LOG = LogFactory.getLog(MasterComponent.class);
    private static final String ZOOKEEPER_URL = "zookeeper.url";
    private static final String ZOOKEEPER_PASSWORD = "zookeeper.password";

    private ManagedCurator managedCurator;
    private CuratorFramework curator;
    private boolean shouldCloseZkClient = false;
    private int maximumConnectionTimeout = 10 * 1000;
    private String zooKeeperUrl;
    private String zooKeeperPassword;

    public CuratorFramework getCurator() {
        if (managedCurator == null) {
            throw new IllegalStateException("Component is not started");
        }
        return managedCurator.getCurator();
    }

    public Group<CamelNodeState> createGroup(String path) {
        if (managedCurator == null) {
            throw new IllegalStateException("Component is not started");
        }
        return managedCurator.createGroup(path);
    }


    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public boolean isShouldCloseZkClient() {
        return shouldCloseZkClient;
    }

    public void setShouldCloseZkClient(boolean shouldCloseZkClient) {
        this.shouldCloseZkClient = shouldCloseZkClient;
    }

    public int getMaximumConnectionTimeout() {
        return maximumConnectionTimeout;
    }

    public void setMaximumConnectionTimeout(int maximumConnectionTimeout) {
        this.maximumConnectionTimeout = maximumConnectionTimeout;
    }


    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void setZooKeeperUrl(String zooKeeperUrl) {
        this.zooKeeperUrl = zooKeeperUrl;
    }

    public String getZooKeeperPassword() {
        return zooKeeperPassword;
    }

    public void setZooKeeperPassword(String zooKeeperPassword) {
        this.zooKeeperPassword = zooKeeperPassword;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (curator == null) {
            try {
                curator = (CuratorFramework) getCamelContext().getRegistry().lookupByName("curator");
            } catch (Exception exception) {
            }
        }
        if (managedCurator == null && curator != null) {
            LOG.debug("IZKClient found in camel registry. " + curator);
            managedCurator = new StaticManagedCurator();
        }
        if (managedCurator == null) {
            try {
                managedCurator = OsgiSupport.getTrackingManagedCurator();
            } catch (NoClassDefFoundError e) {
                // We're not in OSGi
            }
        }
        if (managedCurator == null) {
            String connectString = getZooKeeperUrl();
            if (connectString == null) {
                connectString = System.getProperty(ZOOKEEPER_URL, "localhost:2181");
            }
            String password = getZooKeeperPassword();
            if (password == null) {
                System.getProperty(ZOOKEEPER_PASSWORD);
            }
            LOG.debug("CuratorFramework not find in camel registry, creating new with connection " + connectString);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                                                                             .connectString(connectString)
                                                                             .retryPolicy(new RetryOneTime(1000))
                                                                             .connectionTimeoutMs(getMaximumConnectionTimeout());

            if (password != null && !password.isEmpty()) {
                builder.authorization("digest", ("fabric:"+password).getBytes());
            }

            CuratorFramework client = builder.build();
            LOG.debug("Starting curator " + curator);
            client.start();
            curator = client;
            setShouldCloseZkClient(true);
            managedCurator = new StaticManagedCurator();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (managedCurator != null) {
            managedCurator.close();
            managedCurator = null;
        }
    }

    static interface ManagedCurator {
        CuratorFramework getCurator();
        Group<CamelNodeState> createGroup(String path);
        void close();
    }

    class StaticManagedCurator implements ManagedCurator {

        @Override
        public CuratorFramework getCurator() {
            return curator;
        }

        @Override
        public Group<CamelNodeState> createGroup(String path) {
            return new ZooKeeperGroup<CamelNodeState>(curator, path, CamelNodeState.class);
        }

        @Override
        public void close() {
            if (isShouldCloseZkClient()) {
                curator.close();
            }
        }
    }

    static class OsgiSupport {

        static ManagedCurator getTrackingManagedCurator() {
            BundleContext context = FrameworkUtil.getBundle(ZKComponentSupport.class).getBundleContext();
            return new OsgiTrackingManagedCurator(context);
        }

        static class OsgiTrackingManagedCurator implements ManagedCurator, ServiceTrackerCustomizer<CuratorFramework, CuratorFramework> {

            private final BundleContext bundleContext;
            private final ServiceTracker<CuratorFramework, CuratorFramework> tracker;
            private CuratorFramework curator;
            private final List<DelegateZooKeeperGroup<CamelNodeState>> groups = new ArrayList<DelegateZooKeeperGroup<CamelNodeState>>();

            OsgiTrackingManagedCurator(BundleContext bundleContext) {
                this.bundleContext = bundleContext;
                this.tracker = new ServiceTracker<CuratorFramework, CuratorFramework>(
                        bundleContext, CuratorFramework.class, this);
                this.tracker.open();
            }

            @Override
            public CuratorFramework addingService(ServiceReference<CuratorFramework> reference) {
                CuratorFramework curator = OsgiTrackingManagedCurator.this.bundleContext.getService(reference);
                useCurator(curator);
                return curator;
            }

            @Override
            public void modifiedService(ServiceReference<CuratorFramework> reference, CuratorFramework service) {
            }

            @Override
            public void removedService(ServiceReference<CuratorFramework> reference, CuratorFramework service) {
                useCurator(null);
                OsgiTrackingManagedCurator.this.bundleContext.ungetService(reference);
            }

            protected void useCurator(CuratorFramework curator) {
                this.curator = curator;
                for (DelegateZooKeeperGroup<CamelNodeState> group : groups) {
                    group.useCurator(curator);
                }
            }

            @Override
            public CuratorFramework getCurator() {
                return curator;
            }

            @Override
            public Group<CamelNodeState> createGroup(String path) {
                return new DelegateZooKeeperGroup<CamelNodeState>(path, CamelNodeState.class) {
                    @Override
                    public void start() {
                        useCurator(curator);
                        groups.add(this);
                        super.start();
                    }

                    @Override
                    public void close() throws IOException {
                        groups.remove(this);
                        super.close();
                    }
                };
            }

            @Override
            public void close() {
                this.tracker.close();
            }
        }
    }
}
