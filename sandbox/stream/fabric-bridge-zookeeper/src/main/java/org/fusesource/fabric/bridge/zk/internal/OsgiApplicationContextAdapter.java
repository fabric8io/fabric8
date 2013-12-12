/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.bridge.zk.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author Dhiraj Bokde
 */
public class OsgiApplicationContextAdapter implements InvocationHandler {

    private final String pid;
    private final AbstractZkManagedServiceFactory serviceFactory;

    private final Method getBeansMethod;
    private static final Logger LOG = LoggerFactory.getLogger(OsgiApplicationContextAdapter.class);

    public OsgiApplicationContextAdapter(String pid, AbstractZkManagedServiceFactory serviceFactory) {
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
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("OSGi service lookup for class [%s] with filter [%s]", beanClass.getName(), filter));
        }
        ServiceReference[] references = bundleContext.getServiceReferences(beanClass.getName(), filter);
        if (references != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Found %d OSGi services", references.length));
            }
            Object service = bundleContext.getService(references[0]);
            serviceFactory.addServiceReference(pid, references[0]);
            return service;
        }
        LOG.error("OSGi service not found for class [%s] with filter [%s]", beanClass.getName(), filter);
        throw new NoSuchBeanDefinitionException(beanName, " no such OSGi service");
    }

}
