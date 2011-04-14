/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.data;

public interface BundleInfo {

    enum State {
        INSTALLED,
        RESOLVED,
        STARTING,
        ACTIVE,
        STOPPING,
        UNINSTALLED,
        UNKNOWN
    }

    State getState();
    String getSymbolicName();
    Long getId();
    String getVersion();

    String[] getImportPackages();
    String[] getExportPackages();

}
