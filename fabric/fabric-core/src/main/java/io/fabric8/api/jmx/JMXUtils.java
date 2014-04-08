package io.fabric8.api.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 */
public class JMXUtils {

    private JMXUtils() {
        //Utils class
    }
    
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
