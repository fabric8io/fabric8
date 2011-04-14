/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.util.Comparator;

import org.fusesource.fabric.api.data.BundleInfo;

public class BundleInfoComparator implements Comparator<BundleInfo> {

    public int compare(BundleInfo object1, BundleInfo object2) {
        return object1.getId().compareTo(object2.getId());
    }
}
