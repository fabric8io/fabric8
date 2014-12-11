/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.jboss.fuse.rhaccess.servlet;

import org.jboss.fuse.rhaccess.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * AbstractJMXServlet is a superclass for all the support servlets. It instantiate the JMX required objects.
 */
public abstract class AbstractJMXServlet extends HttpServlet {
    public static final String SUPPORT_TYPE_SUPPORT_SERVICE_MBEAN = "support:type=SupportServiceMBean";
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJMXServlet.class);
    private static String fuseVersion;
    protected Config config;
    private MBeanServer mBeanServer = null;
    private ObjectName objectName;
    private String version;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (objectName == null) {
            try {
                objectName = getObjectName();
            } catch (Exception e) {
                String message = "Failed to create object name: ";
                LOG.warn(message, e);
                throw new RuntimeException(message, e);
            }
        }
        config = new Config();
    }

    public void destroy() {
        if (mBeanServer != null && objectName != null) {
            try {
                if (mBeanServer.isRegistered(objectName)) {
                    mBeanServer.unregisterMBean(objectName);
                }
            } catch (Exception e) {
                String message = "Failed to unregister mbean: ";
                LOG.warn(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    protected ObjectName getObjectName() throws Exception {
        return new ObjectName(SUPPORT_TYPE_SUPPORT_SERVICE_MBEAN);
    }


    protected String collect() throws MBeanException, InstanceNotFoundException, ReflectionException {
        Object o = mBeanServer.invoke(objectName, "collect", null, null);
        return String.valueOf(o);
    }

    protected String obtainVersion() throws IOException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {
        if (version == null) {
            // Object o = mBeanServer.invoke(objectName, "getVersion", null, null);
            Object o = mBeanServer.getAttribute(objectName, "Version");
            version = String.valueOf(o);
        }
        return version;
    }


}
