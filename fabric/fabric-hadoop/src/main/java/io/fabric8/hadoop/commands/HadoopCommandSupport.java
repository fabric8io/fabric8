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
package io.fabric8.hadoop.commands;

import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Enumeration;
import javax.security.auth.Subject;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.hadoop.HadoopFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public abstract class HadoopCommandSupport extends OsgiCommandSupport {

    protected abstract void doExecute(org.apache.hadoop.conf.Configuration configuration) throws Exception;

    @Override
    protected Object doExecute() throws Exception {
        Subject.doAs(null, new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
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
        });
        return null;
    }
}
