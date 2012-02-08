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
package org.fusesource.fabric.hadoop.hdfs;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class DataNodeFactory implements ManagedServiceFactory {

    private BundleContext bundleContext;
    private Map<String, DataNode> dataNodes = new HashMap<String, DataNode>();
    private Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getName() {
        return "HDFS DataNode factory";
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
            DataNode dataNode = DataNode.createDataNode(null, conf);
            dataNodes.put(pid, dataNode);
            services.put(pid, bundleContext.registerService(DataNode.class.getName(), dataNode, properties));
        } catch (Exception e) {
            throw (ConfigurationException) new ConfigurationException(null, "Unable to parse HDFS configuration: " + e.getMessage()).initCause(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }

    public synchronized void deleted(String pid) {
        DataNode dataNode = dataNodes.remove(pid);
        ServiceRegistration reg = services.remove(pid);
        if (reg != null) {
            reg.unregister();
        }
        if (dataNode != null) {
            dataNode.shutdown();
        }
    }

    public void destroy() {
        while (!dataNodes.isEmpty()) {
            String pid = dataNodes.keySet().iterator().next();
            deleted(pid);
        }
    }
}
