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
package org.fusesource.fabric.agent;

import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    public static final String AGENT_PID = "org.fusesource.fabric.agent";

    private DeploymentAgent agent;
    private ServiceTracker packageAdmin;
    private ServiceTracker startLevel;
    private ServiceTracker zkClient;
    private ServiceTracker fabricService;
    private ServiceRegistration registration;

    public void start(BundleContext context) throws Exception {
        agent = new DeploymentAgent();
        agent.setBundleContext(context);
        agent.setObrResolver(new ObrResolver(new RepositoryAdminImpl(context, new Logger(context))));
        agent.setPackageAdmin(getPackageAdmin(context));
        agent.setStartLevel(getStartLevel(context));
        agent.setZkClient(getZkClient(context));
        agent.start();
        Properties props = new Properties();
        props.setProperty(Constants.SERVICE_PID, AGENT_PID);
        registration = context.registerService(ManagedService.class.getName(), agent, props);
    }

    private ServiceTracker getZkClient(BundleContext context) {
        zkClient = new ServiceTracker(context, IZKClient.class.getName(), null);
        zkClient.open();
        return zkClient;
    }

    private StartLevel getStartLevel(BundleContext context) {
        startLevel = new ServiceTracker(context, StartLevel.class.getName(), null);
        startLevel.open();
        return (StartLevel) startLevel.getService();
    }

    private PackageAdmin getPackageAdmin(BundleContext context) {
        packageAdmin = new ServiceTracker(context, PackageAdmin.class.getName(), null);
        packageAdmin.open();
        return (PackageAdmin) packageAdmin.getService();
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        context.removeFrameworkListener(agent);
        agent.stop();
        packageAdmin.close();
        startLevel.close();
        zkClient.close();
    }

}
