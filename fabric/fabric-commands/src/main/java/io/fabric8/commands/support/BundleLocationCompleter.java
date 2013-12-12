package io.fabric8.commands.support;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

public class BundleLocationCompleter implements Completer {

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
