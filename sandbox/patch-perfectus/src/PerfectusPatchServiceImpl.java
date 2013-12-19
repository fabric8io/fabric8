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
package org.fusesource.fabric.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.utils.version.VersionTable;
import org.fusesource.fabric.api.Constants;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Issue;
import org.fusesource.fabric.api.PatchService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.RuntimeProperties;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.utils.Base64Encoder;
import org.fusesource.fabric.utils.SystemProperties;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PerfectusPatchServiceImpl implements PerfectusPatchService {

    public static final String PATCH_REPOSITORIES = "patch.repositories";
    public static final String PATCH_INCLUDE_NON_FUSE_VERSION = "patch.include-non-fuse-versions";

    private static final String PATCH_ID = "id";
    private static final String PATCH_DESCRIPTION = "description";
    private static final String PATCH_BUNDLES = "bundle";
    private static final String PATCH_COUNT = "count";
    private static final String PATCH_RANGE = "range";

    public static final String ISSUE = "issue";
    public static final String ISSUE_KEY = "key";
    public static final String ISSUE_MODULE = "module";
    public static final String ISSUE_DESCRIPTION = "description";

    public static final String CACHE_FILE = "patch-cache.properties";
    public static final String CACHE_LAST_DATE = "lastDate";
    public static final String CACHE_LOCATION = "location";
    public static final String CACHE_COUNT = "count";

    /**
     * List of groupId:artifactId containing perfectus patches
     */
    public static final String DEFAULT_GROUPS = "org.apache.felix:org.apache.felix.framework," + "org.apache.felix:org.apache.felix.configadmin,"
            + "org.apache.felix:org.apache.felix.eventadmin," + "org.apache.felix:org.apache.felix.fileinstall," + "org.apache.felix:org.apache.felix.webconsole,"
            + "org.apache.aries.blueprint:blueprint," + "org.apache.aries.jmx:jmx," + "org.apache.aries:org.apache.aries.util,"
            + "org.apache.aries.transaction:transaction," + "org.apache.servicemix.specs:specs," + "org.apache.karaf:karaf," + "org.apache.cxf:cxf,"
            + "org.apache.camel:camel," + "org.apache.activemq:activemq-parent," + "org.apache.servicemix:servicemix-utils," + "org.apache.servicemix:components,"
            + "org.apache.servicemix.nmr:nmr-parent," + "org.apache.servicemix:features," + "org.apache.servicemix:archetypes," + "org.fusesource:fuse-project";

    /**
     * Old perfectus patches are missing the features descriptors
     * so we add them manually
     */
    public static final String MISSING_FEATURES_DESCRIPTOR = "org.apache.activemq:activemq-parent|" + "org.apache.activemq:activemq-karaf," + "org.apache.camel:camel|"
            + "org.apache.camel.karaf:apache-camel," + "org.apache.cxf:cxf|" + "org.apache.cxf.karaf:apache-cxf," + "org.apache.karaf:karaf|"
            + "org.apache.karaf.assemblies.features:enterprise|" + "org.apache.karaf.assemblies.features:spring|" + "org.apache.karaf.assemblies.features:standard,"
            + "org.apache.servicemix.nmr:nmr-parent|" + "org.apache.servicemix.nmr:apache-servicemix-nmr," + "org.fusesource:fuse-project|"
            + "org.jboss.fuse:jboss-fuse|" + "org.fusesource.examples:fabric-activemq-demo|" + "org.fusesource.examples:fabric-camel-cluster|"
            + "org.fusesource.examples:fabric-camel-demo|" + "org.fusesource.examples:fabric-camel-dosgi|" + "org.fusesource.examples:fabric-cxf-demo-features|"
            + "org.fusesource.fabric:fuse-fabric";

    private static final Logger LOGGER = LoggerFactory.getLogger(PerfectusPatchServiceImpl.class);

    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    private final File patchDir;
    private final FabricService fabric;
    private final ConfigurationAdmin configAdmin;
    private final RuntimeProperties runtimeProperties;
    private final ExecutorService executor = Executors.newFixedThreadPool(50);

    public PerfectusPatchServiceImpl(RuntimeProperties runtimeProperties, FabricService fabric, ConfigurationAdmin configAdmin, BundleContext bundleContext) {
        this.runtimeProperties = runtimeProperties;
        String patchLocation = runtimeProperties.getProperty("fuse.patch.location");
        patchDir = patchLocation != null ? new File(patchLocation) : bundleContext.getDataFile("patches");
        if (patchDir == null) {
            throw new IllegalArgumentException("Unable to retrieve patch directory");
        }
        createDir(patchDir);
        this.fabric = fabric;
        this.configAdmin = configAdmin;
    }

    public String getMavenArtifact(String url) {
        String mvnUrl = url;
        if (mvnUrl.startsWith("wrap:")) {
            mvnUrl = mvnUrl.substring("wrap:".length());
            if (mvnUrl.contains("$")) {
                mvnUrl = mvnUrl.substring(0, mvnUrl.lastIndexOf('$'));
            }
        }
        if (mvnUrl.startsWith("war:")) {
            mvnUrl = mvnUrl.substring("war:".length());
            if (mvnUrl.contains("?")) {
                mvnUrl = mvnUrl.substring(0, mvnUrl.lastIndexOf('?'));
            }
        }
        if (mvnUrl.startsWith("blueprint:") || mvnUrl.startsWith("spring:")) {
            mvnUrl = mvnUrl.substring(mvnUrl.indexOf(':') + 1);
        }
        if (mvnUrl.startsWith("mvn:")) {
            mvnUrl = mvnUrl.substring(4);
            String[] mvn = mvnUrl.split("/");
            return mvn[0] + ":" + mvn[1] + ":" + mvn[2];
        } else {
            mvnUrl = null;
        }
        return mvnUrl;
    }

    public Map<String, Set<String>> getPossibleUpgrades() {
        Set<String> artifacts = new TreeSet<String>();
        for (Version version : fabric.getVersions()) {
            doCollectArtifacts(version, artifacts);
        }
        return doGetPossibleUpgrades(artifacts);
    }

    @Override
    public Map<String, Set<String>> getPossibleUpgrades(Version version) {
        Set<String> artifacts = new TreeSet<String>();
        doCollectArtifacts(version, artifacts);
        return doGetPossibleUpgrades(artifacts);
    }

    @Override
    public Map<String, Set<String>> getPossibleUpgrades(Profile profile) {
        Set<String> artifacts = new TreeSet<String>();
        doCollectArtifacts(profile, artifacts);
        return doGetPossibleUpgrades(artifacts);
    }

    @Override
    public void applyUpgrades(Map<String, String> upgrades) {
        for (Version version : fabric.getVersions()) {
            applyUpgrades(version, upgrades);
        }
    }

    @Override
    public void applyUpgrades(Version version, Map<String, String> upgrades) {
        for (Profile profile : version.getProfiles()) {
            applyUpgrades(profile, upgrades);
        }
    }

    @Override
    public void applyUpgrades(Profile profile, Map<String, String> upgrades) {
        List<String> bundles = profile.getBundles();
        List<String> newBundles = doApplyUpgrade(bundles, upgrades);
        if (!newBundles.equals(bundles)) {
            profile.setBundles(newBundles);
        }
        List<String> fabs = profile.getFabs();
        List<String> newFabs = doApplyUpgrade(fabs, upgrades);
        if (!newFabs.equals(fabs)) {
            profile.setFabs(newFabs);
        }
        List<String> repositories = profile.getRepositories();
        List<String> newRepositories = doApplyUpgrade(repositories, upgrades);
        if (!newRepositories.equals(repositories)) {
            profile.setRepositories(newRepositories);
        }
    }

    private void doCollectArtifacts(Version version, Set<String> artifacts) {
        for (Profile profile : version.getProfiles()) {
            doCollectArtifacts(profile, artifacts);
        }
    }

    private void doCollectArtifacts(Profile profile, Set<String> artifacts) {
        artifacts.addAll(profile.getBundles());
        artifacts.addAll(profile.getFabs());
        artifacts.addAll(profile.getRepositories());
    }

    private Map<String, Set<String>> doGetPossibleUpgrades(Set<String> artifacts) {
        Dictionary config = getConfig();
        final List<String> repositories = getRepositories(config);
        final boolean includeNonFuseVersions = Boolean.parseBoolean((String) config.get(PATCH_INCLUDE_NON_FUSE_VERSION));
        Set<String> mavenArtifacts = new TreeSet<String>();
        for (String artifact : artifacts) {
            String mvn = getMavenArtifact(artifact);
            if (mvn != null) {
                mavenArtifacts.add(mvn);
            }
        }
        ExecutorService executor = Executors.newFixedThreadPool(20);
        final Map<String, Set<String>> artifactsVersions = new HashMap<String, Set<String>>();
        for (final String mvn : mavenArtifacts) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    Set<String> versions = doGetPossibleUpgrades(repositories, mvn, includeNonFuseVersions);
                    if (versions != null && !versions.isEmpty()) {
                        synchronized (artifactsVersions) {
                            artifactsVersions.put(mvn, versions);
                        }
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return artifactsVersions;
    }

    private Set<String> doGetPossibleUpgrades(List<String> repositories, String mvn, boolean includeNonFuseVersions) {
        Set<String> allVersions = new TreeSet<String>(new FuseVersionComparator());
        String[] mvnParts = mvn.split(":");
        org.osgi.framework.Version artifactVersion = VersionTable.getVersion(mvnParts[2]);
        for (String repo : repositories) {
            try {
                URL base = new URL(repo + "/" + mvnParts[0].replace('.', '/') + "/" + mvnParts[1] + "/");
                URL metadata = new URL(base, "maven-metadata.xml");
                URLConnection con = metadata.openConnection();
                if (metadata.getUserInfo() != null) {
                    con.setRequestProperty("Authorization", "Basic " + Base64Encoder.encode(metadata.getUserInfo()));
                }
                InputStream is = con.getInputStream();
                try {
                    Document doc = dbf.newDocumentBuilder().parse(is);
                    NodeList versions = doc.getDocumentElement().getElementsByTagName("version");
                    for (int i = 0; i < versions.getLength(); i++) {
                        Node version = versions.item(i);
                        String v = version.getTextContent();
                        org.osgi.framework.Version ver = VersionTable.getVersion(v);
                        if (isInMajorRange(artifactVersion, ver)) {
                            if (includeNonFuseVersions || v.contains("fuse")) {
                                allVersions.add(v.trim());
                            }
                        }
                    }
                } finally {
                    is.close();
                }
            } catch (Exception e) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Unable to retrieve versions for artifact: " + mvn, e);
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unable to retrieve versions for artifact: " + mvn + ": " + e.getMessage());
                }
            }
        }
        return allVersions;
    }

    private List<String> doApplyUpgrade(List<String> artifacts, Map<String, String> upgrades) {
        List<String> newArtifacts = new ArrayList<String>();
        for (String artifact : artifacts) {
            newArtifacts.add(doApplyUpgrade(artifact, upgrades));
        }
        return newArtifacts;
    }

    private String doApplyUpgrade(String artifact, Map<String, String> upgrades) {
        String mvn = getMavenArtifact(artifact);
        if (mvn != null && upgrades.containsKey(mvn)) {
            String[] mvnParts = mvn.split(":");
            String oldUrl = mvnParts[0] + "/" + mvnParts[1] + "/" + mvnParts[2];
            String newUrl = mvnParts[0] + "/" + mvnParts[1] + "/" + upgrades.get(mvn);
            artifact = artifact.replaceAll(oldUrl, newUrl);
        }
        return artifact;
    }

    public Set<Patch> getPossiblePatches() {
        Set<String> artifacts = new TreeSet<String>();
        for (Version version : fabric.getVersions()) {
            doCollectArtifacts(version, artifacts);
        }
        return doGetPossiblePatches(artifacts);
    }

    @Override
    public Set<Patch> getPossiblePatches(Version version) {
        Set<String> artifacts = new TreeSet<String>();
        doCollectArtifacts(version, artifacts);
        return doGetPossiblePatches(artifacts);
    }

    @Override
    public Set<Patch> getPossiblePatches(Profile profile) {
        Set<String> artifacts = new TreeSet<String>();
        doCollectArtifacts(profile, artifacts);
        return doGetPossiblePatches(artifacts);
    }

    @Override
    public void applyPatches(Set<Patch> patches) {
        for (Version version : fabric.getVersions()) {
            applyPatches(version, patches);
        }
    }

    @Override
    public void applyPatches(Version version, Set<Patch> patches) {
        for (Profile profile : version.getProfiles()) {
            applyPatches(profile, patches);
        }
    }

    @Override
    public void applyPatches(Profile profile, Set<Patch> patches) {
        List<String> bundles = profile.getBundles();
        List<String> newBundles = doApplyPatches(bundles, patches);
        if (!newBundles.equals(bundles)) {
            profile.setBundles(newBundles);
        }
        List<String> fabs = profile.getFabs();
        List<String> newFabs = doApplyPatches(fabs, patches);
        if (!newFabs.equals(fabs)) {
            profile.setFabs(newFabs);
        }
        List<String> repositories = profile.getRepositories();
        List<String> newRepositories = doApplyPatches(repositories, patches);
        if (!newRepositories.equals(repositories)) {
            profile.setRepositories(newRepositories);
        }
        List<String> features = profile.getFeatures();
        List<String> newFeatures = new ArrayList<String>();
        for (String feature : features) {
            int idx = feature.lastIndexOf('/');
            if (idx > 0) {
                feature = feature.substring(0, idx);
            }
            newFeatures.add(feature);
        }
        if (!newFeatures.equals(features)) {
            profile.setFeatures(newFeatures);
        }
    }

    protected Set<Patch> doGetPossiblePatches(Set<String> artifacts) {
        Set<Patch> perfectusPatches = loadPerfectusPatches(false);
        Set<Patch> possiblePatches = new TreeSet<Patch>();
        Set<String> otherArtifacts = new TreeSet<String>();
        for (String artifact : artifacts) {
            String mvn = getMavenArtifact(artifact);
            if (mvn == null) {
                continue;
            }
            String[] parts = mvn.split(":");
            String ga = parts[0] + ":" + parts[1];
            org.osgi.framework.Version artifactVersion = VersionTable.getVersion(parts[2]);
            boolean found = false;
            for (Patch patch : perfectusPatches) {
                if (patch.getArtifacts().contains(ga)) {
                    org.osgi.framework.Version ver = VersionTable.getVersion(patch.getVersion());
                    if (isInMajorRange(artifactVersion, ver)) {
                        possiblePatches.add(patch);
                    }
                    found = true;
                }
            }
            if (!found) {
                otherArtifacts.add(artifact);
            }
        }
        if (!otherArtifacts.isEmpty()) {
            Map<String, Set<String>> upgrades = doGetPossibleUpgrades(otherArtifacts);
            for (Map.Entry<String, Set<String>> entry : upgrades.entrySet()) {
                String artifact = entry.getKey();
                for (String version : entry.getValue()) {
                    Patch patch = new PatchImpl(artifact + ":" + version, "|" + artifact.replace(':', '|') + "|" + version, Collections.singleton(artifact),
                            Collections.<Issue> emptyList());
                    possiblePatches.add(patch);
                }
            }
        }
        return possiblePatches;
    }

    protected List<String> doApplyPatches(List<String> artifacts, Set<Patch> patches) {
        List<String> newArtifacts = new ArrayList<String>();
        for (String artifact : artifacts) {
            newArtifacts.add(doApplyPatches(artifact, patches));
        }
        return newArtifacts;
    }

    private String doApplyPatches(String artifact, Set<Patch> patches) {
        String mvn = getMavenArtifact(artifact);
        if (mvn != null) {
            String[] mvnParts = mvn.split(":");
            String art = mvnParts[0] + ":" + mvnParts[1];
            org.osgi.framework.Version v1 = VersionTable.getVersion(mvnParts[2]);
            for (Patch patch : patches) {
                if (patch.getArtifacts().contains(art)) {
                    org.osgi.framework.Version v2 = VersionTable.getVersion(patch.getVersion());
                    if (isInMajorRange(v1, v2)) {
                        artifact = artifact.replace(mvnParts[2], patch.getVersion());
                        mvnParts[2] = patch.getVersion();
                        v1 = v2;
                    }
                }
            }
        }
        return artifact;
    }

    public Set<Patch> loadPerfectusPatches(boolean reload) {
        try {
            Dictionary config = getConfig();
            List<String> repositories = getRepositories(config);
            Set<Patch> patches = loadPerfectusPatches(repositories, reload);
            boolean includeNonFuseVersions = Boolean.parseBoolean((String) config.get(PATCH_INCLUDE_NON_FUSE_VERSION));
            if (!includeNonFuseVersions) {
                Set<Patch> newPatches = new TreeSet<Patch>();
                for (Patch patch : patches) {
                    if (patch.getVersion().contains("fuse")) {
                        newPatches.add(patch);
                    }
                }
                patches = newPatches;
            }
            return patches;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Patch> loadPerfectusPatches(List<String> repositories, boolean reload) throws IOException, InterruptedException {
        File cache = new File(patchDir, CACHE_FILE);
        List<String> locations = null;
        if (!reload && cache.isFile()) {
            try {
                Properties props = loadProperties(cache);
                String lastDateStr = props.getProperty(CACHE_LAST_DATE);
                if (lastDateStr != null) {
                    long date = Long.parseLong(lastDateStr);
                    if (System.currentTimeMillis() - date < TimeUnit.DAYS.toMillis(1)) {
                        locations = new ArrayList<String>();
                        int count = Integer.parseInt(props.getProperty(CACHE_LOCATION + "." + CACHE_COUNT, "0"));
                        for (int i = 0; i < count; i++) {
                            locations.add(props.getProperty(CACHE_LOCATION + "." + Integer.toString(i)));
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("Error loading patch cache.  Cache will be reset.", e);
            }
        }
        if (locations == null) {
            // Load patch locations
            List<String> groups = Arrays.asList(DEFAULT_GROUPS.split(","));
            locations = findPerfectusPatchLocations(repositories, groups);
            reload = true;
        }
        // Load patches
        Set<Patch> patches = loadPerfectusPatches(locations);
        // Save patch locations
        if (reload) {
            try {
                Properties props = new Properties();
                props.setProperty(CACHE_LAST_DATE, Long.toString(System.currentTimeMillis()));
                props.setProperty(CACHE_LOCATION + "." + CACHE_COUNT, Integer.toString(locations.size()));
                for (int i = 0; i < locations.size(); i++) {
                    props.setProperty(CACHE_LOCATION + "." + Integer.toString(i), locations.get(i));
                }
                cache.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(cache);
                try {
                    props.store(fos, "Patch cache");
                } finally {
                    close(fos);
                }
            } catch (Exception e) {
                LOGGER.info("Error storing patch cache", e);
            }
        }
        return patches;
    }

    public List<String> findPerfectusPatchLocations(List<String> repos, List<String> artifacts) throws InterruptedException {
        final List<String> locations = new ArrayList<String>();
        final CountDownLatch latch = new CountDownLatch(repos.size() * artifacts.size());
        for (final String repo : repos) {
            for (final String artifact : artifacts) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String[] mvn = artifact.split(":");
                            URL base = new URL(repo + (repo.endsWith("/") ? "" : "/") + mvn[0].replace('.', '/') + "/" + mvn[1] + "/");
                            URL metadata = new URL(base, "maven-metadata.xml");
                            URLConnection con = metadata.openConnection();
                            if (metadata.getUserInfo() != null) {
                                con.setRequestProperty("Authorization", "Basic " + Base64Encoder.encode(metadata.getUserInfo()));
                            }
                            InputStream is = con.getInputStream();
                            try {
                                Document doc = dbf.newDocumentBuilder().parse(is);
                                NodeList versions = doc.getDocumentElement().getElementsByTagName("version");
                                for (int i = 0; i < versions.getLength(); i++) {
                                    Node version = versions.item(i);
                                    String v = version.getTextContent();
                                    synchronized (locations) {
                                        locations.add(repo + "|" + mvn[0] + "|" + mvn[1] + "|" + v);
                                    }
                                }
                            } finally {
                                close(is);
                            }
                        } catch (FileNotFoundException e) {
                            // Ignore
                        } catch (Exception e) {
                            LOGGER.info("Error in " + repo + " - " + artifact + ": " + e.getMessage(), e);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
        }
        latch.await();
        return locations;
    }

    private Map<String, Set<String>> getMissingArtifacts() {
        Map<String, Set<String>> missingArtifacts = new HashMap<String, Set<String>>();
        String[] patches = MISSING_FEATURES_DESCRIPTOR.split(",");
        for (String patch : patches) {
            String[] artifacts = patch.split("\\|");
            Set<String> set = new HashSet<String>();
            for (int i = 1; i < artifacts.length; i++) {
                set.add(artifacts[i]);
            }
            missingArtifacts.put(artifacts[0], set);
        }
        return missingArtifacts;
    }

    public Set<Patch> loadPerfectusPatches(final List<String> locations) throws InterruptedException {
        final Map<String, Set<String>> missingArtifacts = getMissingArtifacts();
        final Set<Patch> patches = new TreeSet<Patch>();
        final CountDownLatch latch = new CountDownLatch(locations.size());
        for (final String location : new ArrayList<String>(locations)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Load metadata
                        File metadata = download(location, "patch", "patch");
                        Properties props = loadProperties(metadata);
                        // Load issues
                        File issues = download(location, "xml", "issues");
                        List<Issue> issueList = loadIssues(issues);
                        // Build patch
                        PatchDescriptor descriptor = new PatchDescriptor(props);
                        Set<String> bundles = new TreeSet<String>();
                        for (String url : descriptor.getBundles()) {
                            String mvn = getMavenArtifact(url);
                            String[] p = mvn.split(":");
                            bundles.add(p[0] + ":" + p[1]);
                        }
                        String[] mvn = location.split("\\|");
                        Set<String> artifacts = missingArtifacts.get(mvn[1] + ":" + mvn[2]);
                        if (artifacts != null) {
                            bundles.addAll(artifacts);
                        }
                        Patch patch = new PatchImpl(descriptor.getId(), location, bundles, issueList);
                        synchronized (patches) {
                            patches.add(patch);
                        }
                    } catch (FileNotFoundException e) {
                        synchronized (locations) {
                            locations.remove(location);
                        }
                    } catch (Exception e) {
                        LOGGER.info("Error downloading patch " + location, e);
                    } finally {
                        latch.countDown();
                    }
                }

            });
        }
        latch.await();
        return patches;
    }

    protected Dictionary getConfig() {
        try {
            Configuration[] configuration = configAdmin.listConfigurations("(service.pid=" + Constants.AGENT_PID + ")");
            Dictionary dictionary = (configuration != null && configuration.length > 0) ? configuration[0].getProperties() : null;
            return dictionary != null ? dictionary : new Hashtable();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve repositories", e);
        }
    }

    protected List<String> getRepositories(Dictionary dictionary) {
        Object repos = dictionary.get(PATCH_REPOSITORIES);
        if (repos == null) {
            repos = dictionary.get("org.ops4j.pax.url.mvn.repositories");
        }
        if (repos != null) {
            List<String> repositories = new ArrayList<String>();
            for (String repo : repos.toString().split(",")) {
                repositories.add(repo.trim());
            }
            return repositories;
        }
        return Arrays.asList("https://repo.fusesource.com/nexus/content/repositories/releases");
    }

    static void createDir(File dir) {
        if (!dir.isDirectory()) {
            dir.mkdirs();
            if (!dir.isDirectory()) {
                throw new IllegalStateException("Unable to create folder: " + dir);
            }
        }
    }

    File download(String location, String type, String qualifier) throws IOException {
        String[] mvn = location.split("\\|");
        String repo = mvn[0];
        String groupId = mvn[1];
        String artifactId = mvn[2];
        String version = mvn[3];
        URL url = new URL(repo + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version
                + (qualifier != null ? "-" + qualifier : "") + "." + type);
        String karafHom = runtimeProperties.getProperty(SystemProperties.KARAF_HOME);
        File file = new File(karafHom + "/" + runtimeProperties.getProperty("karaf.default.repository") + "/" + groupId.replace('.', '/') + "/" + artifactId + "/"
                + version + "/" + artifactId + "-" + version + (qualifier != null ? "-" + qualifier : "") + "." + type);
        download(file, url);
        return file;
    }

    static void download(File file, URL location) throws IOException {
        if (!file.isFile()) {
            File temp = new File(file.toString() + ".tmp");
            URLConnection con = location.openConnection();
            if (location.getUserInfo() != null) {
                con.setRequestProperty("Authorization", "Basic " + Base64Encoder.encode(location.getUserInfo()));
            }
            if (temp.isFile()) {
                con.setRequestProperty("Range", "Bytes=" + (temp.length()) + "-");
            }
            InputStream is = new BufferedInputStream(con.getInputStream());
            try {
                boolean resume = "bytes".equals(con.getHeaderField("Accept-Ranges"));
                temp.getParentFile().mkdirs();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(temp, resume));
                try {
                    copy(is, os);
                } finally {
                    close(os);
                }
                temp.renameTo(file);
            } finally {
                close(is);
            }
        }
    }

    static List<Issue> loadIssues(File issues) throws SAXException, IOException, ParserConfigurationException {
        FileInputStream fis;
        List<Issue> issueList = new ArrayList<Issue>();
        fis = new FileInputStream(issues);
        try {
            Element root = dbf.newDocumentBuilder().parse(fis).getDocumentElement();
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(ISSUE)) {
                    Element el = (Element) child;
                    String desc = el.getAttribute(ISSUE_DESCRIPTION);
                    List<String> keys = new ArrayList<String>();
                    List<String> arts = new ArrayList<String>();
                    for (Node child2 = el.getFirstChild(); child2 != null; child2 = child2.getNextSibling()) {
                        if (child2.getNodeType() == Node.ELEMENT_NODE) {
                            if (child2.getNodeName().equals(ISSUE_KEY)) {
                                keys.add(child2.getTextContent());
                            } else if (child2.getNodeName().equals(ISSUE_MODULE)) {
                                arts.add(child2.getTextContent());
                            }
                        }
                    }
                    issueList.add(new IssueImpl(desc, keys, arts));
                }
            }
        } finally {
            close(fis);
        }
        return issueList;
    }

    static Properties loadProperties(File file) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(file);
        try {
            props.load(fis);
        } finally {
            close(fis);
        }
        return props;
    }

    static class PatchDescriptor {

        final String id;
        final String description;
        final List<String> bundles;

        PatchDescriptor(Properties properties) {
            this.id = properties.getProperty(PATCH_ID);
            this.description = properties.getProperty(PATCH_DESCRIPTION);
            this.bundles = new ArrayList<String>();
            int count = Integer.parseInt(properties.getProperty(PATCH_BUNDLES + "." + PATCH_COUNT, "0"));
            for (int i = 0; i < count; i++) {
                String url = properties.getProperty(PATCH_BUNDLES + "." + Integer.toString(i));

                String range = properties.getProperty(PATCH_BUNDLES + "." + Integer.toString(i) + "." + PATCH_RANGE);
                if (range != null) {
                    url = String.format("%s;range=%s", url, range);
                }

                this.bundles.add(url);
            }
        }

        PatchDescriptor(String id, String description, List<String> bundles) {
            this.id = id;
            this.description = description;
            this.bundles = bundles;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getBundles() {
            return bundles;
        }
    }

    static boolean isInMajorRange(org.osgi.framework.Version minRange, org.osgi.framework.Version version) {
        if (minRange.getMajor() != version.getMajor()) {
            return false;
        }
        int c = version.getMinor() - minRange.getMinor();
        if (c < 0) {
            return false;
        } else if (c > 0) {
            return true;
        }
        c = version.getMicro() - minRange.getMicro();
        if (c < 0) {
            return false;
        } else if (c > 0) {
            return true;
        }
        String q1 = minRange.getQualifier();
        String q2 = version.getQualifier();
        if (q1.startsWith("fuse-") && q2.startsWith("fuse-")) {
            q1 = cleanQualifierForComparison(q1);
            q2 = cleanQualifierForComparison(q2);
        }
        return q1.compareTo(q2) > 0;
    }

    static int compareFuseVersions(org.osgi.framework.Version v1, org.osgi.framework.Version v2) {
        int c = v1.getMajor() - v2.getMajor();
        if (c != 0) {
            return c;
        }
        c = v1.getMinor() - v2.getMinor();
        if (c != 0) {
            return c;
        }
        c = v1.getMicro() - v2.getMicro();
        if (c != 0) {
            return c;
        }
        String q1 = v1.getQualifier();
        String q2 = v2.getQualifier();
        if (q1.startsWith("fuse-") && q2.startsWith("fuse-")) {
            q1 = cleanQualifierForComparison(q1);
            q2 = cleanQualifierForComparison(q2);
        }
        return q1.compareTo(q2);
    }

    static String cleanQualifierForComparison(String q) {
        if (q.startsWith("fuse-")) {
            return q.replace("-alpha-", "-").replace("-beta-", "-").replace("-7-0-", "-70-").replace("-7-", "-70-");
        } else {
            return q;
        }
    }

    static class FuseVersionComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            org.osgi.framework.Version v1 = VersionTable.getVersion(o1);
            org.osgi.framework.Version v2 = VersionTable.getVersion(o2);
            return compareFuseVersions(v1, v2);
        }
    }

    static void copy(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] b = new byte[4096];
            int l = is.read(b);
            while (l >= 0) {
                os.write(b, 0, l);
                l = is.read(b);
            }
        } finally {
            close(os);
        }
    }

    static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
        }
    }

}
