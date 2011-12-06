/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.FabricException;
import org.fusesource.insight.log.*;

import org.fusesource.insight.log.service.LogQueryCallback;
import org.fusesource.insight.log.service.LogQueryMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Utility class which contains code related to JMX connectivity.
 *
 * @author ldywicki
 */
public abstract class JmxTemplateSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(JmxTemplateSupport.class);

    public interface JmxConnectorCallback<T> {

        T doWithJmxConnector(JMXConnector connector) throws Exception;

    }

    public abstract <T> T execute(JmxConnectorCallback<T> callback);

    public <T> T execute(final LogQueryCallback<T> callback) {
        return execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[]{"type", "LogQuery"};
                return callback.doWithLogQuery(getMBean(connector, LogQueryMBean.class, "org.fusesource.insight", bean));
            }
        });
    }


    // MBean specific callbacks

    public static ObjectName safeObjectName(String domain, String ... args) {
        if ((args.length % 2) != 0) {
             LOGGER.warn("Not all values were defined for arguments %", Arrays.toString(args));
        }
        Hashtable<String, String> table = new Hashtable<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            table.put(args[i], args[i + 1]);
        }
        try {
            return new ObjectName(domain, table);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Object name is invalid", e);
        }
    }

    public <T> T getMBean(JMXConnector connector, Class<T> type, String domain, String ... params) {
        try {
            return JMX.newMBeanProxy(connector.getMBeanServerConnection(), safeObjectName(domain, params), type);
        } catch (IOException e) {
            throw new FabricException(e);
        }
    }

}
