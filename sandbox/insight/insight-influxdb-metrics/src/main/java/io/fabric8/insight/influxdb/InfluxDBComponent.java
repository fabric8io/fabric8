/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.insight.influxdb;

import io.fabric8.api.FabricException;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@Component
public class InfluxDBComponent extends AbstractComponent implements GroupListener<InfluxDBNode> {

    private static final String INFLUXDB_CLUSTER_PATH = "/fabric/registry/clusters/influxdb";

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private Group<InfluxDBNode> group;
    private InfluxDB influxDB;
    private ServiceRegistration<InfluxDB> registration;
    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        activateInternal();
        activateComponent();
    }

    @Deactivate
    synchronized void deactivate() throws IOException {
        deactivateComponent();
        if (group != null) {
            group.close();
        }
        unregister();
    }


    private void activateInternal() {
        group = new ZooKeeperGroup<InfluxDBNode>(curator.get(), INFLUXDB_CLUSTER_PATH, InfluxDBNode.class);
        group.add(this);
        group.start();
    }

    @Override
    public synchronized void groupEvent(Group<InfluxDBNode> group, GroupEvent event) {
        switch (event) {
            case CONNECTED:
                InfluxDBNode master = group.master();
                if (master != null) {
                    try {
                        String url = getSubstitutedData(curator.get(), master.getUrl());
                        influxDB = InfluxDBFactory.connect(url, "root", "root");
                        unregister();
                        registration = bundleContext.registerService(InfluxDB.class, influxDB, null);
                    } catch (URISyntaxException e) {
                        throw FabricException.launderThrowable(e);
                    }
                }
            case CHANGED:
                break;
            case DISCONNECTED:
                unregister();

        }
    }

    private synchronized void unregister() {
        if (registration != null) {
            registration.unregister();
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
