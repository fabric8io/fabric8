/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.bridge.zk.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Dhiraj Bokde
 */
public class SpringOsgiBeanBridge implements InvocationHandler {

    private final String pid;
    private final ZkManagedBridgeServiceFactory serviceFactory;

    private final Method getBeansMethod;
    private static final Logger LOG = LoggerFactory.getLogger(SpringOsgiBeanBridge.class);

    public SpringOsgiBeanBridge(String pid, ZkManagedBridgeServiceFactory serviceFactory) {
        super();
        this.pid = pid;
        this.serviceFactory = serviceFactory;
        try {
            this.getBeansMethod = ApplicationContext.class.getMethod(
                "getBean", String.class, Class.class);
        } catch (NoSuchMethodException e) {
            String msg = "Error getting getBean method: " + e.getMessage();
            LOG.error(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!getBeansMethod.equals(method)) {
            String msg = "Unsupported method " + method.getName();
            LOG.error(msg);
            throw new UnsupportedOperationException(msg);
        }
        final BundleContext bundleContext = serviceFactory.getBundleContext();
        String beanName = String.valueOf(args[0]);
        Class beanClass = (Class) args[1];
        String filter = "(" + Constants.SERVICE_PID + "=" + beanName + ")";
        ServiceReference[] references = bundleContext.getServiceReferences(beanClass.getName(), filter);
        if (references != null) {
            Object service = bundleContext.getService(references[0]);
            serviceFactory.addServiceReference(pid, references[0]);
            return service;
        }
        throw new NoSuchBeanDefinitionException(beanName, " no such OSGi service");
    }

}
