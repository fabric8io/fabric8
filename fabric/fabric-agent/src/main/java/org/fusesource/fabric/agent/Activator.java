/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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

    private Callable<IZKClient> getZkClient(BundleContext context) {
        zkClient = new ServiceTracker(context, IZKClient.class.getName(), null);
        zkClient.open();
        return new Callable<IZKClient>() {
            @Override
            public IZKClient call() throws Exception {
                return (IZKClient) zkClient.getService();
            }
        };
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
