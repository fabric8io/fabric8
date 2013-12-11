
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fusesource.fabric.zookeeper.curator;

import org.apache.curator.framework.CuratorFramework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;


/**
 * Locate the {@link CuratorFramework}
 *
 * @author thomas.diesler@jboss.com
 * @since 10-Dec-2013
 */
public final class CuratorFrameworkLocator  {

    // Hide ctor
    private CuratorFrameworkLocator() {
    }

    /**
     * Get the current CuratorFramework or null.
     */
    public static CuratorFramework getCuratorFramework() {
        CuratorFramework curator = null;
        ClassLoader classLoader = CuratorFrameworkLocator.class.getClassLoader();
        if (classLoader instanceof BundleReference) {
            Bundle bundle = ((BundleReference)classLoader).getBundle();
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext != null) {
                BundleContext syscontext = bundleContext.getBundle(0).getBundleContext();
                org.osgi.framework.ServiceReference<CuratorFramework> sref = syscontext.getServiceReference(CuratorFramework.class);
                if (sref != null) {
                    curator = syscontext.getService(sref);
                }
            }
        } else {
            throw new UnsupportedOperationException("Cannot obtain curator using: " + classLoader);
        }
        return curator;
    }
}