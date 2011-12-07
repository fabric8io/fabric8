/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.hadoop.commands;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.fusesource.fabric.hadoop.HadoopFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public class HadoopCommand extends AbstractCommand implements Function, CompletableFunction {

    Class tool;

    public Class getTool() {
        return tool;
    }

    public void setTool(Class tool) {
        this.tool = tool;
    }

    @Override
    public Object execute(CommandSession commandSession, List<Object> objects) throws Exception {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            doExecute(objects);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
        return null;
    }


    protected void doExecute(List<Object> objects) throws Exception {
        org.apache.hadoop.conf.Configuration conf = getConfiguration();
        Tool run;
        try {
            Constructor cns = tool.getDeclaredConstructor();
            cns.setAccessible(true);
            run = (Tool) cns.newInstance();
            run.setConf(conf);
        } catch (NoSuchMethodException e) {
            Constructor cns = tool.getDeclaredConstructor(org.apache.hadoop.conf.Configuration.class);
            cns.setAccessible(true);
            run = (Tool) cns.newInstance(conf);
        }
        String[] args = new String[objects.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = objects.get(i) != null ? objects.get(i).toString() : null;
        }
        run.run(args);
    }

    private Configuration getConfiguration() throws IOException {
        BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference ref = bc.getServiceReference(ConfigurationAdmin.class.getName());
        try {
            ConfigurationAdmin admin = ref != null ? (ConfigurationAdmin) bc.getService(ref) : null;
            org.osgi.service.cm.Configuration config = admin != null ? admin.getConfiguration(HadoopFactory.CONFIG_PID) : null;
            Dictionary dictionary = config != null ? config.getProperties() : null;
            if (dictionary == null) {
                throw new IllegalStateException("No configuration found for pid " + HadoopFactory.CONFIG_PID);
            }
            Configuration conf = new Configuration();
            for (Enumeration e = dictionary.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                Object val = dictionary.get(key);
                conf.set( key.toString(), val.toString() );
            }
            return conf;
        }
        finally {
            if (ref != null) {
                bc.ungetService(ref);
            }
        }
    }

    @Override
    public List<Completer> getCompleters() {
        return null;
    }

    @Override
    public Class<? extends Action> getActionClass() {
        return tool;
    }

    @Override
    public Action createNewAction() {
        return null;
    }
}
