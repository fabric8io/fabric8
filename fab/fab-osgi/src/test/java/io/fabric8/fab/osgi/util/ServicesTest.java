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
package io.fabric8.fab.osgi.util;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

/**
 * Test cases for {@link Services}
 */
public class ServicesTest {

    @Test
    public void testParseManifestHeader() {
        assertEquals("Should have a single NamespaceHandler", new Service("org.apache.aries.blueprint.NamespaceHandler"),
                     Services.parseHeader("org.apache.aries.blueprint.NamespaceHandler").iterator().next());
        
        Set<Service> services = Services.parseHeader("org.apache.aries.blueprint.NamespaceHandler;osgi.service.blueprint.namespace=\"\"," +
                "org.apache.aries.blueprint.NamespaceHandler;osgi.service.blueprint.namespace=\"http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0\"," +
                "org.apache.aries.blueprint.ParserService");

        assertTrue("Should have a plain NamespaceHandler", services.contains(new Service("org.apache.aries.blueprint.NamespaceHandler")));
        assertTrue("Should have a plain ParserService", services.contains(new Service("org.apache.aries.blueprint.ParserService")));

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("osgi.service.blueprint.namespace", "http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0");
        assertTrue("Should have a NamespaceHandler with properties",
                   services.contains(new Service("org.apache.aries.blueprint.NamespaceHandler", properties)));
        
        assertEquals("A null header should return an empty set", 0, Services.parseHeader(null).size());
        assertEquals("A blank header should return an empty set", 0, Services.parseHeader("").size());
    }

    @Test
    public void testIsPlainServiceAvailable() throws InvalidSyntaxException {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getServiceReferences("org.apache.aries.blueprint.NamespaceHandler", null)).andReturn(someReferences());

        replay(context);
        assertTrue("Service is available", Services.isAvailable(context, "org.apache.aries.blueprint.NamespaceHandler"));
        verify(context);
    }

    @Test
    public void testIsServiceWithPropertiesAvailable() throws InvalidSyntaxException {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getServiceReferences("org.apache.aries.blueprint.NamespaceHandler", "(key=value)")).andReturn(someReferences());

        replay(context);
        assertTrue("Service is available",
                   Services.isAvailable(context, "org.apache.aries.blueprint.NamespaceHandler",
                                        Services.createProperties("key", "value")));
        verify(context);
    }

    @Test
    public void testIsPlainServiceNotAvailable() throws InvalidSyntaxException {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getServiceReferences("org.apache.aries.blueprint.NamespaceHandler", null)).andReturn(null);

        replay(context);
        assertFalse("Service is not available", Services.isAvailable(context, "org.apache.aries.blueprint.NamespaceHandler"));
        verify(context);
    }

    @Test
    public void testIsServiceWithPropertiesNotAvailable() throws InvalidSyntaxException {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getServiceReferences("org.apache.aries.blueprint.NamespaceHandler", "(key=value)")).andReturn(null);

        replay(context);
        assertFalse("Service is not available",
                    Services.isAvailable(context, "org.apache.aries.blueprint.NamespaceHandler",
                                         Services.createProperties("key", "value")));
        verify(context);
    }

    @Test
    public void testAreMultipleServicesAvailable() throws InvalidSyntaxException {
        BundleContext context = createNiceMock(BundleContext.class);
        expect(context.getServiceReferences("org.apache.aries.blueprint.NamespaceHandler", "(key=value)")).andReturn(someReferences());
        expect(context.getServiceReferences("some.other.class.Name", null)).andReturn(someReferences());

        replay(context);
        Set<Service> services = new HashSet<Service>();
        services.add(new Service("org.apache.aries.blueprint.NamespaceHandler", Services.createProperties("key", "value")));
        services.add(new Service("some.other.class.Name"));

        assertTrue("Services are available",
                   Services.isAvailable(context, services));
        verify(context);

        reset(context);
        
        replay(context);
        services.clear();
        services.add(new Service("org.apache.aries.blueprint.NamespaceHandler", Services.createProperties("key", "not_the_right_value")));
        services.add(new Service("some.other.class.Name"));

        assertFalse("Services are not available",
                    Services.isAvailable(context, services));
        verify(context);
    }

    private static ServiceReference[] someReferences() {
        return new ServiceReference[] {createMock(ServiceReference.class)};
    }
}
