/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.data.ServiceInfo;

import static org.osgi.jmx.framework.ServiceStateMBean.*;
import javax.management.openmbean.CompositeData;

/**
 * Implementation of ServiceInfo interface based on CompositeData.
 */
public class JmxServiceInfo extends JmxInfo implements ServiceInfo {

    public JmxServiceInfo(CompositeData data) {
        super(data, IDENTIFIER);
    }

    public Long getBundleId() {
        return (Long) data.get(BUNDLE_IDENTIFIER);
    }

    public Long[] getUsingBundlesId() {
        return (Long[]) data.get(USING_BUNDLES);
    }

    public String[] getObjectClasses() {
        return (String[]) data.get(OBJECT_CLASS);
    }

}
