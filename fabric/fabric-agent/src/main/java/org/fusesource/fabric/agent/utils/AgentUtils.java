package org.fusesource.fabric.agent.utils;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.FeatureValidationUtil;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.fusesource.fabric.agent.download.DownloadFuture;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.download.FutureListener;
import org.fusesource.fabric.api.FabricService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.net.URI;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class AgentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentUtils.class);

    public static final String FAB_PROTOCOL = "fab:";

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
            locations.add(override);
        }
        final CountDownLatch latch = new CountDownLatch(locations.size());
        final Map<String, File> downloads = new ConcurrentHashMap<String, File>();
        final List<Throwable> errors = new CopyOnWriteArrayList<Throwable>();
        for (final String location : locations) {
            final String strippedLocation = location.startsWith(FAB_PROTOCOL) ? location.substring(FAB_PROTOCOL.length()) : location;
            //The Fab URL Handler may not be present so we strip the fab protocol before downloading.
            manager.download(strippedLocation).addListener(new FutureListener<DownloadFuture>() {
                public void operationComplete(DownloadFuture future) {
                    try {
                        downloads.put(strippedLocation, future.getFile());
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        latch.await();
        if (!errors.isEmpty()) {
            throw new MultiException("Error while downloading bundles", errors);
        }
        return downloads;
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
}
