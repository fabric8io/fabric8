/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        ServiceReference configurationAdminReference =
                context.getServiceReference(ConfigurationAdmin.class.getName());

        if (configurationAdminReference != null) {
            LOG.info("Changing security realm to zookeeper");
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            Configuration config = confAdmin.getConfiguration("org.apache.karaf.shell");
            Dictionary props = config.getProperties();
            props.put("sshRealm", "zookeeper");
            config.update(props);

            config = confAdmin.getConfiguration("org.apache.karaf.management");
            props = config.getProperties();
            props.put("jmxRealm", "zookeeper");
            config.update(props);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ServiceReference configurationAdminReference =
                context.getServiceReference(ConfigurationAdmin.class.getName());

        if (configurationAdminReference != null) {
            LOG.info("Changing security realm to karaf");
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            Configuration config = confAdmin.getConfiguration("org.apache.karaf.shell");
            Dictionary props = config.getProperties();
            props.put("sshRealm", "karaf");
            config.update(props);

            config = confAdmin.getConfiguration("org.apache.karaf.management");
            props = config.getProperties();
            props.put("jmxRealm", "karaf");
            config.update(props);
        }
    }
}
