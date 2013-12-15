package io.fabric8.utils;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;

public class OsgiUtils {

    public static final Long SERVICE_TIMEOUT = 20000L;

    public void waitForSerice(Class type, long timeout) {
        waitForSerice(type, null, timeout);
    }

    public static void waitForSerice(Class type) {
        waitForSerice(type, null, SERVICE_TIMEOUT);
    }

    public static void waitForSerice(Class type, String filter, long timeout) {
        BundleContext bundleContext = getBundleContext();
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + org.osgi.framework.Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + org.osgi.framework.Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + org.osgi.framework.Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            if (tracker.waitForService(timeout) == null) {
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            tracker.close();
        }
    }

    /**
     * Returns the bundle context.
     *
     * @return
     */
    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(OsgiUtils.class).getBundleContext();
    }

    /**
     * Explode the dictionary into a ,-delimited list of key=value pairs
     */
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuffer result = new StringBuffer();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /**
     * Provides an iterable collection of references, even if the original array is null
     */
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference>emptyList();
    }
}
