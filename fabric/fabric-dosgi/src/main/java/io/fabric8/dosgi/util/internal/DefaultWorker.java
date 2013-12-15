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


package io.fabric8.dosgi.util.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.IdentityHashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;

public class DefaultWorker implements FrameworkUtilWorker, BundleListener, FrameworkListener {
    private Map<Bundle, ClassLoader> classLoaders = new IdentityHashMap<Bundle, ClassLoader>();
    private static final Bundle myFrameworkBundle = FrameworkUtil.getBundle(DefaultWorker.class).getBundleContext().getBundle(0);

    public ClassLoader getClassLoader(final Bundle b) {
        ClassLoader cl = get(b);

        if (cl != null) return cl;

        // so first off try to get the real classloader. We can do this by loading a known class
        // such as the bundle activator. There is no guarantee this will work, so we have a back door too.
        String activator = (String) b.getHeaders().get(Constants.BUNDLE_ACTIVATOR);
        if (activator != null) {
            try {
                Class<?> clazz = b.loadClass(activator);
                // so we have the class, but it could have been imported, so we make sure the two bundles
                // are the same. A reference check should work here because there will be one.
                Bundle activatorBundle = FrameworkUtil.getBundle(clazz);
                if (activatorBundle == b) {
                    cl = clazz.getClassLoader();
                }
            } catch (ClassNotFoundException e) {
            }
        }

        if (cl == null) {
            // ok so we haven't found a class loader yet, so we need to create a wapper class loader
            cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return new BundleToClassLoaderAdapter(b);
                }
            });
        }

        if (cl != null) {
            setupListener(b);
            cl = put(b, cl);
        }

        return cl;
    }

    private void setupListener(Bundle b) {
        // So we need to cope with multiple equinox frameworks, so we can't just listen to our
        // BundleContext. Instead we add a listener to Bundle 0 of the framework bundle associated
        // with the bundle passed in.
        BundleContext ctx = b.getBundleContext().getBundle(0).getBundleContext();
        ctx.addBundleListener(this);
        ctx.addFrameworkListener(this);
    }

    private synchronized ClassLoader put(Bundle b, ClassLoader cl) {
        // If the bundle is uninstalled or installed then there is no classloader so we should
        // just return null. This is a last second sanity check to avoid memory leaks that could
        // occur if a bundle is uninstalled or unresolved while someone is calling getClassLoader
        if (b.getState() == Bundle.UNINSTALLED || b.getState() == Bundle.INSTALLED) return null;

        ClassLoader previous = classLoaders.put(b, cl);
        // OK, so we could cause a replace to occur here, so we want to check to
        // see if previous is not null. If it is not null we need to do a replace
        // and return the previous classloader. This ensures we have one classloader
        // in use for a bundle.
        if (previous != null) {
            cl = previous;
            classLoaders.put(b, cl);
        }

        return cl;
    }

    private synchronized ClassLoader get(Bundle b) {
        return classLoaders.get(b);
    }

    private synchronized void remove(Bundle bundle) {
        classLoaders.remove(bundle);
    }

    public boolean isValid() {
        return true;
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED || event.getType() == BundleEvent.UNRESOLVED) {
            Bundle b = event.getBundle();

            remove(b);

            if (b.getBundleId() == 0) {
                clearBundles(b);
            }
        }
    }

    private void clearBundles(Bundle b) {
        // we have been told about the system bundle, so we need to clear up any state for this framework.
        BundleContext ctx = b.getBundleContext();
        ctx.removeBundleListener(this);
        Bundle[] bundles = ctx.getBundles();
        for (Bundle bundle : bundles) {
            remove(bundle);
        }
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STOPPED) {
            Bundle b = event.getBundle();
            if (b == myFrameworkBundle) {
                classLoaders.clear();
            } else if (b != null) {
                clearBundles(b);
            }

            b.getBundleContext().removeFrameworkListener(this);
        }
    }
}