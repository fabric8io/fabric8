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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.fusesource.fabric.fab.osgi.ServiceConstants.INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link FabResolverFactoryImpl.CamelFeaturesFilter}
 */
public class CamelFeaturesFilterTest {

    @Test
    public void testIsEnabled() {
        FeaturesService service = createNiceMock(FeaturesService.class);
        replay(service);

        FabResolverFactoryImpl.CamelFeaturesFilter filter = new FabResolverFactoryImpl.CamelFeaturesFilter(service);

        FabClassPathResolver resolver = new FabClassPathResolver(new FabClassPathResolverTest.MockFabFacade(), new Properties(), null);
        assertTrue("No configuration specified - filter should be enabled", filter.isEnabled(resolver));

        resolver = new FabClassPathResolver(new FabClassPathResolverTest.MockFabFacade(), new Properties(), null) {
            Map<String, String> properties = Services.createProperties(INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION, "org.apache.cxf");

            @Override
            public String getManifestProperty(String name) {
                return properties.get(name);
            }
        };
        assertTrue("Only CXF to be skipped - filter should be enabled", filter.isEnabled(resolver));

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

        FabResolverFactoryImpl.CamelFeaturesFilter filter = new FabResolverFactoryImpl.CamelFeaturesFilter(service);
        assertFalse("camel-blueprint feature should not have replaced the customer dependency",
                    filter.matches(DependencyTree.newBuilder("com.customer.group.id", "camel-blueprint", "2.9.0").build()));
        assertEquals(0, filter.getCollection().size());
    }

    @Test
    public void testMatches() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        Feature feature = createNiceMock(Feature.class);
        expect(feature.getName()).andReturn("camel-blueprint").anyTimes();
        expect(feature.getVersion()).andReturn("2.9.0.fuse-build-01").anyTimes();
        expect(service.getFeature("camel-blueprint")).andReturn(feature);
        replay(service, feature);

        FabResolverFactoryImpl.CamelFeaturesFilter filter = new FabResolverFactoryImpl.CamelFeaturesFilter(service);
        assertTrue("camel-blueprint feature should have replaced the dependency",
                   filter.matches(DependencyTree.newBuilder("org.apache.camel", "camel-blueprint", "2.9.0").build()));
        assertEquals(1, filter.getCollection().size());
        assertTrue(filter.getCollection().contains("camel-blueprint/2.9.0.fuse-build-01"));
    }

    @Test
    public void testNoMatchForCamelJar() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        Feature feature = createNiceMock(Feature.class);
        expect(feature.getName()).andReturn("camel-blueprint").anyTimes();
        expect(feature.getVersion()).andReturn("2.9.0.fuse-build-01").anyTimes();
        expect(service.getFeature("camel-blueprint")).andReturn(null);
        replay(service, feature);

        FabResolverFactoryImpl.CamelFeaturesFilter filter = new FabResolverFactoryImpl.CamelFeaturesFilter(service);
        assertFalse("camel-blueprint feature should not have replaced the dependency if no matching feature is available",
                    filter.matches(DependencyTree.newBuilder("org.apache.camel", "camel-blueprint", "2.9.0").build()));
        assertEquals(0, filter.getCollection().size());
    }


}
