package io.fabric8.agent.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class OsgiUtils {

    public static void ensureAllClassesLoaded(Bundle bundle) throws ClassNotFoundException {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            for (String path : wiring.listResources("/", "*.class", BundleWiring.LISTRESOURCES_RECURSE)) {
                String className = path.substring(0, path.length() - ".class".length());
                className = className.replace('/', '.');
                bundle.loadClass(className);
            }
        }
    }

}
