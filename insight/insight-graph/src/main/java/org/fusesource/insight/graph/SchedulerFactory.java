/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.graph;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

/**
 * A helper factory class for creating Quartz {@link Scheduler} instances
 */
public class SchedulerFactory {
    private static final transient Logger LOG = LoggerFactory.getLogger(SchedulerFactory.class);
    private static final String PID = "org.fusesource.insight.insight-graph";

    private StdSchedulerFactory factory = new StdSchedulerFactory();
    private Properties properties = new Properties();
    private ConfigurationAdmin configAdmin;

    public Scheduler createScheduler() throws SchedulerException {
        if (configAdmin != null) {
            try {
                Configuration configuration = configAdmin.getConfiguration(PID);
                Dictionary dictionary = configuration.getProperties();
                if (dictionary == null) {
                    LOG.warn("No properties for configuration: " + PID);
                } else {
                    Enumeration e = dictionary.keys();
                    if (e == null) {
                        LOG.warn("No properties for configuration: " + PID);
                    } else {
                        properties = new Properties();
                        while (e.hasMoreElements()) {
                            Object key = e.nextElement();
                            if (key != null) {
                                properties.put(key.toString(), dictionary.get(key));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to get configuration for PID: " + PID);
            }
        }
        LOG.info("Creating Quartz Schedular using properties: " + properties);
        factory.initialize(properties);
        return factory.getScheduler();
    }

    public StdSchedulerFactory getFactory() {
        return factory;
    }

    public void setFactory(StdSchedulerFactory factory) {
        this.factory = factory;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}
