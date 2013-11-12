package org.fusesource.fabric.features;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.utils.features.FeatureUtils.search;

/**
 * A FeaturesService implementation for Fabric managed containers.
 */
@ThreadSafe
@Component(name = "org.fusesource.fabric.features", description = "Fabric Features Service", immediate = true)
@Service(FeaturesService.class)
public final class FabricFeaturesServiceImpl extends AbstractComponent implements FeaturesService, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesService.class);

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @GuardedBy("this") private final Set<Repository> repositories = new HashSet<Repository>();
    @GuardedBy("this") private final Set<Feature> allfeatures = new HashSet<Feature>();
    @GuardedBy("this") private final Set<Feature> installed = new HashSet<Feature>();

    @Activate
    void activate() {
        fabricService.get().trackConfiguration(this);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        fabricService.get().untrackConfiguration(this);
    }

    @Override
    public synchronized void run() {
        assertValid();
        repositories.clear();
        allfeatures.clear();
        installed.clear();
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
    public synchronized Repository[] listRepositories() {
        assertValid();
        if (repositories.isEmpty()) {
            Set<String> repositoryUris = new LinkedHashSet<String>();
            Container container = fabricService.get().getCurrentContainer();
            Version version = container.getVersion();
            Profile[] profiles = version.getProfiles();
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
                    RepositoryImpl r = new RepositoryImpl(new URI(uri));
                    r.load();
                    repositories.add(r);
                } catch (URISyntaxException e) {
                    LOGGER.debug("Error while adding repository with uri {}.", uri);
                } catch (IOException e) {
                    LOGGER.debug("Error while loading repository with uri {}.", uri);
                }
            }
        }
        return repositories.toArray(new Repository[repositories.size()]);
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
    public synchronized Feature[] listFeatures() throws Exception {
        assertValid();
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
    public synchronized Feature[] listInstalledFeatures() {
        assertValid();
        if (installed.isEmpty()) {
            try {
                Map<String, Map<String, Feature>> allFeatures = getFeatures(listProfileRepositories());
                Container container = fabricService.get().getCurrentContainer();
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
    public synchronized boolean isInstalled(Feature feature) {
        assertValid();
        if (installed.isEmpty()) {
            listInstalledFeatures();
        }
        return installed.contains(feature);
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


    private Map<String, Map<String, Feature>> getFeatures(Repository[] repositories) throws Exception {
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
    private Repository[] listProfileRepositories() {
        Set<String> repositoryUris = new LinkedHashSet<String>();
        Set<Repository> repositories = new LinkedHashSet<Repository>();
        //The method is only called when fabricService is available so no need to guard for null values.
        Container container = fabricService.get().getCurrentContainer();
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
     */
    private void addRepositoryUri(String uri, Set<String> repositoryUris) {
        if (repositoryUris.contains(uri)) {
            return;
        }
        repositoryUris.add(uri);
        try {
            RepositoryImpl repository = new RepositoryImpl(new URI(uri));
            repository.load();
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
     */
    private void addProfiles(Profile profile, Set<Profile> profiles) {
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
     */
    private void addFeatures(Feature feature, Set<Feature> features) {
        if (features.contains(feature)) {
            return;
        }

        features.add(feature);
        for (Feature dependency : feature.getDependencies()) {
            addFeatures(search(dependency.getName(), dependency.getVersion(), repositories), features);
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
