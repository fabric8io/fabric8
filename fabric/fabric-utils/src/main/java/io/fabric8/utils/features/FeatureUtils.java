package io.fabric8.utils.features;

import java.util.Collection;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.osgi.framework.Version;

public class FeatureUtils {

    private static final String DEFAULT_VERSION = "0.0.0";

    private FeatureUtils() {
        //Utility Class
    }

    public static Feature search(String key, Collection<Repository> repositories) {
        String[] split = key.split("/");
        String name = split[0].trim();
        String version = null;
        if (split.length == 2) {
            version = split[1].trim();
        }
        if (version == null || version.length() == 0) {
            version = DEFAULT_VERSION;
        }
        return search(name, version, repositories);
    }

    public static Feature search(String name, String version, Collection<Repository> repositories) {
        VersionRange range = new VersionRange(version, false, true);
        Feature bestFeature = null;
        Version bestVersion = null;
        for (Repository repo : repositories) {
            Feature[] features;
            try {
                features = repo.getFeatures();
            } catch (Exception e) {
                // This should not happen as the repository has been loaded already
                throw new IllegalStateException(e);
            }
            for (Feature feature : features) {
                if (name.equals(feature.getName())) {
                    Version v = new Version(VersionCleaner.clean(feature.getVersion()));
                    if (range.contains(v)) {
                        if (bestVersion == null || bestVersion.compareTo(v) < 0) {
                            bestFeature = feature;
                            bestVersion = v;
                        }
                    }
                }
            }
        }
        return bestFeature;
    }
}
