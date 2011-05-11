/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import java.util.Comparator;

import org.fusesource.fabric.api.data.ServiceInfo;

public class ServiceInfoComparator implements Comparator<ServiceInfo> {

    public int compare(ServiceInfo serviceInfo1, ServiceInfo serviceInfo2) {
        return serviceInfo1.getId().compareTo(serviceInfo2.getId());
    }
}
