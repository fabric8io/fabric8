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
package org.fusesource.fabric.fab.osgi.internal;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.util.Services;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.fusesource.fabric.fab.osgi.ServiceConstants.INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION;
import static org.junit.Assert.*;

/**
 * Test cases for {@link org.fusesource.fabric.fab.osgi.internal.FabResolverFactoryImpl.CXFFeaturesFilter}
 */
public class CxfFeaturesFilterTest {

    @Test
    public void testIsEnabled() {
        FeaturesService service = createNiceMock(FeaturesService.class);
        replay(service);

        FabResolverFactoryImpl.CXFFeaturesFilter filter = new FabResolverFactoryImpl.CXFFeaturesFilter(service);

        FabClassPathResolver resolver = new FabClassPathResolver(new FabClassPathResolverTest.MockFabFacade(), new Properties(), null);
        assertTrue("No configuration specified - filter should be enabled", filter.isEnabled(resolver));

        resolver = new FabClassPathResolver(new FabClassPathResolverTest.MockFabFacade(), new Properties(), null) {
            Map<String, String> properties = Services.createProperties(INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION, "org.apache.camel");

            @Override
            public String getManifestProperty(String name) {
                return properties.get(name);
            }
        };
        assertTrue("Only Camel to be skipped - filter should be enabled", filter.isEnabled(resolver));

        resolver = new FabClassPathResolver(new FabClassPathResolverTest.MockFabFacade(), new Properties(), null) {
            Map<String, String> properties = Services.createProperties(INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION, "org.apache.camel org.apache.cxf");

            @Override
            public String getManifestProperty(String name) {
                return properties.get(name);
            }
        };
        assertFalse("Camel and CXF to be skipped - filter should be disabled", filter.isEnabled(resolver));
    }

    @Test
    public void testNoMatchesForCustomerBundle() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        replay(service);

        FabResolverFactoryImpl.CXFFeaturesFilter filter = new FabResolverFactoryImpl.CXFFeaturesFilter(service);
        assertFalse("cxf feature should not have replaced the customer dependency",
                    filter.matches(DependencyTree.newBuilder("com.customer.group.id", "cxf-bundle", "2.5.0").build()));
        assertEquals(0, filter.getCollection().size());
    }

    @Test
    public void testMatchesCxfBundle() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        Feature feature = createNiceMock(Feature.class);
        expect(feature.getName()).andReturn("cxf").anyTimes();
        expect(feature.getVersion()).andReturn("2.5.0.fuse-build-01").anyTimes();
        expect(service.getFeature("cxf")).andReturn(feature);
        replay(service, feature);

        FabResolverFactoryImpl.CXFFeaturesFilter filter = new FabResolverFactoryImpl.CXFFeaturesFilter(service);
        assertTrue("cxf feature should have replaced the dependency",
                   filter.matches(DependencyTree.newBuilder("org.apache.cxf", "cxf-bundle", "2.5.0").build()));
        assertEquals(1, filter.getCollection().size());
        assertTrue(filter.getCollection().contains("cxf/2.5.0.fuse-build-01"));
    }

    @Test
    public void testMatchesCxfStsBundle() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        Feature feature = createNiceMock(Feature.class);
        expect(feature.getName()).andReturn("cxf-sts").anyTimes();
        expect(feature.getVersion()).andReturn("2.5.0.fuse-build-01").anyTimes();
        expect(service.getFeature("cxf-sts")).andReturn(feature);
        replay(service, feature);

        FabResolverFactoryImpl.CXFFeaturesFilter filter = new FabResolverFactoryImpl.CXFFeaturesFilter(service);
        assertTrue("cxf-sts feature should have replaced the dependency",
                filter.matches(DependencyTree.newBuilder("org.apache.cxf.sts", "cxf-sts-core", "2.5.0").build()));
        assertEquals(1, filter.getCollection().size());
        assertTrue(filter.getCollection().contains("cxf-sts/2.5.0.fuse-build-01"));
    }

    @Test
    public void testMatchesCxfWsnBundle() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        Feature feature = createNiceMock(Feature.class);
        expect(feature.getName()).andReturn("cxf-wsn").anyTimes();
        expect(feature.getVersion()).andReturn("2.5.0.fuse-build-01").anyTimes();
        expect(service.getFeature("cxf-wsn")).andReturn(feature);
        replay(service, feature);

        FabResolverFactoryImpl.CXFFeaturesFilter filter = new FabResolverFactoryImpl.CXFFeaturesFilter(service);
        assertTrue("cxf-wsn feature should have replaced the dependency",
                filter.matches(DependencyTree.newBuilder("org.apache.cxf.wsn", "cxf-wsn-core", "2.5.0").build()));
        assertEquals(1, filter.getCollection().size());
        assertTrue(filter.getCollection().contains("cxf-wsn/2.5.0.fuse-build-01"));
    }
}
