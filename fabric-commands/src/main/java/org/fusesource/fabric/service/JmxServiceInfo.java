/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.util.Collection;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.fusesource.fabric.api.data.ServiceInfo;
import org.osgi.jmx.JmxConstants;

import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.framework.ServiceStateMBean.USING_BUNDLES;

/**
 * Implementation of ServiceInfo interface based on CompositeData.
 */
public class JmxServiceInfo extends JmxInfo implements ServiceInfo {

    protected final TabularData properties;

    public JmxServiceInfo(CompositeData data, TabularData properties) {
        super(data, IDENTIFIER);
        this.properties = properties;
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

    @Override
    public Property[] getProperties() {
        Property[] props = new Property[this.properties.size()];
        int i = 0;
        for (CompositeData data : (Collection<CompositeData>) properties.values()) {
            String key = data.get(JmxConstants.KEY).toString();
            Object value = data.get(JmxConstants.VALUE);
            props[i++] = new PropertyImpl(key, value);
        }
        return props;
    }

    static class PropertyImpl implements Property {
        final String key;
        final Object value;

        PropertyImpl(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
