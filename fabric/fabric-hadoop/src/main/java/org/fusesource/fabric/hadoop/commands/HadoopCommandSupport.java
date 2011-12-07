/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.hadoop.commands;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.hadoop.HadoopFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public abstract class HadoopCommandSupport extends OsgiCommandSupport {

    protected abstract void doExecute(org.apache.hadoop.conf.Configuration configuration) throws Exception;

    @Override
    protected Object doExecute() throws Exception {
        ServiceReference ref = getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin admin = ref != null ? getService(ConfigurationAdmin.class, ref) : null;
        org.osgi.service.cm.Configuration config = admin != null ? admin.getConfiguration(HadoopFactory.CONFIG_PID) : null;
        Dictionary dictionary = config != null ? config.getProperties() : null;
        if (dictionary == null) {
            throw new IllegalStateException("No configuration found for pid " + HadoopFactory.CONFIG_PID);
        }

        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        for (Enumeration e = dictionary.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object val = dictionary.get(key);
            conf.set( key.toString(), val.toString() );
        }

        doExecute(conf);
        return null;
    }
}
