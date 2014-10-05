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
package io.fabric8.openshift.agent;

import io.fabric8.api.FabricService;
import io.fabric8.common.util.Files;

import io.fabric8.maven.util.MavenRepositoryURL;
import io.fabric8.maven.util.Parser;
import org.apache.karaf.features.Feature;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;

import io.fabric8.common.util.LoggingOutputStream;
import io.fabric8.common.util.Strings;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.git.internal.GitHelpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Updates the deployment in a Fabric managed OpenShift cartridge
 * by either copying new deployment artifacts into directories in git directly or
 * by updating the pom.xml in the maven build of the cartridges git repository to
 * download the required deployment artifacts as part of the build (based on the flag
 * {@link #isCopyFilesIntoGit()}.
 * <p/>
 * For some common containers like Tomcat we also auto-detect special files like {@link #OPENSHIFT_CONFIG_CATALINA_PROPERTIES}
 * so that we can enable the use of a shared folder for deploying jars on a shared classpath across deployment units.
 * <p/>
 * This allows, for example, shared features to be used across deployment units; such as, say, Apache Camel jars to be installed
 * and shared across all web applications in the container.
 */
public class DeploymentUpdater {
    private static final transient Logger LOG = LoggerFactory.getLogger(DeploymentUpdater.class);
    public static final String OPENSHIFT_CONFIG_CATALINA_PROPERTIES = ".openshift/config/catalina.properties";

    private final DownloadManager downloadManager;
    private final FabricService fabricService;
    private final Container container;
    private final String webAppDir;
    private final String deployDir;
    private boolean copyFilesIntoGit = false;
    private String repositories;

    public DeploymentUpdater(DownloadManager downloadManager, FabricService fabricService, Container container, String webAppDir,
                             String deployDir) {
        this.downloadManager = downloadManager;
        this.fabricService = fabricService;
        this.container = container;
        this.webAppDir = webAppDir;
        this.deployDir = deployDir;
    }

    public void updateDeployment(Git git, File baseDir, CredentialsProvider credentials) throws Exception {
        Set<String> bundles = new LinkedHashSet<String>();
        Set<Feature> features = new LinkedHashSet<Feature>();
        Profile overlayProfile = container.getOverlayProfile();
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService, overlayProfile);
        bundles.addAll(effectiveProfile.getBundles());
        AgentUtils.addFeatures(features, fabricService, downloadManager, effectiveProfile);

        if (copyFilesIntoGit) {
            copyDeploymentsIntoGit(git, baseDir, bundles, features);
        } else {
            addDeploymentsIntoPom(git, baseDir, effectiveProfile, bundles, features);
        }

        // now lets do a commit
        String message = "updating deployment";
        git.commit().setMessage(message).call();

        enableDeployDirectory(git, baseDir);

        String branch = GitHelpers.currentBranch(git);
        LOG.info("Pushing deployment changes to branch " + branch
                + " credentials " + credentials + " for container " + container.getId());
        try {
            Iterable<PushResult> results = git.push().setCredentialsProvider(credentials).setRefSpecs(new RefSpec(branch))
                    .setOutputStream(new LoggingOutputStream(LOG)).call();
/*
            for (PushResult result : results) {
                LOG.info(result.getMessages());
            }
*/
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
    protected void addDeploymentsIntoPom(Git git, File baseDir, Profile profile, Set<String> bundles, Set<Feature> features) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException, TransformerException, GitAPIException {
        Collection<Parser> artifacts = AgentUtils.getProfileArtifacts(fabricService, profile, bundles, features).values();

        if (artifacts.size() > 0) {
            OpenShiftPomDeployer pomDeployer = new OpenShiftPomDeployer(git, baseDir, deployDir, webAppDir);

            List<MavenRepositoryURL> repositories = parseMavenRepositoryURLs();
            pomDeployer.update(artifacts, repositories);
        }
    }

    protected List<MavenRepositoryURL> parseMavenRepositoryURLs() throws MalformedURLException {
        List<MavenRepositoryURL> repositories = new ArrayList<MavenRepositoryURL>();
        String text = getRepositories();
        if (Strings.isNotBlank(text)) {
            StringTokenizer iter = new StringTokenizer(text);
            while (iter.hasMoreTokens()) {
                String url = iter.nextToken();
                if (url.endsWith(",")) {
                    url = url.substring(0, url.length() - 1);
                }
                MavenRepositoryURL mavenUrl = new MavenRepositoryURL(url);
                repositories.add(mavenUrl);
            }
        }
        return repositories;
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

    public String getRepositories() {
        return repositories;
    }

    public void setRepositories(String repositories) {
        this.repositories = repositories;
    }
}
