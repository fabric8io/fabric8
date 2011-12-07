/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.hadoop.hdfs;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.SecondaryNameNode;
import org.apache.hadoop.util.Daemon;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class SecondaryNameNodeFactory implements ManagedServiceFactory {

    private BundleContext bundleContext;
    private Map<String, SecondaryNameNode> secondaryNameNodes = new HashMap<String, SecondaryNameNode>();
    private Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getName() {
        return "HDFS NodeName factory";
    }

    public synchronized void updated(String pid, Dictionary properties) throws ConfigurationException {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );

            deleted(pid);

            Configuration conf = new Configuration();
            for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                Object val = properties.get(key);
                conf.set( key.toString(), val.toString() );
            }
            SecondaryNameNode secondaryNameNode = new SecondaryNameNode(conf);
            Daemon checkpointThread = new Daemon(secondaryNameNode);
            checkpointThread.start();
            secondaryNameNodes.put(pid, secondaryNameNode);
            services.put(pid, bundleContext.registerService(NameNode.class.getName(), secondaryNameNode, properties));
        } catch (Exception e) {
            throw (ConfigurationException) new ConfigurationException(null, "Unable to parse HDFS configuration: " + e.getMessage()).initCause(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }

    public synchronized void deleted(String pid) {
        SecondaryNameNode secondaryNameNode = secondaryNameNodes.remove(pid);
        ServiceRegistration reg = services.remove(pid);
        if (reg != null) {
            reg.unregister();
        }
        if (secondaryNameNode != null) {
            secondaryNameNode.shutdown();
        }
    }

    public void destroy() {
        while (!secondaryNameNodes.isEmpty()) {
            String pid = secondaryNameNodes.keySet().iterator().next();
            deleted(pid);
        }
    }
}
