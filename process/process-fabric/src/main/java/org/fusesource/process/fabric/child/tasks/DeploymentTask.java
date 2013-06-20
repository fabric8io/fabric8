package org.fusesource.process.fabric.child.tasks;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.mvn.DictionaryPropertyResolver;
import org.fusesource.fabric.agent.mvn.MavenConfiguration;
import org.fusesource.fabric.agent.mvn.MavenConfigurationImpl;
import org.fusesource.fabric.agent.mvn.MavenSettingsImpl;
import org.fusesource.fabric.agent.mvn.PropertiesPropertyResolver;
import org.fusesource.fabric.agent.utils.AgentUtils;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.utils.features.FeatureUtils;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.config.ProcessConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;

public class DeploymentTask implements InstallTask {

    private static String PAX_URL_MVN_PID = "org.ops4j.pax.url.mvn";

    private final DownloadManager downloadManager;
    private final Profile profile;


    public DeploymentTask(DownloadManager downloadManager, Profile profile) {
        this.downloadManager = downloadManager;
        this.profile = profile;
    }

    @Override

    public void install(ProcessConfig config, int id, File installDir) throws Exception {
        Set<String> bundles = new LinkedHashSet<String>(profile.getBundles());
        Set<Feature> features = getFeatures(profile);
        Map<String, File> files = AgentUtils.downloadBundles(downloadManager, features, bundles, Collections.<String>emptySet());
    }

    /**
     * Extracts the {@link URI}/{@link Repository} map from the profile.
     *
     * @param p
     * @return
     * @throws URISyntaxException
     */
    public Map<URI, Repository> getRepositories(Profile p) throws Exception {
        Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        for (String repositoryUrl : p.getRepositories()) {
            URI repoUri = new URI(repositoryUrl);
            AgentUtils.addRepository(downloadManager, repositories, repoUri);
        }
        return repositories;
    }

    public Set<Feature> getFeatures(Profile p) throws Exception {
        Set<Feature> features = new LinkedHashSet<Feature>();
        List<String> featureNames = p.getFeatures();
        Map<URI, Repository> repositories = getRepositories(p);
        for (String featureName : featureNames) {
            features.add(FeatureUtils.search(featureName, repositories.values()));
        }
        return features;
    }
}
