package io.fabric8.api.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Stan Lewis
 */
public class JMXUtils {

    public static void registerMBean(Object bean, MBeanServer mBeanServer, ObjectName objectName) throws Exception {
        if (!mBeanServer.isRegistered(objectName)) {
            mBeanServer.registerMBean(bean, objectName);
        } else {
            unregisterMBean(mBeanServer, objectName);
            mBeanServer.registerMBean(bean, objectName);
        }
    }

    public static void unregisterMBean(MBeanServer mBeanServer, ObjectName objectName) throws Exception {
        if (mBeanServer.isRegistered(objectName)) {
            mBeanServer.unregisterMBean(objectName);
        }
    }

}
