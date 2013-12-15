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

import java.util.LinkedList;
import java.util.List;

/**
 * Helper methods for working with Karaf {@link Feature}s
 */
public class Features {

    /**
     * Extract the required bundle locations from a feature, omitting bundles that are marked with <code>dependency="true"</code>
     *
     * @param feature the Karaf feature
     * @return the list of required bundle locations
     */
    public static List<String> getRequiredLocations(Feature feature) {
        List<String> result = new LinkedList<String>();

        for (BundleInfo info : feature.getBundles()) {
            if (!info.isDependency()) {
                result.add(info.getLocation());
            }
        }

        return result;
    }

    /**
     * Find the feature that contains the required bundle location.
     *
     * @param features the list of features to search
     * @param bundle the required bundle location URL
     * @return the feature found or <code>null</code> if there was no matching feature
     */
    public static Feature getFeatureForBundle(Feature[] features, String bundle) {
        for (Feature feature : features) {
            if (getRequiredLocations(feature).contains(bundle)) {
                return feature;
            }
        }
        return null;
    }

    /**
     * Find the feature that contains bundle corresponding to the dependency tree item
     *
     * @param features the list of features to search
     * @param dependency the required dependency tree item
     * @return the feature found or <code>null</code> if there was no matching feature
     */
    public static Feature getFeatureForBundle(Feature[] features, DependencyTree dependency) {
        return getFeatureForBundle(features,
                                   String.format("mvn:%s/%s/%s",
                                                 dependency.getGroupId(),
                                                 dependency.getArtifactId(),
                                                 dependency.getVersion()));
    }
}
