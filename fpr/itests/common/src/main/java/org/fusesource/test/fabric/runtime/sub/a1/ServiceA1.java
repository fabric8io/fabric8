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
package org.fusesource.test.fabric.runtime.sub.a1;

import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { ServiceA1.class })
public class ServiceA1 {

    static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    @Activate
    void activate(ComponentContext context) {
    }

    @Deactivate
    void deactivate() {
    }

    public String doStuff(String msg) {
        return name + ":" + msg;
    }

    @Override
    public String toString() {
        return name;
    }
}