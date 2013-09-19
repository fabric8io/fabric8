package org.fusesource.fabric.openshift.agent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.fusesource.common.util.Strings;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.agent.utils.AgentUtils;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.git.internal.GitHelpers;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.utils.features.FeatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentTask {
    private static final transient Logger LOG = LoggerFactory.getLogger(DeploymentTask.class);
    public static final String OPENSHIFT_CONFIG_CATALINA_PROPERTIES = ".openshift/config/catalina.properties";

    private final DownloadManager downloadManager;
    private final Container container;
    private final String webAppDir;
    private final String deployDir;
    private boolean copyFilesIntoGit = false;


    public DeploymentTask(DownloadManager downloadManager, Container container, String webAppDir,
                          String deployDir) {
        this.downloadManager = downloadManager;
        this.container = container;
        this.webAppDir = webAppDir;
        this.deployDir = deployDir;
    }

    public void updateDeployment(Git git, File baseDir, CredentialsProvider credentials) throws Exception {
        Set<String> bundles = new LinkedHashSet<String>();
        Set<Feature> features = new LinkedHashSet<Feature>();
        Profile profile = container.getOverlayProfile();
        bundles.addAll(profile.getBundles());
        addFeatures(features, profile);

        if (copyFilesIntoGit) {
            copyDeploymentsIntoGit(git, baseDir, bundles, features);
        } else {
            addDeploymentsIntoPom(git, baseDir, bundles, features);
        }

        // now lets do a commit
        String message = "updating deployment";
        git.commit().setMessage(message).call();

        enableDeployDirectory(git, baseDir);

        String branch = GitHelpers.currentBranch(git);
        LOG.info("Pushing deployment changes to branch " + branch
                + " credentials " + credentials + " for container " + container.getId());
        try {
            git.push().setCredentialsProvider(credentials).setRefSpecs(new RefSpec(branch)).setProgressMonitor(new LoggingProgressMonitor(LOG)).call();
            LOG.info("Pushed deployment changes to branch " + branch + " for container " + container.getId());
        } catch (GitAPIException e) {
            LOG.error("Failed to push deployment changes to branch " + branch + " for container " + container.getId() + ". Reason: " + e, e);
        }
    }

    /**
     * Lets download all the deployments and copy them into the {@link #webAppDir} or {@link #deployDir} in git
     */
    protected void copyDeploymentsIntoGit(Git git, File baseDir, Set<String> bundles, Set<Feature> features) throws Exception {
        List<String> webAppFilesToDelete = filesToDelete(baseDir, webAppDir);
        List<String> deployDirFilesToDelete = filesToDelete(baseDir, deployDir);

        LOG.debug("Deploying into container " + container.getId() + " features " + features + " and bundles "
                + bundles);
        Map<String, File> files = AgentUtils.downloadBundles(downloadManager, features, bundles,
                Collections.<String>emptySet());
        Set<Map.Entry<String, File>> entries = files.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            String name = entry.getKey();
            File file = entry.getValue();
            String destPath;
            String fileName = file.getName();
            if (name.startsWith("war:") || name.contains("/war/") || fileName.toLowerCase()
                    .endsWith(".war")) {
                destPath = webAppDir;
                webAppFilesToDelete.remove(fileName);
            } else {
                destPath = deployDir;
                deployDirFilesToDelete.remove(fileName);
            }

            if (destPath != null) {
                File destDir = new File(baseDir, destPath);
                destDir.mkdirs();
                File destFile = new File(destDir, fileName);
                LOG.info("Copying file " + fileName + " to :  " + destFile.getCanonicalPath()
                        + " for container " + container.getId());
                Files.copy(file, destFile);
                git.add().addFilepattern(destPath + "/" + fileName).call();
            }

            // now lets delete all the old remaining files from the directory
            deleteFiles(git, baseDir, webAppDir, webAppFilesToDelete);
            deleteFiles(git, baseDir, deployDir, deployDirFilesToDelete);
        }
    }


    /**
     * Copy the various deployments into the pom.xml so that after the push, OpenShift will
     * run the build and download the deployments into the {@link #webAppDir} or {@link #deployDir}
     */
    protected void addDeploymentsIntoPom(Git git, File baseDir, Set<String> bundles, Set<Feature> features) {
    }


    /**
     * Checks things like Tomcat to see if the deployDir needs to be added to the shared class loader
     */
    protected void enableDeployDirectory(Git git, File baseDir) throws GitAPIException {
        File catalinaProperties = new File(baseDir, OPENSHIFT_CONFIG_CATALINA_PROPERTIES);
        if (catalinaProperties.exists()) {
            // TODO make this configurable?
            String propertyName = "shared.loader";
            Properties properties = new Properties();
            String value = properties.getProperty(propertyName);
            if (Strings.isNotBlank(value) && (value.startsWith(deployDir + "/") || value.contains(":" + deployDir + "/"))) {
                LOG.info("Already has valid " + propertyName + " in " + catalinaProperties + " with value: " + value);
            } else {
                String newValue = deployDir + "/*.jar";
                if (Strings.isNotBlank(value)) {
                    newValue = newValue + ":" + value;
                }

                // now lets replace the line which starts with propertyName;
                LOG.info("Updating " + propertyName + " to " + newValue + " in " + catalinaProperties + " to enable the use of the shared deploy directory: " + deployDir);
                try {
                    int propertyNameLength = propertyName.length();
                    List<String> lines = Files.readLines(catalinaProperties);
                    for (int i = 0, size = lines.size(); i < size; i++) {
                        String line = lines.get(i);
                        if (line.startsWith(propertyName) && line.length() > propertyNameLength) {
                            char ch = line.charAt(propertyNameLength);
                            if (Character.isWhitespace(ch) || ch == '=') {
                                String newLine = propertyName + "=" + newValue;
                                lines.set(i, newLine);
                            }
                        }
                    }
                    Files.writeLines(catalinaProperties, lines);

                    git.add().addFilepattern(OPENSHIFT_CONFIG_CATALINA_PROPERTIES).call();
                    String message = "enabled the deploy directory '" + deployDir + "' to be on the shared class loader";
                    git.commit().setMessage(message).call();
                    LOG.info("Committed changes to: " + catalinaProperties);

                } catch (IOException e) {
                    LOG.warn("Failed to update " + catalinaProperties + " for container " + container.getId()
                            + ". " + e, e);
                }
            }
        }
    }

    /**
     * Returns a list of file names contained in the path from the given base directory or an empty
     * list if the path is null or the directory does not exist
     */
    protected List<String> filesToDelete(File baseDir, String path) {
        List<String> answer = new ArrayList<String>();
        if (path != null) {
            File dir = new File(baseDir, path);
            if (dir.exists() && dir.isDirectory()) {
                String[] list = dir.list();
                if (list != null) {
                    answer.addAll(Arrays.asList(list));
                }
            }
        }
        return answer;
    }

    protected void deleteFiles(Git git, File baseDir, String path, List<String> fileNames)
            throws GitAPIException {
        if (path != null) {
            for (String fileName : fileNames) {
                File file = new File(baseDir, path + "/" + fileName);
                if (file.exists()) {
                    LOG.debug("Removing " + file + " for container " + container.getId());
                    git.rm().addFilepattern(path + "/" + fileName).call();
                    file.delete();
                }
            }
        }
    }

    /**
     * Extracts the {@link java.net.URI}/{@link org.apache.karaf.features.Repository} map from the profile.
     *
     * @param p
     * @return
     * @throws java.net.URISyntaxException
     */
    public Map<URI, Repository> getRepositories(Profile p) throws Exception {
        Map<URI, Repository> repositories = new HashMap<URI, Repository>();
        for (String repositoryUrl : p.getRepositories()) {
            URI repoUri = new URI(repositoryUrl);
            AgentUtils.addRepository(downloadManager, repositories, repoUri);
        }
        return repositories;
    }

    protected void addFeatures(Set<Feature> features, Profile profile) throws Exception {
        List<String> featureNames = profile.getFeatures();
        Map<URI, Repository> repositories = getRepositories(profile);
        for (String featureName : featureNames) {
            features.add(FeatureUtils.search(featureName, repositories.values()));
        }
    }

    public String getWebAppDir() {
        return webAppDir;
    }

    public String getDeployDir() {
        return deployDir;
    }

    public boolean isCopyFilesIntoGit() {
        return copyFilesIntoGit;
    }

    public void setCopyFilesIntoGit(boolean copyFilesIntoGit) {
        this.copyFilesIntoGit = copyFilesIntoGit;
    }
}
