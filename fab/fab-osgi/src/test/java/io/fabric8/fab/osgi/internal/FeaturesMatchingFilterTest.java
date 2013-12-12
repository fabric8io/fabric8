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
package io.fabric8.fab.osgi.internal;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.FabConfiguration;
import io.fabric8.fab.osgi.ServiceConstants;
import io.fabric8.fab.osgi.util.FeaturesTest;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link FabResolverFactoryImpl.FeaturesMatchingFilter}
 */
public class FeaturesMatchingFilterTest {

    @Test
    public void testMatches() throws Exception {
        FeaturesService service = createMockFeaturesService();

        FabResolverFactoryImpl.FeaturesMatchingFilter filter = new FabResolverFactoryImpl.FeaturesMatchingFilter(service, new FabConfiguration() {
            @Override
            public String getStringProperty(String name) {
                // no configuration
                return null;
            }
        });
        assertTrue(filter.matches(DependencyTree.newBuilder("required", "bundle1", "1.0").build()));
        assertFalse(filter.matches(DependencyTree.newBuilder("optional", "bundle2", "1.0").build()));

        assertEquals(1, filter.getCollection().size());
        assertTrue(filter.getCollection().contains("feature-a/1.0"));
    }

    @Test
    public void testMatchesWithFilter() throws Exception {
        FeaturesService service = createMockFeaturesService();
        FabConfiguration configuration = new FabConfiguration() {

            @Override
            public String getStringProperty(String name) {
                if (ServiceConstants.INSTR_FAB_SKIP_MATCHING_FEATURE_DETECTION.equals(name)) {
                    return "required:*";
                }
                return null;
            }
        };

        // configuration defines that artifacts with groupid required should not be replaced
        FabResolverFactoryImpl.FeaturesMatchingFilter filter = new FabResolverFactoryImpl.FeaturesMatchingFilter(service, configuration);
        assertFalse(filter.matches(DependencyTree.newBuilder("required", "bundle1", "1.0").build()));
        assertFalse(filter.matches(DependencyTree.newBuilder("required", "bundle2", "1.0").build()));

        assertEquals(0, filter.getCollection().size());
    }

    private FeaturesService createMockFeaturesService() throws Exception {
        FeaturesService service = createNiceMock(FeaturesService.class);

        Feature feature1 =
            FeaturesTest.createMockFeatureWithOptionalBundles("feature-a",
                    "mvn:required/bundle1/1.0",
                    "mvn:optional/bundle1/1.0");
        Feature feature2 =
            FeaturesTest.createMockFeatureWithOptionalBundles("feature-b",
                                                              "mvn:required/bundle2/1.0",
                                                              "mvn:optional/bundle2/1.0");

        Feature[] features = new Feature[] { feature1, feature2 };
        expect(service.listFeatures()).andReturn(features).anyTimes();
        replay(service);
        return service;
    }

}
