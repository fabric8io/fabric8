package org.fusesource.fabric.commands.support;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

public class BundleCompleter implements Completer {

    private FeaturesService featuresService;
    private Set<String> locations = new LinkedHashSet<String>();


    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        delegate.getStrings().addAll(locations);
        int complete = delegate.complete(buffer, cursor, candidates);
        if (complete > 0) {
            return complete;
        } else {
            Set<Feature> features = getFeatures();
            locations = getFeatureLocations(features);
            delegate.getStrings().clear();
            delegate.getStrings().addAll(locations);
            return delegate.complete(buffer, cursor, candidates);
        }
    }

    private Set<Feature> getFeatures() {
        Set<Feature> features = new LinkedHashSet<Feature>();
        try {
            for (Feature feature : featuresService.listFeatures()) {
                features.addAll(getFeatures(feature));
            }
        } catch (Exception e) {
            //Ignore
        }
        return features;
    }

    private Set<Feature> getFeatures(Feature feature) {
        Set<Feature> features = new LinkedHashSet<Feature>();
        features.add(feature);
        for (Feature dependency : feature.getDependencies()) {
            features.add(dependency);
        }
        return features;
    }

    private Set<String> getFeatureLocations(Set<Feature> features) {
        Set<String> bundleLocations = new LinkedHashSet<String>();
        for (Feature feature : features) {
            for (BundleInfo info : feature.getBundles()) {
                bundleLocations.add(info.getLocation());
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
