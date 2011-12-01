/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.hadoop;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.fusesource.fabric.hadoop.hdfs.DataNodeFactory;
import org.fusesource.fabric.hadoop.hdfs.NameNodeFactory;
import org.fusesource.fabric.hadoop.hdfs.SecondaryNameNodeFactory;
import org.fusesource.fabric.hadoop.mapred.JobTrackerFactory;
import org.fusesource.fabric.hadoop.mapred.TaskTrackerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

public class HadoopFactory implements ManagedService {

    public static final String CONFIG_PID = "org.fusesource.fabric.hadoop";
    private BundleContext bundleContext;

    private DataNodeFactory dataNodeFactory;
    private NameNodeFactory nameNodeFactory;
    private SecondaryNameNodeFactory secondaryNameNodeFactory;
    private JobTrackerFactory jobTrackerFactory;
    private TaskTrackerFactory taskTrackerFactory;

    private Set<String> nameNodes = new HashSet<String>();
    private Set<String> dataNodes = new HashSet<String>();
    private Set<String> secondaryNameNodes = new HashSet<String>();
    private Set<String> jobTrackers = new HashSet<String>();
    private Set<String> taskTrackers = new HashSet<String>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public DataNodeFactory getDataNodeFactory() {
        return dataNodeFactory;
    }

    public void setDataNodeFactory(DataNodeFactory dataNodeFactory) {
        this.dataNodeFactory = dataNodeFactory;
    }

    public NameNodeFactory getNameNodeFactory() {
        return nameNodeFactory;
    }

    public void setNameNodeFactory(NameNodeFactory nameNodeFactory) {
        this.nameNodeFactory = nameNodeFactory;
    }

    public SecondaryNameNodeFactory getSecondaryNameNodeFactory() {
        return secondaryNameNodeFactory;
    }

    public void setSecondaryNameNodeFactory(SecondaryNameNodeFactory secondaryNameNodeFactory) {
        this.secondaryNameNodeFactory = secondaryNameNodeFactory;
    }

    public JobTrackerFactory getJobTrackerFactory() {
        return jobTrackerFactory;
    }

    public void setJobTrackerFactory(JobTrackerFactory jobTrackerFactory) {
        this.jobTrackerFactory = jobTrackerFactory;
    }

    public TaskTrackerFactory getTaskTrackerFactory() {
        return taskTrackerFactory;
    }

    public void setTaskTrackerFactory(TaskTrackerFactory taskTrackerFactory) {
        this.taskTrackerFactory = taskTrackerFactory;
    }

    public void updated(Dictionary properties) throws ConfigurationException {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            if (properties == null) {
                deleted(getClass().getName());
            } else {
                updated(getClass().getName(), properties);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }

    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        updateFactory(pid, properties, "nameNode", nameNodes, nameNodeFactory);
        updateFactory(pid, properties, "dataNode", dataNodes, dataNodeFactory);
        updateFactory(pid, properties, "secondaryNameNode", secondaryNameNodes, secondaryNameNodeFactory);
        updateFactory(pid, properties, "jobTracker", jobTrackers, jobTrackerFactory);
        updateFactory(pid, properties, "taskTracker", taskTrackers, taskTrackerFactory);
    }

    private void updateFactory(String pid, Dictionary properties, String prop, Set<String> set, ManagedServiceFactory factory) throws ConfigurationException {
        if (getBool(properties.get(prop), false)) {
            set.add(pid);
            factory.updated(pid, properties);
        } else if (set.remove(pid)) {
            factory.deleted(pid);
        }
    }

    public void deleted(String pid) {
        deleteFactory(pid, nameNodes, nameNodeFactory);
    }

    private void deleteFactory(String pid, Set<String> set, ManagedServiceFactory factory) {
        if (set.remove(pid)) {
            factory.deleted(pid);
        }
    }

    public void destroy() throws ConfigurationException {
        updated(null);
    }

    private boolean getBool(Object val, boolean def) {
        if (val != null) {
            return Boolean.parseBoolean(val.toString());
        } else {
            return def;
        }
    }

}
