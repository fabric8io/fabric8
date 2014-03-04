package io.fabric8.features;

import static io.fabric8.utils.features.FeatureUtils.search;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.internal.ProfileImpl;
import io.fabric8.internal.ProfileOverlayImpl;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureValidationUtil;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A FeaturesService implementation for Fabric managed containers.
 */
@ThreadSafe
@Component(name = "io.fabric8.features", label = "Fabric8 Features Service", immediate = true, metatype = false)
@Service(FeaturesService.class)
public final class FabricFeaturesServiceImpl extends AbstractComponent implements FeaturesService, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesService.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @GuardedBy("this")
    private final LoadingCache<String, Repository> repositories = CacheBuilder.newBuilder().build(new CacheLoader<String, Repository>() {
        @Override
        public Repository load(String uri) throws Exception {
            RepositoryImpl repository = new RepositoryImpl(new URI(uri));
            repository.load();
            return repository;
        }
    });

    @GuardedBy("this")
    private final Set<Repository> installedRepositories = new HashSet<Repository>();
    @GuardedBy("this")
    private final Set<Feature> installedFeatures = new HashSet<Feature>();

    @Activate
    void activate() {
        fabricService.get().trackConfiguration(this);
        activateComponent();
        executor.submit(this);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        fabricService.get().untrackConfiguration(this);
    }

    @Override
    public synchronized void run() {
        assertValid();
        repositories.invalidateAll();
        installedRepositories.clear();
        installedRepositories.addAll(Arrays.asList(listInstalledRepositories()));
        installedFeatures.clear();
        installedFeatures.addAll(Arrays.asList(listInstalledFeatures()));
    }

    @Override
    public void validateRepository(URI uri) throws Exception {
        assertValid();
        FeatureValidationUtil.validate(uri);
    }

    @Override
    public void addRepository(URI uri) throws Exception {
        unsupportedAddRepository(uri);
    }

    @Override
    public void addRepository(URI uri, boolean b) throws Exception {
        unsupportedAddRepository(uri);
    }

    private void unsupportedAddRepository(URI uri) {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --repositories %s target-profile instead. See fabric:profile-edit --help for more information.", uri.toString()));
    }

    @Override
    public void removeRepository(URI uri) throws Exception {
        unsupportedRemoveRepository(uri);
    }

    @Override
    public void removeRepository(URI uri, boolean b) throws Exception {
        unsupportedRemoveRepository(uri);
    }

    private void unsupportedRemoveRepository(URI uri) {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --delete --repositories %s target-profile instead. See fabric:profile-edit --help for more information.", uri.toString()));
    }

    @Override
    public void restoreRepository(URI uri) throws Exception {
    }

    /**
     * Lists all {@link Repository} entries found in any {@link Profile} of the current {@link Container} {@link Version}.
     */
    @Override
    public Repository[] listRepositories() {
        assertValid();
        Set<Repository> repos = new LinkedHashSet<Repository>();
        for (String uri : getAllProfilesOverlay().getRepositories()) {
            try {
                populateRepositories(uri, repos);
            } catch (Exception ex) {
                LOGGER.warn("Error while populating repositories from uri.", ex);
            }
        }

        return repos.toArray(new Repository[repos.size()]);
    }

    @Override
    public void installFeature(String s) throws Exception {
        unsupportedInstallFeature(s);
    }

    @Override
    public void installFeature(String s, EnumSet<Option> options) throws Exception {
        unsupportedInstallFeature(s);
    }

    @Override
    public void installFeature(String s, String s2) throws Exception {
        String featureName = s;
        if (s2 != null && s2.equals("0.0.0")) {
            featureName = s + "/" + s2;
        }
        unsupportedInstallFeature(featureName);
    }

    @Override
    public void installFeature(String s, String s2, EnumSet<Option> options) throws Exception {
        String featureName = s;
        if (s2 != null && s2.equals("0.0.0")) {
            featureName = s + "/" + s2;
        }
        unsupportedInstallFeature(featureName);
    }

    @Override
    public void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        unsupportedInstallFeature(feature.getName());
    }

    @Override
    public void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (Feature feature : features) {
            sb.append("--feature ").append(feature.getName());
        }
        unsupportedInstallFeature(sb.toString());
    }

    private void unsupportedInstallFeature(String s) {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --features %s target-profile instead. See fabric:profile-edit --help for more information.", s));
    }

    @Override
    public void uninstallFeature(String s) throws Exception {
        unsupportedUninstallFeature(s);
    }

    @Override
    public void uninstallFeature(String s, String s2) throws Exception {
        String featureName = s;
        if (s2 != null && s2.equals("0.0.0")) {
            featureName = s + "/" + s2;
        }
        unsupportedUninstallFeature(featureName);
    }

    private void unsupportedUninstallFeature(String s) {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --delete --features %s target-profile instead. See fabric:profile-edit --help for more information.", s));
    }

    @Override
    public Feature[] listFeatures() throws Exception {
        assertValid();
        Set<Feature> allfeatures = new HashSet<Feature>();
            Repository[] repositories = listRepositories();
            for (Repository repository : repositories) {
                try {
                    for (Feature feature : repository.getFeatures()) {
                        if (!allfeatures.contains(feature)) {
                            allfeatures.add(feature);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.debug("Could not load features from %s.", repository.getURI());
                }
            }
        return allfeatures.toArray(new Feature[allfeatures.size()]);
    }

    @Override
    public Feature[] listInstalledFeatures() {
        assertValid();
        Set<Feature> installed = new HashSet<Feature>();
            try {
                Map<String, Map<String, Feature>> allFeatures = getFeatures(installedRepositories);
                for (String featureName : fabricService.get().getCurrentContainer().getOverlayProfile().getFeatures()) {
                    try {
                        Feature f;
                        if (featureName.contains("/")) {
                            String[] parts = featureName.split("/");
                            String name = parts[0];
                            String version = parts[1];
                            f = allFeatures.get(name).get(version);
                        } else {
                            TreeMap<String, Feature> versionMap = (TreeMap<String, Feature>) allFeatures.get(featureName);
                            f = versionMap.lastEntry().getValue();
                        }
                        addFeatures(f, installed);
                    } catch (Exception ex) {
                        LOGGER.debug("Error while adding {} to the features list");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error retrieving features.", e);
            }
        return installed.toArray(new Feature[installed.size()]);
    }


    @Override
    public boolean isInstalled(Feature feature) {
        assertValid();
        return installedFeatures.contains(feature);
    }

    @Override
    public Feature getFeature(String name) throws Exception {
        assertValid();
        Feature[] features = listFeatures();
        for (Feature feature : features) {
            if (name.equals(feature.getName())) {
                return feature;
            }
        }
        return null;
    }

    @Override
    public Feature getFeature(String name, String version) throws Exception {
        assertValid();
        Feature[] features = listFeatures();
        for (Feature feature : features) {
            if (name.equals(feature.getName()) && version.equals(feature.getVersion())) {
                return feature;
            }
        }
        return null;
    }


    private Map<String, Map<String, Feature>> getFeatures(Iterable<Repository> repositories) throws Exception {
        Map<String, Map<String, Feature>> features = new HashMap<String, Map<String, Feature>>();
        for (Repository repo : repositories) {
            try {
                for (Feature f : repo.getFeatures()) {
                    if (features.get(f.getName()) == null) {
                        Map<String, Feature> versionMap = new TreeMap<String, Feature>();
                        versionMap.put(f.getVersion(), f);
                        features.put(f.getName(), versionMap);
                    } else {
                        features.get(f.getName()).put(f.getVersion(), f);
                    }
                }
            } catch (Exception ex) {
                LOGGER.debug("Could not load features from %s.", repo.getURI());
            }
        }
        return features;
    }


    /**
     * Lists all {@link Repository} enties found in the {@link Profile}s assigned to the current {@link Container}.
     */
    private Repository[] listInstalledRepositories() {
        Set<String> repositoryUris = new LinkedHashSet<String>();
        Set<Repository> repos = new LinkedHashSet<Repository>();

        Profile profile = fabricService.get().getCurrentContainer().getOverlayProfile();
        if (profile.getRepositories() != null) {
            for (String uri : profile.getRepositories()) {
                repositoryUris.add(uri);
            }
        }

        for (String uri : repositoryUris) {
            try {
                populateRepositories(uri, repos);
            } catch (Exception ex) {
                LOGGER.warn("Error while populating repositories from uri.", ex);
            }
        }

        return repos.toArray(new Repository[repos.size()]);
    }

    private void populateRepositories (String uri, Set<Repository> repos) throws Exception {
        Repository repository = repositories.get(uri);
        if (repository != null && !repos.contains(repository)) {
            repos.add(repository);
            for (URI u : repository.getRepositories()) {
                populateRepositories(u.toString(), repos);
            }
        }
    }

    /**
     * Adds {@link Feature} and its dependencies to the set of {@link Feature}s.
     */
    private void addFeatures(Feature feature, Set<Feature> features) {
        if (features.contains(feature)) {
            return;
        }

        features.add(feature);
        for (Feature dependency : feature.getDependencies()) {
            addFeatures(search(dependency.getName(), dependency.getVersion(), repositories.asMap().values()), features);
        }
    }

    private class VersionProfile extends ProfileImpl {

        private VersionProfile(Version version) {
            super("#version-" + version.getId(),
                    version.getId(),
                    fabricService.get());
        }

        @Override
        public Profile[] getParents() {
            return fabricService.get().getVersion(getVersion()).getProfiles();
        }

        @Override
        public Map<String, String> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public void setAttribute(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Container[] getAssociatedContainers() {
            return new Container[0];
        }

        @Override
        public Map<String, byte[]> getFileConfigurations() {
            return Collections.emptyMap();
        }

        @Override
        public void setFileConfigurations(Map<String, byte[]> configurations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Map<String, String>> getConfigurations() {
            return Collections.emptyMap();
        }

        @Override
        public void setConfigurations(Map<String, Map<String, String>> configurations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the time in milliseconds of the last modification of the profile.
         */
        @Override
        public String getProfileHash() {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Creates an aggregation of all available {@link Profile}s.
     */
    private Profile getAllProfilesOverlay() {
        FabricService fservice = fabricService.get();
        Container container = fservice.getCurrentContainer();
        Version version = container.getVersion();

        Profile p = new VersionProfile(version);
        return new ProfileOverlayImpl(p, fservice.getEnvironment(), true, fservice);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
