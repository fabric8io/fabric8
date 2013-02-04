package org.fusesource.fabric.features;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureValidationUtil;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.tracker.NodeEvent;
import org.linkedin.zookeeper.tracker.NodeEventsListener;
import org.linkedin.zookeeper.tracker.ZKStringDataReader;
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.utils.features.FeatureUtils.search;

/**
 * A FeaturesService implementation for Fabric managed containers.
 */
public class FabricFeaturesServiceImpl implements FeaturesService, NodeEventsListener<String>, LifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesService.class);

    private FabricService fabricService;
    private IZKClient zooKeeper;

    private ZooKeeperTreeTracker<String> profilesTracker;


    private final Set<Repository> repositories = new HashSet<Repository>();
    private final Set<Feature> allfeatures = new HashSet<Feature>();
    private final Set<Feature> installed = new HashSet<Feature>();

    public void init() throws Exception {
    }

    public void destroy() throws Exception {
        profilesTracker.destroy();
    }

    @Override
    public void onEvents(Collection<NodeEvent<String>> nodeEvents) {
        repositories.clear();
        allfeatures.clear();
        installed.clear();
    }

    @Override
    public void onConnected() {
        profilesTracker = new ZooKeeperTreeTracker<String>(zooKeeper, new ZKStringDataReader(), ZkPath.CONFIG_VERSIONS.getPath());
        try {
            profilesTracker.track(this);
        } catch (InterruptedException e) {
            LOGGER.error("Error while setting tracker for Fabric Features Service.", e);
        } catch (KeeperException e) {
            LOGGER.error("Error while setting tracker for Fabric Features Service.", e);
        }
        onEvents(null);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void validateRepository(URI uri) throws Exception {
        FeatureValidationUtil.validate(uri);
    }

    @Override
    public void addRepository(URI uri) throws Exception {
        addRepository(uri, true);
    }

    @Override
    public void addRepository(URI uri, boolean b) throws Exception {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --repositories %s target-profile instead. See fabric:profile-edit --help for more information.", uri.toString()));
    }

    @Override
    public void removeRepository(URI uri) throws Exception {
        removeRepository(uri, true);
    }

    @Override
    public void removeRepository(URI uri, boolean b) throws Exception {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --delete --repositories %s target-profile instead. See fabric:profile-edit --help for more information.", uri.toString()));
    }

    @Override
    public void restoreRepository(URI uri) throws Exception {
    }

    /**
     * Lists all {@link Repository} entries found in any {@link Profile} of the current {@link Container} {@link Version}.
     *
     * @return
     */
    @Override
    public Repository[] listRepositories() {
        if (repositories.isEmpty()) {
            Set<String> repositoryUris = new LinkedHashSet<String>();
            Container container = fabricService.getCurrentContainer();
            Version version = container.getVersion();
            Profile[] profiles = fabricService.getProfiles(version.getName());
            if (profiles != null) {
                for (Profile profile : profiles) {
                    if (profile.getRepositories() != null) {
                        for (String uri : profile.getRepositories()) {
                            repositoryUris.add(uri);
                            addRepositoryUri(uri, repositoryUris);
                        }
                    }
                }
            }

            for (String uri : repositoryUris) {
                try {
                    repositories.add(new RepositoryImpl(new URI(uri)));
                } catch (URISyntaxException e) {
                    LOGGER.debug("Error while adding repository with uri {}.", uri);
                }
            }
        }
        return repositories.toArray(new Repository[repositories.size()]);
    }

    @Override
    public void installFeature(String s) throws Exception {
        installFeature(s, (EnumSet) null);
    }

    @Override
    public void installFeature(String s, EnumSet<Option> options) throws Exception {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --features %s target-profile instead. See fabric:profile-edit --help for more information.", s));
    }

    @Override
    public void installFeature(String s, String s2) throws Exception {
        installFeature(s, s2, null);
    }

    @Override
    public void installFeature(String s, String s2, EnumSet<Option> options) throws Exception {
        String featureName = s;
        if (s2 != null && s2.equals("0.0.0")) {
            featureName = s + "/" + s2;
        }
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --features %s target-profile instead. See fabric:profile-edit --help for more information.", featureName));
    }

    @Override
    public void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --features %s target-profile instead. See fabric:profile-edit --help for more information.", feature.getName()));
    }

    @Override
    public void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (Feature feature : features) {
            sb.append("--feature ").append(feature.getName());
        }
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --features %s target-profile instead. See fabric:profile-edit --help for more information.", sb.toString()));
    }

    @Override
    public void uninstallFeature(String s) throws Exception {
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --delete --features %s target-profile instead. See fabric:profile-edit --help for more information.", s));
    }

    @Override
    public void uninstallFeature(String s, String s2) throws Exception {
        String featureName = s;
        if (s2 != null && s2.equals("0.0.0")) {
            featureName = s + "/" + s2;
        }
        throw new UnsupportedOperationException(String.format("The container is managed by fabric, please use fabric:profile-edit --features %s target-profile instead. See fabric:profile-edit --help for more information.", featureName));
    }

    @Override
    public Feature[] listFeatures() throws Exception {
        if (allfeatures.isEmpty()) {
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
        }
        return allfeatures.toArray(new Feature[allfeatures.size()]);
    }

    @Override
    public Feature[] listInstalledFeatures() {
        if (installed.isEmpty()) {
            try {
                Map<String, Map<String, Feature>> allFeatures = getFeatures(listProfileRepositories());
                Container container = fabricService.getCurrentContainer();
                Profile[] profiles = container.getProfiles();

                if (profiles != null) {
                    for (Profile profile : profiles) {
                        List<String> featureNames = profile.getFeatures();
                        for (String featureName : featureNames) {
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
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error retrieveing features.", e);
            }
        }
        return installed.toArray(new Feature[installed.size()]);
    }


    @Override
    public boolean isInstalled(Feature feature) {
        if (installed.isEmpty()) {
            listInstalledFeatures();
        }
        return installed.contains(feature);
    }

    @Override
    public Feature getFeature(String name) throws Exception {
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
        Feature[] features = listFeatures();
        for (Feature feature : features) {
            if (name.equals(feature.getName()) && version.equals(feature.getVersion())) {
                return feature;
            }
        }
        return null;
    }


    protected Map<String, Map<String, Feature>> getFeatures() throws Exception {
        return getFeatures(listRepositories());
    }

    protected Map<String, Map<String, Feature>> getFeatures(Repository[] repositories) throws Exception {
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
     *
     * @return
     */
    private Repository[] listProfileRepositories() {
        Set<String> repositoryUris = new LinkedHashSet<String>();
        Set<Repository> repositories = new LinkedHashSet<Repository>();
        Container container = fabricService.getCurrentContainer();
        Set<Profile> profilesWithParents = new HashSet<Profile>();

        Profile[] profiles = container.getProfiles();
        if (profiles != null) {
            for (Profile profile : profiles) {
                addProfiles(profile, profilesWithParents);
            }
            for (Profile profile : profilesWithParents) {
                if (profile.getRepositories() != null) {
                    for (String uri : profile.getRepositories()) {
                        repositoryUris.add(uri);
                        addRepositoryUri(uri, repositoryUris);
                    }
                }
            }
        }

        for (String uri : repositoryUris) {
            try {
                repositories.add(new RepositoryImpl(new URI(uri)));
            } catch (URISyntaxException e) {
                LOGGER.debug("Error while adding repository with uri {}.", uri);
            }
        }
        return repositories.toArray(new Repository[repositories.size()]);
    }


    /**
     * Adds the {@link URI} of {@link Feature} {@link Repository} and its internals to the set of repositories {@link URI}s.
     *
     * @param uri
     * @param repositoryUris
     */
    protected void addRepositoryUri(String uri, Set<String> repositoryUris) {
        if (repositoryUris.contains(uri)) {
            return;
        }
        repositoryUris.add(uri);
        try {
            Repository repository = new RepositoryImpl(new URI(uri));
            URI[] internalUris = repository.getRepositories();
            if (internalUris != null) {
                for (URI u : internalUris) {
                    addRepositoryUri(u.toString(), repositoryUris);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error while adding internal repositories of {}.", uri);
        }
    }

    /**
     * Adds {@link Profile} and its parents to the set of {@link Profile}s.
     *
     * @param profile
     * @param profiles
     */
    protected void addProfiles(Profile profile, Set<Profile> profiles) {
        if (profiles.contains(profile)) {
            return;
        }

        profiles.add(profile);
        for (Profile parent : profile.getParents()) {
            addProfiles(parent, profiles);
        }
    }

    /**
     * Adds {@link Feature} and its dependencies to the set of {@link Feature}s.
     *
     * @param feature
     * @param features
     */
    protected void addFeatures(Feature feature, Set<Feature> features) {
        if (features.contains(feature)) {
            return;
        }

        features.add(feature);
        for (Feature dependency : feature.getDependencies()) {
            addFeatures(search(dependency.getName(), dependency.getVersion(), repositories), features);
        }
    }


    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}
