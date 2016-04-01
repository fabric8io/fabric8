/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 */
public class JMXUtils {
    private static final transient Logger LOG = LoggerFactory.getLogger(JMXUtils.class);


    private JMXUtils() {
        //Utils class
    }

    public static void registerMBean(Object bean, ObjectName objectName) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (mBeanServer != null) {
            try {
                registerMBean(bean, mBeanServer, objectName);
            } catch (Exception e) {
                LOG.error("Failed to register in JMX bean " + bean + " at " + objectName + ". " + e, e);
            }
        }
    }

    public static void registerMBean(Object bean, MBeanServer mBeanServer, ObjectName objectName) throws Exception {
        if (!mBeanServer.isRegistered(objectName)) {
            mBeanServer.registerMBean(bean, objectName);
        } else {
            unregisterMBean(mBeanServer, objectName);
            mBeanServer.registerMBean(bean, objectName);
        }
    }

    public static void unregisterMBean(ObjectName objectName) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (mBeanServer != null) {
            try {
                unregisterMBean(mBeanServer, objectName);
            } catch (Exception e) {
                LOG.error("Failed to unregister " + objectName + ". " + e, e);
            }
        }
    }

    public static void unregisterMBean(MBeanServer mBeanServer, ObjectName objectName) throws Exception {
        if (mBeanServer.isRegistered(objectName)) {
            mBeanServer.unregisterMBean(objectName);
        }
    }

}
