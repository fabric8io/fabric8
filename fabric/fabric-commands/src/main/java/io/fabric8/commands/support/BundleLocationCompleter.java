/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.commands.support;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

@Component(immediate = true)
@Service({BundleLocationCompleter.class, Completer.class})
public class BundleLocationCompleter implements Completer {

    @Reference
    private FeaturesService featuresService;

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            delegate.getStrings().addAll(getFeatureLocations());
        } catch (Exception ex) {
            //ignore
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    private Set<String> getFeatureLocations() throws Exception {
        Set<String> bundleLocations = new LinkedHashSet<String>();
        for (Feature feature : featuresService.listFeatures()) {
            try {
                for (BundleInfo info : feature.getBundles()) {
                    bundleLocations.add(info.getLocation());
                }
            } catch (Exception e) {
                //Ignore
            }
        }
        return bundleLocations;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }
}
