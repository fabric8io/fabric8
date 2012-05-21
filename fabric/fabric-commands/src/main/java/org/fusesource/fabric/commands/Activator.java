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

package org.fusesource.fabric.commands;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.features.command.completers.FeatureRepositoryCompleter;
import org.fusesource.fabric.commands.support.BundleCompleter;
import org.fusesource.fabric.commands.support.FeaturesCompleterDelegate;
import org.fusesource.fabric.commands.support.FeaturesRepositoryCompleterDelegate;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private Runnable featuresServiceSupport;
    private Runnable featureBundleLocationSupport;


    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle. This method
     * can be used to register services or to allocate any resources that this
     * bundle needs.
     * <p/>
     * <p/>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this
     *                   bundle is marked as stopped and the Framework will remove this
     *                   bundle's listeners, unregister all services registered by this
     *                   bundle, and release all services used by this bundle.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        try {
            featuresServiceSupport = new FeaturesServiceSupport(context);
            featureBundleLocationSupport = new FeatureBundleLocationSupport(context);
        } catch (NoClassDefFoundError ex) {
            LOGGER.warn("Feature service is not present, Feature / Repository completion will be disabled");
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In general, this
     * method should undo the work that the <code>BundleActivator.start</code>
     * method started. There should be no active threads that were started by
     * this bundle when this bundle returns. A stopped bundle must not call any
     * Framework objects.
     * <p/>
     * <p/>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the
     *                   bundle is still marked as stopped, and the Framework will remove
     *                   the bundle's listeners, unregister all services registered by the
     *                   bundle, and release all services used by the bundle.
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        if (featuresServiceSupport != null) {
            featuresServiceSupport.run();
        }

        if (featureBundleLocationSupport != null) {
            featureBundleLocationSupport.run();
        }
    }

    private static class FeaturesServiceSupport implements Runnable {

        private BundleContext context;
        private ServiceRegistration registration;
        private ServiceTracker serviceTracker;


        private FeaturesServiceSupport(BundleContext context) {
            this.context = context;
            FeaturesService featuresService = getFeaturesService();

            AllFeatureCompleter allFeatureCompleter = new AllFeatureCompleter();
            allFeatureCompleter.setFeaturesService(featuresService);
            FeaturesCompleterDelegate.DELEGATE = allFeatureCompleter;

            FeatureRepositoryCompleter featureRepositoryCompleter = new FeatureRepositoryCompleter();
            featureRepositoryCompleter.setFeaturesService(featuresService);
            FeaturesRepositoryCompleterDelegate.DELEGATE = featureRepositoryCompleter;
        }

        public void run() {
            FeaturesCompleterDelegate.DELEGATE = null;
            FeaturesRepositoryCompleterDelegate.DELEGATE = null;
            if (registration != null) {
                registration.unregister();
            }

        }

        private FeaturesService getFeaturesService() {
            serviceTracker = new ServiceTracker(context, FeaturesService.class.getName(), null);
            serviceTracker.open();
            return (FeaturesService) serviceTracker.getService();
        }
    }

    private static class FeatureBundleLocationSupport implements Callable<Set<String>>, Runnable {
        private BundleContext context;
        private ServiceRegistration registration;
        private ServiceTracker serviceTracker;
        private FeaturesService featuresService;

        private FeatureBundleLocationSupport(BundleContext context) {
            this.context = context;
            featuresService = getFeaturesService();
            BundleCompleter.BUNDLE_LOCATION_SET = this;
        }

        public void run() {
            if (registration != null) {
                registration.unregister();
            }

        }

        private FeaturesService getFeaturesService() {
            if (serviceTracker == null) {
                serviceTracker = new ServiceTracker(context, FeaturesService.class.getName(), null);
                serviceTracker.open();
            }
            return (FeaturesService) serviceTracker.getService();
        }

        @Override
        public Set<String> call()  {

            return getFeatureLocations(getFeatures());
        }

        private Set<String> getFeatures() {
            Set<String> features = new LinkedHashSet<String>();
            try {
                for (Feature feature : featuresService.listFeatures()) {
                    features.addAll(getFeatures(feature.getName()));
                }
            } catch (Exception e) {
                //Ignore
            }
            return features;
        }

        private Set<String> getFeatures(String feature) throws Exception {
            Set<String> features = new LinkedHashSet<String>();
            features.add(feature);
            Feature f = featuresService.getFeature(feature);
            for (Feature dependency : f.getDependencies()) {
                features.add(dependency.getName());
            }
            return features;
        }

        private Set<String> getFeatureLocations(Set<String> features) {
            Set<String> bundleLocations = new LinkedHashSet<String>();
            for (String feature : features) {
                try {
                    Feature f = featuresService.getFeature(feature);
                    for (BundleInfo info : f.getBundles()) {
                        bundleLocations.add(info.getLocation());
                    }
                } catch (Exception e) {
                    //Ignore
                }
            }
            return bundleLocations;
        }
    }
}
