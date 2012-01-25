/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.insight.graph.support;

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
    private static final String PID = "org.fusesource.insight.graph";

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
