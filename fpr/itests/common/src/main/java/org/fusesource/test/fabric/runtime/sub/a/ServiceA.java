/*
 * #%L
 * Gravia :: Integration Tests :: Common
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
package org.fusesource.test.fabric.runtime.sub.a;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.fusesource.test.fabric.runtime.sub.a1.ServiceA1;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ServiceA.class }, immediate = true)
public class ServiceA {

    static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    final AtomicReference<ServiceA1> ref = new AtomicReference<ServiceA1>();

    @Activate
    void activate(ComponentContext context) {
    }

    @Deactivate
    void deactivate() {
    }

    @Reference
    void bindServiceA1(ServiceA1 service) {
        ref.set(service);
    }

    void unbindServiceA1(ServiceA1 service) {
        ref.compareAndSet(service, null);
    }

    public ServiceA1 getServiceA1() {
        return ref.get();
    }

    public String doStuff(String msg) {
        ServiceA1 srv = ref.get();
        return name + ":" + srv.doStuff(msg);
    }

    @Override
    public String toString() {
        return name;
    }
}