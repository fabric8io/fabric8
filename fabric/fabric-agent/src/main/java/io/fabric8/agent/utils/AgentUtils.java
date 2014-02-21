/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.utils;

import io.fabric8.agent.mvn.Parser;
import io.fabric8.api.Profile;
import io.fabric8.service.VersionPropertyPointerResolver;
import io.fabric8.utils.Strings;
import io.fabric8.utils.features.FeatureUtils;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureValidationUtil;
import org.apache.karaf.features.internal.RepositoryImpl;
import io.fabric8.agent.download.DownloadFuture;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.FutureListener;
import io.fabric8.api.FabricService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static io.fabric8.utils.PatchUtils.extractUrl;

public class AgentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentUtils.class);

    public static final String FAB_PROTOCOL = "fab:";
    public static final String REQ_PROTOCOL = "req:";


    /**
     * Returns the location and parser map (i.e. the location and the parsed maven coordinates and artifact locations) of each bundle and feature
     * of the given profile
     */
    public static Map<String, Parser> getProfileArtifacts(DownloadManager downloadManager, Profile profile) throws Exception {
        List<String> bundles = profile.getBundles();
        Set<Feature> features = new HashSet<Feature>();
        addFeatures(features, downloadManager, profile);
        return getProfileArtifacts(profile, bundles, features);
    }


    /**
     * Returns the location and parser map (i.e. the location and the parsed maven coordinates and artifact locations) of each bundle and feature
     */
    public static Map<String, Parser> getProfileArtifacts(Profile profile, Iterable<String> bundles, Iterable<Feature> features) {
        Set<String> locations = new HashSet<String>();
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
        Map<String,Parser> artifacts = new HashMap<String, Parser>();
        for (String location : locations) {
            try {
                if (location.contains("$")) {
                    location = VersionPropertyPointerResolver.replaceVersions(profile.getOverlay().getConfigurations(), location);
                }
                // lets trim the "mvn:" prefix
                Parser parser = new Parser(location.substring(4));
                artifacts.put(location, parser);

            } catch (MalformedURLException e) {
                LOGGER.error("Failed to parse bundle URL: " + location + ". " + e, e);
            }
        }
        return artifacts;
    }

    public static void addRepository(DownloadManager manager, Map<URI, Repository> repositories, URI uri) throws Exception {
        if (!repositories.containsKey(uri)) {
            File file = manager.download(uri.toString()).await().getFile();
            FeatureValidationUtil.validate(file.toURI());
            //We are using the file uri instead of the maven url, because we want to make sure, that the repo can load.
            //If we used the maven uri instead then we would have to make sure that the download location is added to
            //the ops4j pax url configuration. Using the first is a lot safer and less prone to misconfigurations.
            RepositoryImpl repo = new RepositoryImpl(file.toURI());
            repositories.put(uri, repo);
            repo.load();
            for (URI ref : repo.getRepositories()) {
                addRepository(manager, repositories, ref);
            }
        }
    }

    /**
     * Extracts the {@link java.net.URI}/{@link org.apache.karaf.features.Repository} map from the profile.
     *
     * @param profile
     * @param downloadManager
     * @return
     * @throws java.net.URISyntaxException
     */
    protected static Map<URI, Repository> getRepositories(DownloadManager downloadManager, Profile profile) throws Exception {
        Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        for (String repositoryUrl : profile.getRepositories()) {
            if (Strings.isNotBlank(repositoryUrl)) {
                try {
                    // lets replace any version expressions
                    String replacedUrl = repositoryUrl;
                    if (repositoryUrl.contains("$")) {
                        replacedUrl = VersionPropertyPointerResolver.replaceVersions(profile.getConfigurations(), repositoryUrl);
                    }
                    URI repoUri = new URI(replacedUrl);
                    addRepository(downloadManager, repositories, repoUri);
                } catch (Exception e) {
                    LOGGER.warn("Failed to add repository " + repositoryUrl + " for profile " + profile.getId() + ". " + e);
                }
            }
        }
        return repositories;
    }

    /**
     * Adds the set of features to the given set for the given profile
     *
     * @param features
     * @param downloadManager
     * @param profile
     * @throws Exception
     */
    public static void addFeatures(Set<Feature> features, DownloadManager downloadManager, Profile profile) throws Exception {
        List<String> featureNames = profile.getFeatures();
        Map<URI, Repository> repositories = getRepositories(downloadManager, profile);
        for (String featureName : featureNames) {
            Feature feature = FeatureUtils.search(featureName, repositories.values());
            if (feature == null) {
                LOGGER.warn("Could not find feature " + featureName
                        + " for profile " + profile.getId()
                        + " in repositories " + repositories.keySet());
            } else {
                features.add(feature);
            }
        }
    }

    public static Map<String, Repository> loadRepositories(DownloadManager manager, Set<String> uris) throws Exception {
        RepositoryDownloader downloader = new RepositoryDownloader(manager);
        downloader.download(uris);
        return downloader.await();
    }

    public static Map<String, File> downloadBundles(DownloadManager manager, Set<Feature> features, Set<String> bundles, Set<String> overrides) throws Exception {
        Set<String> locations = new HashSet<String>();
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
        FileDownloader downloader = new FileDownloader(manager);
        downloader.download(locations);
        return downloader.await();
    }

    public static void addMavenProxies(Dictionary props, FabricService fabricService) {
        try {
            if (fabricService != null) {
                StringBuilder sb = new StringBuilder();
                for (URI uri : fabricService.getMavenRepoURIs()) {
                    String mavenRepo = uri.toString();
                    if (!mavenRepo.endsWith("/")) {
                        mavenRepo += "/";
                    }
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(mavenRepo);
                    sb.append("@snapshots");
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

    public interface DownloadCallback {
        public void downloaded(File file) throws Exception;
    }

    public static abstract class ArtifactDownloader<T> {

        protected final DownloadManager manager;
        protected final Object lock = new Object();
        protected final ConcurrentMap<String, DownloadFuture> futures = new ConcurrentHashMap<String, DownloadFuture>();
        protected final ConcurrentMap<String, T> artifacts = new ConcurrentHashMap<String, T>();
        protected final List<Throwable> errors = new CopyOnWriteArrayList<Throwable>();
        protected final AtomicInteger pendings = new AtomicInteger();

        public ArtifactDownloader(DownloadManager manager) {
            this.manager = manager;
        }

        public void download(String uri, final DownloadCallback callback) throws MalformedURLException {
            synchronized (lock) {
                DownloadFuture future = futures.get(uri);
                if (future == null) {
                    pendings.incrementAndGet();
                    future = manager.download(uri);
                    future.addListener(new FutureListener<DownloadFuture>() {
                        @Override
                        public void operationComplete(DownloadFuture future) {
                            onDownloaded(future, callback);
                        }
                    });
                    futures.put(uri, future);
                }
            }
        }

        public DownloadFuture download(String uri) throws MalformedURLException {
            synchronized (lock) {
                DownloadFuture future = futures.get(uri);
                if (future == null) {
                    pendings.incrementAndGet();
                    future = manager.download(uri);
                    future.addListener(new FutureListener<DownloadFuture>() {
                        @Override
                        public void operationComplete(DownloadFuture future) {
                            onDownloaded(future, null);
                        }
                    });
                    futures.put(uri, future);
                }
                return future;
            }
        }

        protected void onDownloaded(DownloadFuture future, DownloadCallback callback) {
            synchronized (lock) {
                try {
                    String url = future.getUrl();
                    File file = future.getFile();
                    T t = getArtifact(url, file);
                    artifacts.put(url, t);
                    if (callback != null) {
                        callback.downloaded(file);
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    pendings.decrementAndGet();
                    lock.notifyAll();
                }
            }
        }

        protected abstract T getArtifact(String uri, File file) throws Exception;

        public void download(Iterable<String> uris) throws MalformedURLException {
            for (String uri : uris) {
                download(uri);
            }
        }

        public Map<String, T> await() throws InterruptedException, MultiException {
            synchronized (lock) {
                while (pendings.get() > 0) {
                    lock.wait();
                }
                if (!errors.isEmpty()) {
                    throw new MultiException("Error while downloading artifacts", errors);
                }
                return artifacts;
            }
        }
    }

    public static class RepositoryDownloader extends ArtifactDownloader<Repository> {

        public RepositoryDownloader(DownloadManager manager) {
            super(manager);
        }

        @Override
        protected Repository getArtifact(String uri, File file) throws Exception {
            FeatureValidationUtil.validate(file.toURI());
            //We are using the file uri instead of the maven url, because we want to make sure, that the repo can load.
            //If we used the maven uri instead then we would have to make sure that the download location is added to
            //the ops4j pax url configuration. Using the first is a lot safer and less prone to misconfigurations.
            RepositoryImpl repo = new RepositoryImpl(file.toURI());
            repo.load();
            for (URI ref : repo.getRepositories()) {
                download(ref.toString());
            }
            return repo;
        }
    }

    public static class FileDownloader extends ArtifactDownloader<File> {
        public FileDownloader(DownloadManager manager) {
            super(manager);
        }

        @Override
        protected File getArtifact(String uri, File file) throws Exception {
            return file;
        }
    }

}
