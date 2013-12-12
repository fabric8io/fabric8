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

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import io.fabric8.fab.DependencyTree;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link Features}
 */
public class FeaturesTest {

    @Test
    public void testGetRequiredLocations() {
        Feature feature =
            createMockFeatureWithOptionalBundles("feature-a",
                                                 "mvn:required/bundle/1.0",
                                                 "mvn:optional/bundle1/1.0",
                                                 "mvn:optional/bundle1/1.0");

        List<String> required = Features.getRequiredLocations(feature);
        assertEquals(1, required.size());
        assertTrue(required.contains("mvn:required/bundle/1.0"));
    }

    @Test
    public void testGetFeatureForBundle() {
        Feature feature1 =
            createMockFeatureWithOptionalBundles("feature-a",
                                                 "mvn:required/bundle1/1.0",
                                                 "mvn:optional/bundle1/1.0");
        Feature feature2 =
            createMockFeatureWithOptionalBundles("feature-b",
                                                 "mvn:required/bundle2/1.0",
                                                 "mvn:optional/bundle2/1.0");

        Feature[] features = new Feature[] { feature1, feature2 };

        // find matching feature that contains the required bundle specified
        assertEquals("feature-a", Features.getFeatureForBundle(features, "mvn:required/bundle1/1.0").getName());
        assertEquals("feature-b", Features.getFeatureForBundle(features, "mvn:required/bundle2/1.0").getName());

        // no matches for optional bundles or bundles not found in any feature
        assertNull(Features.getFeatureForBundle(features, "mvn:optional/bundle1/1.0°"));
        assertNull(Features.getFeatureForBundle(features, "mvn:required/bundle3/1.0°"));

        // let's repeat a few of those tests, now with DependencyTree instances instead
        assertEquals("feature-a",
                     Features.getFeatureForBundle(features, DependencyTree.newBuilder("required", "bundle1", "1.0").build()).getName());
        assertNull(Features.getFeatureForBundle(features, DependencyTree.newBuilder("optional", "bundle1", "1.0").build()));

    }

    public static Feature createMockFeatureWithOptionalBundles(String name, String required, String... optionals) {
        Feature feature = createNiceMock(Feature.class);
        expect(feature.getName()).andReturn(name).anyTimes();
        expect(feature.getVersion()).andReturn("1.0").anyTimes();

        List<BundleInfo> infoList = new LinkedList<BundleInfo>();
        infoList.add(createMockBundleInfo(required, false));
        for (String optional : optionals) {
            infoList.add(createMockBundleInfo(optional, true));
        }
        expect(feature.getBundles()).andReturn(infoList).anyTimes();

        replay(feature);
        return feature;
    }

    private static BundleInfo createMockBundleInfo(String location, boolean dependency) {
        BundleInfo info = createNiceMock(BundleInfo.class);
        expect(info.getLocation()).andReturn(location).anyTimes();
        expect(info.isDependency()).andReturn(dependency).anyTimes();
        replay(info);
        return info;
    }


}
