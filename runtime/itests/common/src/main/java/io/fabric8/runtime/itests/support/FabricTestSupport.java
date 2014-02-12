/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package io.fabric8.runtime.itests.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.Constants;
import org.jboss.gravia.runtime.Filter;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceReference;
import org.junit.Assert;

/**
 * Test helper utility
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Jan-2014
 */
public final class FabricTestSupport {

    public final static long DEFAULT_TIMEOUT = 10000L;

    // Hide ctor
    private FabricTestSupport() {
    }

    public static <T> T getService(Class<T> clazz) {
        ModuleContext context = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceReference<T> sref = context.getServiceReference(clazz);
        Assert.assertNotNull("ServiceReference not null", sref);
        return context.getService(sref);
    }

    public static <T> T getService(Class<T> clazz, String filter) {
        ModuleContext context = RuntimeLocator.getRequiredRuntime().getModuleContext();
        Collection<ServiceReference<T>> srefs = context.getServiceReferences(clazz, filter);
        Assert.assertFalse("ServiceReferences found", srefs.isEmpty());
        return context.getService(srefs.iterator().next());
    }

    public static <T> T awaitService(Class<T> clazz) {
        return awaitService(clazz, null, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(Class<T> clazz, String filter) {
        return awaitService(clazz, filter, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(final Class<T> clazz, final String filterspec, long timeout, TimeUnit unit) {
        final ModuleContext context = RuntimeLocator.getRequiredRuntime().getModuleContext();
        final AtomicReference<ServiceReference<T>> serviceRef = new AtomicReference<ServiceReference<T>>();
        final Filter filter = filterspec != null ? context.createFilter(filterspec) : null;
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void serviceChanged(ServiceEvent event) {
                ServiceReference<?> sref = event.getServiceReference();
                List<String> classes = Arrays.asList((String[]) sref.getProperty(Constants.OBJECTCLASS));
                if (event.getType() == ServiceEvent.REGISTERED && classes.contains(clazz.getName())) {
                    if (filter == null || filter.match(sref)) {
                        serviceRef.set((ServiceReference<T>) sref);
                        latch.countDown();
                    }
                }
            }
        };
        context.addServiceListener(listener);
        try {
            ServiceReference<T> sref;
            if (filterspec != null) {
                Collection<ServiceReference<T>> srefs = context.getServiceReferences(clazz, filterspec);
                sref = srefs.isEmpty() ? null : srefs.iterator().next();
            } else {
                sref = context.getServiceReference(clazz);
            }
            if (sref == null && latch.await(timeout, unit)) {
                sref = serviceRef.get();
            }
            Assert.assertNotNull("ServiceReference not available", sref);
            return context.getService(sref);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        } finally {
            context.removeServiceListener(listener);
        }
    }
}
