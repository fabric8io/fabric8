/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;

import java.util.ArrayList;
import java.util.List;

public class FabricFailOverFeature extends FabricLoadBalancerFeature {
    private static final transient Log LOG = LogFactory.getLog(FabricFailOverFeature.class);
    protected String exceptions;
    protected List<Class> exceptionList = new ArrayList<Class>();

    protected LoadBalanceStrategy getDefaultLoadBalanceStrategy() {
        // This strategy always return the first physical address from the locator
        return new FirstOneLoadBalanceStrategy();
    }

    protected LoadBalanceTargetSelector getDefaultLoadBalanceTargetSelector() {
        return new FailOverTargetSelector(exceptionList);
    }

    public void setExceptions(String exceptions) {
        this.exceptions = exceptions;
    }

    public void afterPropertiesSet() throws Exception {
        if (exceptions != null) {
            String[] exceptionArray =  exceptions.split(";");
            for (String exception: exceptionArray) {
                try {
                    Class<?> clazz = ClassLoaderUtils.loadClass(exception, this.getClass());
                    exceptionList.add(clazz);
                } catch (ClassNotFoundException ex) {
                    LOG.warn("Can't load the exception " + exception + " for the FabricFailOverFeature.");
                }
            }
        }
        super.afterPropertiesSet();
    }
}
