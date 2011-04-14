/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.data;

/**
 * Information about OSGi service.
 *
 * @author ldywicki
 */
public interface ServiceInfo {

    /**
     * Gets service id.
     *
     * @return Service id.
     */
    Long getId();

    /**
     * Gets exporting bundle id.
     *
     * @return Bundle id.
     */
    Long getBundleId();

    /**
     * Gets using bundles id.
     *
     * @return Bundles id.
     */
    Long[] getUsingBundlesId();

    /**
     * Gets names of classes/interfaces which service provides.
     *
     * @return Classes.
     */
    String[] getObjectClasses();

}
