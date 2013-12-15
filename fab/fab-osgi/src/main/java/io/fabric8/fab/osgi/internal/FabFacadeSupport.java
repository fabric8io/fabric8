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

package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;

import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.MavenResolver;
import io.fabric8.fab.MavenResolverImpl;
import io.fabric8.fab.PomDetails;
import org.osgi.framework.BundleContext;

/**
 * Base class for implementing FabFacade
 */
public abstract class FabFacadeSupport implements FabFacade {
    
    private PomDetails pomDetails;
    private MavenResolver resolver;
    private boolean includeSharedResources = true;

    /**
     * If the PomDetails has not been resolved yet, try and resolve it
     */
    public PomDetails resolvePomDetails() throws IOException {
        PomDetails pomDetails = getPomDetails();
        if (pomDetails == null) {
            pomDetails = findPomDetails();
        }
        return pomDetails;
    }

    protected PomDetails findPomDetails() throws IOException {
        PomDetails pomDetails;File fileJar = getJarFile();
        pomDetails = getResolver().findPomFile(fileJar);
        return pomDetails;
    }

    // Properties
    //-------------------------------------------------------------------------

    public boolean isIncludeSharedResources() {
        return includeSharedResources;
    }

    public void setIncludeSharedResources(boolean includeSharedResources) {
        this.includeSharedResources = includeSharedResources;
    }

    public PomDetails getPomDetails() {
        return pomDetails;
    }

    public void setPomDetails(PomDetails pomDetails) {
        this.pomDetails = pomDetails;
    }

    public MavenResolver getResolver() {
        if (resolver == null) {
            resolver = new MavenResolverImpl();
        }
        return resolver;
    }

    public void setResolver(MavenResolver resolver) {
        this.resolver = resolver;
    }
    
    protected static boolean isInstalled(BundleContext context, DependencyTree tree) {
        if (context != null && tree.getVersion() != null && tree.getBundleSymbolicName() != null) {
            return Bundles.findBundle(context, tree.getBundleSymbolicName(), tree.getVersion()) != null;
        }
        return false;
    }
}

