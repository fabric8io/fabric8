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
package org.fusesource.fabric.hadoop.mapred;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TaskTracker;
import org.apache.hadoop.util.Daemon;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class TaskTrackerFactory implements ManagedServiceFactory {

    private BundleContext bundleContext;
    private Map<String, TaskTracker> taskTrackers = new HashMap<String, TaskTracker>();
    private Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getName() {
        return "MapRed JobTracker factory";
    }

    public synchronized void updated(String pid, Dictionary properties) throws ConfigurationException {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );

            deleted(pid);

            JobConf conf = new JobConf();
            for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                Object val = properties.get(key);
                conf.set( key.toString(), val.toString() );
            }
            TaskTracker taskTracker = new TaskTracker(conf);
            new Daemon(taskTracker).start();
            taskTrackers.put(pid, taskTracker);
            services.put(pid, bundleContext.registerService(TaskTracker.class.getName(), taskTracker, properties));
        } catch (Exception e) {
            throw (ConfigurationException) new ConfigurationException(null, "Unable to parse HDFS configuration: " + e.getMessage()).initCause(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }

    public synchronized void deleted(String pid) {
        TaskTracker taskTracker = taskTrackers.remove(pid);
        ServiceRegistration reg = services.remove(pid);
        if (reg != null) {
            reg.unregister();
        }
        if (taskTracker != null) {
            try {
                taskTracker.shutdown();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void destroy() {
        while (!taskTrackers.isEmpty()) {
            String pid = taskTrackers.keySet().iterator().next();
            deleted(pid);
        }
    }
}
