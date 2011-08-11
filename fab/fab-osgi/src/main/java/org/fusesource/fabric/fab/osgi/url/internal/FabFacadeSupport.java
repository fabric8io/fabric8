/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.PomDetails;

import java.io.File;
import java.io.IOException;

/**
 * Base class for implementing FabFacade
 */
public abstract class FabFacadeSupport implements FabFacade {
    private PomDetails pomDetails;
    private MavenResolver resolver = new MavenResolver();
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
        return resolver;
    }

    public void setResolver(MavenResolver resolver) {
        this.resolver = resolver;
    }
}

