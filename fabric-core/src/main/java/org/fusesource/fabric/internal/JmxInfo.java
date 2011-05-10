/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import javax.management.openmbean.CompositeData;

/**
 * Base class for JMX info beans.
 *
 * @author ldywicki
 */
public class JmxInfo {

    /**
     * Composite data.
     */
    protected final CompositeData data;

    /**
     * Name of ID field.
     */
    private final String identifier;

    public JmxInfo(CompositeData data, String identifier) {
        this.data = data;
        this.identifier = identifier;
    }

    public Long getId() {
        return (Long) data.get(identifier);
    }

}
