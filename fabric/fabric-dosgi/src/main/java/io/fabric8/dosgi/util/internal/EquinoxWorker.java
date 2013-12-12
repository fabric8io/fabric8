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

import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.BundleLoaderProxy;
import org.osgi.framework.Bundle;

public class EquinoxWorker extends DefaultWorker implements FrameworkUtilWorker {

    public ClassLoader getClassLoader(Bundle b) {
        BundleHost host = (BundleHost) b;
        BundleLoaderProxy lp = host.getLoaderProxy();
        BundleLoader bl = (lp == null) ? null : lp.getBasicBundleLoader();
        BundleClassLoader cl = (bl == null) ? null : bl.createClassLoader();

        return ((cl instanceof ClassLoader) ? (ClassLoader) cl : null);
    }
}
