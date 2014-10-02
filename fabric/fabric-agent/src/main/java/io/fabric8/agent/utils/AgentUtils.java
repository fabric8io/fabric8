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
package io.fabric8.agent.utils;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

//import io.fabric8.agent.download.DownloadFuture;
import io.fabric8.agent.download.DownloadCallback;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.Downloader;
import io.fabric8.agent.download.StreamProvider;
import io.fabric8.agent.model.BundleInfo;
import io.fabric8.agent.model.Dependency;
import io.fabric8.agent.model.Feature;
import io.fabric8.agent.model.Repository;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.common.util.MultiException;
import io.fabric8.common.util.Strings;
import io.fabric8.maven.util.Parser;
import io.fabric8.service.VersionPropertyPointerResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.utils.PatchUtils.extractUrl;

public class AgentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentUtils.class);

    /**
     * Returns the location and parser map (i.e. the location and the parsed maven coordinates and artifact locations) of each bundle and feature
     * of the given profile
     */
    public static Map<String, Parser> getProfileArtifacts(FabricService fabricService, DownloadManager downloadManager, Profile profile) throws Exception {
        return getProfileArtifacts(fabricService, downloadManager, profile, null);
    }

    public static Map<String, Parser> getProfileArtifacts(FabricService fabricService, DownloadManager downloadManager, Profile profile, Callback<String> callback) throws Exception {
        List<String> bundles = profile.getBundles();
        Set<Feature> features = getFeatures(fabricService, downloadManager, profile);
        return getProfileArtifacts(fabricService, profile, bundles, features, callback);
    }

    public static Set<Feature> getFeatures(FabricService fabricService, DownloadManager downloadManager, Profile profile) throws Exception {
        Set<Feature> features = new HashSet<>();
        addFeatures(features, fabricService, downloadManager, profile);
        return features;
    }


    /**
     * Returns the location and parser map (i.e. the location and the parsed maven coordinates and artifact locations) of each bundle and feature
     */
    public static Map<String, Parser> getProfileArtifacts(FabricService fabricService, Profile profile, Iterable<String> bundles, Iterable<Feature> features) {
        return getProfileArtifacts(fabricService, profile, bundles, features, null);
    }

    public interface Callback<T> {

        /**
         * Callback when a non-maven based location is discovered
         */
        void call(T location);
    }

    /**
     * Returns the location and parser map (i.e. the location and the parsed maven coordinates and artifact locations) of each bundle and feature
     */
    public static Map<String, Parser> getProfileArtifacts(FabricService fabricService, Profile profile, Iterable<String> bundles, Iterable<Feature> features,
                                                          Callback<String> nonMavenLocationCallback) {
        Set<String> locations = new HashSet<>();
        for (Feature feature : features) {
            List<BundleInfo> bundleList = feature.getBundles();
            if (bundleList == null) {
                LOGGER.warn("No bundles for feature " + feature);
            } else {
                for (BundleInfo bundle : bundleList) {
                    locations.add(bundle.getLocation());
                }
            }
        }
        for (String bundle : bundles) {
            locations.add(bundle);
        }
        Map<String,Parser> artifacts = new HashMap<>();
        for (String location : locations) {
            try {
                if (location.contains("$")) {
                    ProfileService profileService = fabricService.adapt(ProfileService.class);
                    Profile overlay = profileService.getOverlayProfile(profile);
					location = VersionPropertyPointerResolver.replaceVersions(fabricService, overlay.getConfigurations(), location);
                }
                if (location.startsWith("mvn:") || location.contains(":mvn:")) {
                    Parser parser = Parser.parsePathWithSchemePrefix(location);
                    artifacts.put(location, parser);
                } else {
                    if (nonMavenLocationCallback != null) {
                        nonMavenLocationCallback.call(location);
                    }

                }
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to parse bundle URL: " + location + ". " + e, e);
            }
        }
        return artifacts;
    }

    /**
     * Extracts the {@link java.net.URI}/{@link org.apache.karaf.features.Repository} map from the profile.
     *
     *
     * @param fabricService
     * @param downloadManager
     * @param profile
     * @return
     * @throws java.net.URISyntaxException
     */
    protected static Map<String, Repository> getRepositories(FabricService fabricService, DownloadManager downloadManager, Profile profile) throws Exception {
        Set<String> uris = new HashSet<>();
        for (String repositoryUrl : profile.getRepositories()) {
            if (Strings.isNotBlank(repositoryUrl)) {
                String replacedUrl = repositoryUrl;
                if (repositoryUrl.contains("$")) {
                    replacedUrl = VersionPropertyPointerResolver.replaceVersions(fabricService, profile.getConfigurations(), repositoryUrl);
                }
                uris.add(replacedUrl);
            }
        }
        return downloadRepositories(downloadManager, uris).call();
    }

    /**
     * Adds the set of features to the given set for the given profile
     *
     * @param features
     * @param fabricService
     *@param downloadManager
     * @param profile   @throws Exception
     */
    public static void addFeatures(Set<Feature> features, FabricService fabricService, DownloadManager downloadManager, Profile profile) throws Exception {
        List<String> featureNames = profile.getFeatures();
        Map<String, Repository> repositories = getRepositories(fabricService, downloadManager, profile);
        for (String featureName : featureNames) {
            Feature feature = FeatureUtils.search(featureName, repositories.values());
            if (feature == null) {
                LOGGER.warn("Could not find feature " + featureName
                        + " for profile " + profile.getId()
                        + " in repositories " + repositories.keySet());
            } else {
                features.addAll(expandFeature(feature, repositories));
            }
        }
    }

    public static Set<Feature> expandFeature(Feature feature, Map<String, Repository> repositories) {
        Set<Feature> features = new HashSet<>();
        for (Dependency f : feature.getDependencies()) {
            Feature loaded = FeatureUtils.search(f.getName(), repositories.values());
            features.addAll(expandFeature(loaded, repositories));
        }
        features.add(feature);
        return features;
    }

    public static Callable<Map<String, Repository>> downloadRepositories(DownloadManager manager, Set<String> uris)
            throws MultiException, InterruptedException, MalformedURLException {
        final Map<String, Repository> repositories = new HashMap<>();
        final Downloader downloader = manager.createDownloader();
        for (String uri : uris) {
            downloader.download(uri, new DownloadCallback() {
                @Override
                public void downloaded(StreamProvider provider) throws Exception {
                    String uri = provider.getUrl();
                    Repository repository = new Repository(URI.create(uri));
                    repository.load(new FileInputStream(provider.getFile()), true);
                    synchronized (repositories) {
                        repositories.put(uri, repository);
                    }
                    for (URI repo : repository.getRepositories()) {
                        downloader.download(repo.toASCIIString(), this);
                    }
                }
            });
        }
        return new Callable<Map<String, Repository>>() {
            @Override
            public Map<String, Repository> call() throws Exception {
                downloader.await();
                return repositories;
            }
        };
    }

    public static Map<String, File> downloadLocations(DownloadManager manager, Collection<String> uris)
            throws MultiException, InterruptedException, MalformedURLException {
        final Map<String, File> files = new HashMap<>();
        final Downloader downloader = manager.createDownloader();
        for (String uri : uris) {
            downloader.download(uri, new DownloadCallback() {
                @Override
                public void downloaded(StreamProvider provider) throws Exception {
                    String uri = provider.getUrl();
                    File file = provider.getFile();
                    synchronized (files) {
                        files.put(uri, file);
                    }
                }
            });
        }
        downloader.await();
        return files;
    }


    /**
     * Downloads all the bundles and features for the given profile
     */
    public static Map<String, File> downloadProfileArtifacts(FabricService fabricService, DownloadManager downloadManager, Profile profile) throws Exception {
        List<String> bundles = profile.getBundles();
        Set<Feature> features = getFeatures(fabricService, downloadManager, profile);
        return downloadBundles(downloadManager, features, bundles, Collections.<String>emptySet());
    }

    public static Map<String, File> downloadBundles(DownloadManager manager, Iterable<Feature> features, Iterable<String> bundles, Set<String> overrides) throws Exception {
        return downloadBundles(manager, features, bundles, overrides, false);
    }

    public static Map<String, File> downloadBundles(DownloadManager manager, Iterable<Feature> features, Iterable<String> bundles, Set<String> overrides, boolean ignoreProfileUrls) throws Exception {
        Set<String> locations = new HashSet<>();
        for (Feature feature : features) {
            for (BundleInfo bundle : feature.getBundles()) {
                locations.add(bundle.getLocation());
            }
        }
        for (String bundle : bundles) {
            locations.add(bundle);
        }
        for (String override : overrides) {
            locations.add(extractUrl(override));
        }
        if (ignoreProfileUrls) {
            for (Iterator<String> it = locations.iterator(); it.hasNext(); ) {
                if (it.next().startsWith("profile:")) {
                    it.remove();
                }
            }
        }
        return downloadLocations(manager, locations);
    }

    public static void addMavenProxies(Dictionary<String, String> props, FabricService fabricService) {
        try {
            if (fabricService != null) {
                String httpUrl = fabricService.getCurrentContainer().getHttpUrl();
                StringBuilder sb = new StringBuilder();
                for (URI uri : fabricService.getMavenRepoURIs()) {
                    String mavenRepo = uri.toString();
                    if (mavenRepo.startsWith(httpUrl)) {
                        continue;
                    }
                    if (!mavenRepo.endsWith("/")) {
                        mavenRepo += "/";
                    }
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(mavenRepo);
                    sb.append("@snapshots@snapshotsUpdate=always");
                }
                String existingRepos = (String) props.get("org.ops4j.pax.url.mvn.repositories");
                if (existingRepos != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(existingRepos);
                }
                props.put("org.ops4j.pax.url.mvn.repositories", sb.toString());
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve maven proxy urls: " + e.getMessage());
            LOGGER.debug("Unable to retrieve maven proxy urls: " + e.getMessage(), e);
        }
    }

}
