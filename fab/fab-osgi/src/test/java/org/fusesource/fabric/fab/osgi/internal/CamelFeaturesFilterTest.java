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
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link FabConnection.CamelFeaturesFilter}
 */
public class CamelFeaturesFilterTest {

    @Test
    public void testNoMatchesForCustomerBundle() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);
        replay(service);

        FabConnection.CamelFeaturesFilter filter = new FabConnection.CamelFeaturesFilter(service);
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

        FabConnection.CamelFeaturesFilter filter = new FabConnection.CamelFeaturesFilter(service);
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

        FabConnection.CamelFeaturesFilter filter = new FabConnection.CamelFeaturesFilter(service);
        assertFalse("camel-blueprint feature should not have replaced the dependency if no matching feature is available",
                    filter.matches(DependencyTree.newBuilder("org.apache.camel", "camel-blueprint", "2.9.0").build()));
        assertEquals(0, filter.getCollection().size());
    }


}
