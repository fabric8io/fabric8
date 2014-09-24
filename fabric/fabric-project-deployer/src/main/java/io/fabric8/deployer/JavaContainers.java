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
package io.fabric8.deployer;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.common.util.MultiException;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;

import io.fabric8.maven.util.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

/**
 */
public class JavaContainers {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(JavaContainers.class);
    
    public static Map<String, Parser> getJavaContainerArtifacts(FabricService fabric, List<Profile> profileList, ExecutorService downloadExecutor) throws Exception {
        DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabric, downloadExecutor);
        return getJavaContainerArtifacts(fabric, profileList, downloadManager);
    }

    public static Map<String, Parser> getJavaContainerArtifacts(FabricService fabric, List<Profile> profileList, DownloadManager downloadManager) throws Exception {
        Map<String, Parser> artifacts = new TreeMap<String, Parser>();
        for (Profile profile : profileList) {
            Map<String, Parser> profileArtifacts = AgentUtils.getProfileArtifacts(fabric, downloadManager, profile);
            artifacts.putAll(profileArtifacts);
            appendMavenDependencies(artifacts, profile);
        }
        return artifacts;
    }

    public static Map<String, File> getJavaContainerArtifactsFiles(FabricService fabricService, List<Profile> profileList, ExecutorService downloadExecutor) throws Exception {
        DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabricService, downloadExecutor);
        return getJavaContainerArtifactsFiles(fabricService, profileList, downloadManager);
    }

    public static Map<String, File> getJavaContainerArtifactsFiles(FabricService fabricService, List<Profile> profileList, DownloadManager downloadManager) throws Exception {
        Map<String, File> answer = new HashMap<String, File>();
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        for (Profile profile : profileList) {
            Profile overlay = profileService.getOverlayProfile(profile);
            Map<String, Parser> profileArtifacts = AgentUtils.getProfileArtifacts(fabricService, downloadManager, overlay);
            appendMavenDependencies(profileArtifacts, profile);
            Set<String> rawUrls = profileArtifacts.keySet();
            downloadArtifactUrls(downloadManager, rawUrls, answer);
        }
        return answer;
    }

    public static void downloadArtifactUrls(DownloadManager downloadManager, Set<String> rawUrls, Map<String, File> answer) throws MalformedURLException, InterruptedException, MultiException {
        List<String> cleanUrlsToDownload = new ArrayList<String>();
        for (String rawUrl : rawUrls) {
            String mvnUrl = removeUriPrefixBeforeMaven(rawUrl);
            cleanUrlsToDownload.add(mvnUrl);
        }
        Map<String, File> profileFiles = AgentUtils.downloadLocations(downloadManager, cleanUrlsToDownload);
        if (profileFiles != null) {
            answer.putAll(profileFiles);
        }
    }

    /**
     * Any URI which has a prefix before the "mvn:" part of the URI, such as "fab:mvn:..." or "war:mvn:..." gets the prefix removed so
     * that the URI is just "mvn:..."
     *
     * @return the URI with any prefix before ":mvn:" removed so that the string starts with "mvn:"
     */
    public static String removeUriPrefixBeforeMaven(String rawUrl) {
        String answer = rawUrl;
        // remove any prefix before :mvn:
        int idx = answer.indexOf(":mvn:");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        return answer;
    }

    protected static void appendMavenDependencies(Map<String, Parser> artifacts, Profile profile) {
        Set<String> configurationFileNames = profile.getConfigurationFileNames();
        for (String configurationFileName : configurationFileNames) {
            if (configurationFileName.startsWith("dependencies/") && configurationFileName.endsWith("-requirements.json")) {
                byte[] data = profile.getFileConfiguration(configurationFileName);
                try {
                    ProjectRequirements requirements = DtoHelper.getMapper().readValue(data, ProjectRequirements.class);
                    if (requirements != null) {
                        DependencyDTO rootDependency = requirements.getRootDependency();
                        if (rootDependency != null) {
                            addMavenDependencies(artifacts, rootDependency);
                        }
                    }

                } catch (IOException e) {
                    LOGGER.error("Failed to parse project requirements from " + configurationFileName + ". " + e, e);
                }
            }
        }
    }

    protected static void addMavenDependencies(Map<String, Parser> artifacts, DependencyDTO dependency) throws MalformedURLException {
        String url = dependency.toBundleUrlWithType();
        Parser parser = Parser.parsePathWithSchemePrefix(url);
        String scope = dependency.getScope();
        if (!artifacts.containsKey(url) && !artifacts.containsValue(parser) && !(Objects.equal("test", scope))) {
            LOGGER.debug("Adding url: " + url + " parser: " + parser);
            artifacts.put(url, parser);
        }
        List<DependencyDTO> children = dependency.getChildren();
        if (children != null) {
            for (DependencyDTO child : children) {
                addMavenDependencies(artifacts, child);
            }
        }
    }

    /**
     * Registers the given jolokia URL for the given container if its not null
     *
     * @param container the container to register the jolokia URL for
     * @param jolokiaUrl the Jolokia URL
     */
    public static void registerJolokiaUrl(Container container, String jolokiaUrl) {
        if (Strings.isNotBlank(jolokiaUrl)) {
            String currentUrl = container.getJolokiaUrl();
            if (!Objects.equal(jolokiaUrl, currentUrl)) {
                container.setJolokiaUrl(jolokiaUrl);
            }
        }
    }
}
