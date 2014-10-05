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
package io.fabric8.agent.commands.support;

import static io.fabric8.agent.commands.support.Utils.findProfileChecksums;
import static io.fabric8.agent.commands.support.Utils.getFileChecksum;
import static io.fabric8.agent.commands.support.Utils.isSnapshot;
import io.fabric8.agent.commands.ProfileWatcher;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Closeables;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.internal.Objects;
import io.fabric8.maven.util.MavenConfiguration;
import io.fabric8.maven.util.MavenConfigurationImpl;
import io.fabric8.maven.util.MavenRepositoryURL;
import io.fabric8.maven.util.Parser;
import io.fabric8.service.child.ChildContainers;
import io.fabric8.utils.Base64Encoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Runnable singleton which watches at the defined location for bundle updates.
 */
@Component(immediate = true, metatype = false)
@Service(ProfileWatcher.class)
public class ProfileWatcherImpl extends AbstractComponent implements ProfileWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileWatcherImpl.class);
    private static final String WATCHED_URL_FILE = "watched-urls.properties";

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configurationAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    private AtomicBoolean running = new AtomicBoolean(false);
    private long interval = 1000L;
    private List<String> watchURLs = new CopyOnWriteArrayList<String>();
    private AtomicInteger counter = new AtomicInteger(0);
    private Map<ProfileVersionKey, Map<String, Parser>> profileArtifacts = null;

    private Runnable fabricConfigureChangeRunnable = new Runnable() {
        @Override
        public void run() {
            LOG.debug("Fabric configuration changed so refreshing profile watcher");
            counter.incrementAndGet();
        }
    };
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean upload;
    private Set<String> missingChecksums = new HashSet<String>();

    public ProfileWatcherImpl() {
    }

    @Activate
    void activate(BundleContext bundleContext) {
        start();
        activateComponent();
        load(bundleContext);
    }

    @Deactivate
    void deactivate(BundleContext bundleContext) {
        save(bundleContext);
        stop();
        deactivateComponent();
    }

    void save(BundleContext bundleContext) {
        File file = bundleContext.getDataFile(WATCHED_URL_FILE);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            for (String url : watchURLs) {
                bw.write(url);
                bw.newLine();
            }
        } catch (Exception ex) {
            LOG.error("Error while saving watched URLs", ex);
        } finally {
            Closeables.closeQuitely(bw);
        }
    }

    void load(BundleContext bundleContext) {
        File file = bundleContext.getDataFile(WATCHED_URL_FILE);
        if (file.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    add(line);
                }
            } catch (Exception ex) {
                LOG.error("Error while loading watched URLs", ex);
            } finally {
                Closeables.closeQuitely(br);
            }
        }
    }

    public void run() {
        assertValid();
        LOG.debug("Profile watcher thread started");
        int oldCounter = -1;
        SortedSet<String> oldActiveProfiles = null;
        Map<File, Long> localChecksums = new HashMap<File, Long>();
        Map<File, Long> localModified = new HashMap<File, Long>();
        Set<Profile> refreshProfiles = new HashSet<Profile>();
        ProfileService profileService = fabricService.get().adapt(ProfileService.class);
        while (running.get() && watchURLs.size() > 0) {
            SortedSet<String> currentActiveProfiles = getCurrentActiveProfileVersions();
            if (profileArtifacts == null || oldCounter != counter.get() ||
                    oldActiveProfiles == null || !oldActiveProfiles.equals(currentActiveProfiles)) {
                oldCounter = counter.get();
                oldActiveProfiles = currentActiveProfiles;
                try {
                    LOG.debug("Reloading the currently active profile artifacts");
                    profileArtifacts = findProfileArifacts();
                } catch (Exception e) {
                    LOG.error("Failed to get profiles artifacts: " + e, e);
                }
            }

            // lets refresh profiles on the next loop; so we've time to finish uploading/modifying files
            for (Profile profile : refreshProfiles) {
                LOG.info("Refreshing profile: " + profile);
                Profiles.refreshProfile(fabricService.get(), profile);
            }
            refreshProfiles.clear();

            if (profileArtifacts != null) {
                File localRepository = getLocalRepository();

                Set<Map.Entry<ProfileVersionKey, Map<String, Parser>>> entries = profileArtifacts.entrySet();
                for (Map.Entry<ProfileVersionKey, Map<String, Parser>> entry : entries) {
                    ProfileVersionKey key = entry.getKey();
                    Map<String, Parser> artifactMap = entry.getValue();

                    // lets find a container for the profile
                    Profile profile = key.getProfile();
                    Properties checksums = findProfileChecksums(fabricService.get(), profile);
                    if (checksums != null) {
                        Set<Map.Entry<String, Parser>> artifactMapEntries = artifactMap.entrySet();
                        for (Map.Entry<String, Parser> artifactMapEntry : artifactMapEntries) {
                            String location = artifactMapEntry.getKey();
                            Parser parser = artifactMapEntry.getValue();
                            if (isSnapshot(parser) || wildCardMatch(location)) {
                                Object value = checksums.get(location);
                                if (value == null) {
                                    value = checksums.get(JavaContainers.removeUriPrefixBeforeMaven(location));
                                }
                                Long checksum = null;
                                if (value instanceof Number) {
                                    checksum = ((Number) value).longValue();
                                } else  if (value instanceof String) {
                                    checksum = Long.parseLong((String) value);
                                }
                                if (checksum == null) {
                                    if (missingChecksums.add(location)) {
                                        LOG.warn("Could not find checksum for location " + location);
                                    }
                                } else {
                                    File file = new File(localRepository.getPath() + File.separator + parser.getArtifactPath());
                                    if (!file.exists()) {
                                        LOG.info("Ignoring file " + file.getPath() + " as it does not exist");
                                    } else {
                                        // lets use a cache of last modified times to avoid having to continuously
                                        // recalculate the checksum on each file
                                        Long oldModfied = localModified.get(file);
                                        long modified = file.lastModified();
                                        if (oldModfied == null || modified != oldModfied) {
                                            localModified.put(file, modified);
                                            Long fileChecksum = getFileChecksum(file);
                                            if (fileChecksum != null && !fileChecksum.equals(checksum)) {
                                                // lets keep track of local checksums in case we've already started the upload process
                                                // and it takes the profile a little while to respond to uploaded jars and to
                                                // refreshed profiles
                                                Long localChecksum = localChecksums.get(file);
                                                if (localChecksum == null || !localChecksum.equals(fileChecksum)) {
                                                    localChecksums.put(file, fileChecksum);
                                                    LOG.info("Checksums don't match for " + location + ", container: " + checksum + " and local file: " + fileChecksum);
                                                    if (isUpload()) {
                                                        uploadFile(location, parser, file);
                                                    }
                                                    refreshProfiles.add(profile);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LOG.info("Ignoring " + location);
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ex) {
                running.set(false);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Profile watcher thread stopped");
        }
    }

    /**
     * Returns the currently known profile artifacts
     */
    public Map<ProfileVersionKey, Map<String, Parser>> getProfileArtifacts() {
        if (profileArtifacts == null) {
            return Collections.EMPTY_MAP;
        }
        return profileArtifacts;
    }

    /**
     * Uploads the given file to the fabric maven proxy
     */
    protected void uploadFile(String bundleUrl, Parser parser, File fileToUpload) {
        String user = fabricService.get().getZooKeeperUser();
        String password = fabricService.get().getZookeeperPassword();
        URI uploadUri = fabricService.get().getMavenRepoUploadURI();
        URI artifactUri = uploadUri.resolve(parser.getArtifactPath());
        URL url;
        try {
            url = artifactUri.toURL();
        } catch (MalformedURLException e) {
            LOG.warn("Failed to parse URI " + artifactUri + ". " + e, e);
            return;
        }
        if (fileToUpload == null) {
            return;
        }
        if (!fileToUpload.exists() || !fileToUpload.isFile()) {
            LOG.warn("Artifact file does not exist! " + fileToUpload.getAbsolutePath());
            return;
        }

        LOG.info("Uploading " + fileToUpload.getPath() + " to fabric8 maven repo: " + url + " as user: " + user);
        try {
            FileChannel in = new FileInputStream(fileToUpload).getChannel();
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + Base64Encoder.encode(user + ":" + password));
            WritableByteChannel out = Channels.newChannel(connection.getOutputStream());
            in.transferTo(0, fileToUpload.length(), out);

            if (connection instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) connection).getResponseCode();
                if (code < 200 || code >= 300) {
                    LOG.warn(String.format("Got response code %s while uploading %s to fabric8 maven repo: %s", code, fileToUpload.getPath(), url));
                    return;
                }
            }

            LOG.info("Uploaded  " + fileToUpload.getPath() + " to fabric8 maven repo: " + url);
        } catch (Exception e) {
            LOG.error("Failed to upload " + fileToUpload.getPath() + " to fabric8 maven repo: " + url + ". " + e, e);
        }
    }

    /**
     * Adds a Bundle URLs to the watch list.
     *
     * @param url
     */
    public void add(String url) {
        assertValid();
        boolean shouldStart = running.get() && (watchURLs.size() == 0);
        if (!watchURLs.contains(url)) {
            watchURLs.add(url);
            counter.incrementAndGet();
        }
        if (shouldStart) {
            Thread thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Removes a bundle URLs from the watch list.
     *
     * @param url
     */
    public void remove(String url) {
        assertValid();
        watchURLs.remove(url);
        counter.incrementAndGet();
    }

    /**
     * Gets the set of active profile ids and versions
     */
    protected SortedSet<String> getCurrentActiveProfileVersions() {
        SortedSet<String> answer = new TreeSet<String>();
        Container[] containers = fabricService.get().getContainers();
        for (Container container : containers) {
            container.getProvisionList();
            Profile[] profiles = container.getProfiles();
            // TODO allow filter on a profile here?
            for (Profile profile : profiles) {
                String id = profile.getId();
                String version = profile.getVersion();
                answer.add(id + "/" + version);
            }
        }
        return answer;
    }

    // For each profile and version return the map of bundle locations to parsers
    private Map<ProfileVersionKey, Map<String, Parser>> findProfileArifacts() throws Exception {
        Map<ProfileVersionKey, Map<String, Parser>> profileArtifacts = new HashMap<ProfileVersionKey, Map<String, Parser>>();
        ProfileService profileService = fabricService.get().adapt(ProfileService.class);
        DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabricService.get(), executorService);
        Container[] containers = fabricService.get().getContainers();
        for (Container container : containers) {
            Profile[] profiles = container.getProfiles();
            boolean javaOrProcessContainer = ChildContainers.isJavaOrProcessContainer(fabricService.get(), container);
                // TODO allow filter on a profile here?
                for (Profile profile : profiles) {
                    Profile overlay = profileService.getOverlayProfile(profile);
                    ProfileVersionKey key = new ProfileVersionKey(profile);
                    //if (!profileArtifacts.containsKey(key)) {
                        Map<String, Parser> artifacts = null;
                        if (javaOrProcessContainer) {
                            List<Profile> singletonList = Collections.singletonList(profile);
                            artifacts = JavaContainers.getJavaContainerArtifacts(fabricService.get(), singletonList, executorService);
                        } else {
                            artifacts = AgentUtils.getProfileArtifacts(fabricService.get(), downloadManager, overlay);
                        }
                        if (artifacts != null) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Profile " + profile.getId() + " maps to artefacts: " + artifacts.keySet());
                            }
                            profileArtifacts.put(key, artifacts);
                        }
                    //}
                }
        }
        return profileArtifacts;
    }

    public File getLocalRepository() {
        assertValid();
        // Attempt to retrieve local repository location from MavenConfiguration
        MavenConfiguration configuration = retrieveMavenConfiguration();
        if (configuration != null) {
            MavenRepositoryURL localRepositoryURL = configuration.getLocalRepository();
            if (localRepositoryURL != null) {
                return localRepositoryURL.getFile().getAbsoluteFile();
            }
        }
        // If local repository not found assume default.
        String localRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        return new File(localRepo).getAbsoluteFile();
    }

    protected MavenConfiguration retrieveMavenConfiguration() {
        MavenConfiguration mavenConfiguration = null;
        try {
            Configuration configuration = configurationAdmin.get().getConfiguration("org.ops4j.pax.url.mvn");
            if (configuration != null) {
                Dictionary dictonary = configuration.getProperties();
                if (dictonary != null) {
                    DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(dictonary);
                    mavenConfiguration = new MavenConfigurationImpl(resolver, "org.ops4j.pax.url.mvn");
                }
            }
        } catch (IOException e) {
            LOG.error("Error retrieving maven configuration", e);
        }
        return mavenConfiguration;
    }


    public boolean wildCardMatch(String text) {
         for (String watchURL : watchURLs) {
             if (wildCardMatch(text, watchURL)) {
                 return true;
             }
         }
         return false;
     }

    /**
     * Matches text using a pattern containing wildcards.
     *
     * @param text
     * @param pattern
     * @return
     */
    public boolean wildCardMatch(String text, String pattern) {
        assertValid();
        String[] cards = pattern.split("\\*");
        // Iterate over the cards.
        for (String card : cards) {
            int idx = text.indexOf(card);
            // Card not detected in the text.
            if (idx == -1) {
                return false;
            }

            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }
        return true;
    }

    public void start() {
        missingChecksums.clear();
        Objects.notNull(fabricService, "fabricService");
        fabricService.get().trackConfiguration(fabricConfigureChangeRunnable);

        // start the watch thread
        if (running.compareAndSet(false, true)) {
            if (watchURLs.size() > 0) {
                Thread thread = new Thread(this);
                thread.start();
            }
        }
    }

    /**
     * Stops the execution of the thread and releases the singleton instance
     */
    public void stop() {
        missingChecksums.clear();
        if (fabricService != null) {
            fabricService.get().untrackConfiguration(fabricConfigureChangeRunnable);
        }
        running.set(false);
    }


    public List<String> getWatchURLs() {
        return watchURLs;
    }

    public void setWatchURLs(List<String> watchURLs) {
        this.watchURLs = watchURLs;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isUpload() {
        return upload;
    }

    public void setUpload(boolean upload) {
        this.upload = upload;
    }

    void bindConfigurationAdmin(ConfigurationAdmin service) {
        this.configurationAdmin.bind(service);
    }

    void unbindConfigurationAdmin(ConfigurationAdmin service) {
        this.configurationAdmin.unbind(service);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
