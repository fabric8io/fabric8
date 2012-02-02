/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.impl;

import org.fusesource.patch.BundleUpdate;

public class BundleUpdateImpl implements BundleUpdate {

    private final String symbolicName;
    private final String newVersion;
    private final String previousVersion;
    private final String previousLocation;

    public BundleUpdateImpl(String symbolicName, String newVersion, String previousVersion, String previousLocation) {
        this.symbolicName = symbolicName;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.previousLocation = previousLocation;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public String getPreviousLocation() {
        return previousLocation;
    }

}
