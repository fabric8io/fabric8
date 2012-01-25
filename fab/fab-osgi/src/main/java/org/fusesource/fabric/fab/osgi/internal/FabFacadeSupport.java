/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.fab.osgi.internal;

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

