/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.esb.itests.pax.exam.karaf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

/**
 * This test performs no assertions. It just displays duplicate bundles.
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class DuplicateBundlesTest extends EsbTestSupport {

    @Test
    public void testDulicates() throws Exception {
        FeaturesService featuresService = ServiceLocator.getOsgiService(FeaturesService.class);
        Feature[] features = featuresService.listFeatures();
        List<BundleInfo> bundles = new ArrayList<BundleInfo>();

        Assert.assertNotNull(features);
        for (Feature feature:features) {
            System.out.println("Collecting bundles from feature:" + feature.getName());
            populateBundles(feature,bundles);
        }

        //Install all bundles
        for(BundleInfo bundle:bundles) {
            try{
            bundleContext.installBundle(bundle.getLocation(),null);
            }catch(Exception ex) {
             //Ignore
            }
        }

        //Check For duplicates
        Map<String, Version> bundleMap = new HashMap<String,Version>();
        Bundle[] installedBundles = bundleContext.getBundles();
        Boolean duplicateExists = false;
        for (Bundle bundle : installedBundles) {
            if (bundleMap.containsKey(bundle.getSymbolicName()) && !bundleMap.get(bundle.getSymbolicName()).equals(bundle.getVersion())) {
               duplicateExists = true;
               System.out.println("Duplicate bundle:" + bundle.getSymbolicName());
               System.out.println(executeCommand("osgi:list -t 0 -s | grep " + "\" "+bundle.getSymbolicName()+" \""));
            } else {
                bundleMap.put(bundle.getSymbolicName(),bundle.getVersion());
            }
        }
    }

    public void populateBundles(Feature feature, List<BundleInfo> bundles) {
        bundles.addAll(feature.getBundles());
        for (Feature dependency : feature.getDependencies()) {
            populateBundles(dependency, bundles);
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                esbDistributionConfiguration(), keepRuntimeFolder(),
                editConfigurationFilePut("system.properties", "esb.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID)),
                logLevel(LogLevelOption.LogLevel.INFO)};
    }
}
