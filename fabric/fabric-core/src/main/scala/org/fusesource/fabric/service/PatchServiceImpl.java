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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.PatchService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.utils.Base64Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PatchServiceImpl implements PatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatchServiceImpl.class);

    private final FabricService fabric;
    private final List<String> repositories;
    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public PatchServiceImpl(FabricService fabric) {
        this(fabric, Arrays.asList("http://repo.fusesource.com/nexus/content/groups/public/"));
    }

    public PatchServiceImpl(FabricService fabric, List<String> repositories) {
        this.fabric = fabric;
        this.repositories = repositories;
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
        Map<String, Set<String>> artifactVersions = new TreeMap<String, Set<String>>();
        for (Version version : fabric.getVersions()) {
            doGetPossibleUpgrades(version, artifactVersions);
        }
        return artifactVersions;
    }

    @Override
    public Map<String, Set<String>> getPossibleUpgrades(Version version) {
        Map<String, Set<String>> artifactVersions = new TreeMap<String, Set<String>>();
        doGetPossibleUpgrades(version, artifactVersions);
        return artifactVersions;
    }

    @Override
    public Map<String, Set<String>> getPossibleUpgrades(Profile profile) {
        Map<String, Set<String>> artifactVersions = new TreeMap<String, Set<String>>();
        doGetPossibleUpgrades(profile, artifactVersions);
        return artifactVersions;
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

    private void doGetPossibleUpgrades(Version version, Map<String, Set<String>> artifactVersions) {
        for (Profile profile : version.getProfiles()) {
            doGetPossibleUpgrades(profile, artifactVersions);
        }
    }

    private void doGetPossibleUpgrades(Profile profile, Map<String, Set<String>> artifactVersions) {
        doGetPossibleUpgrades(profile.getBundles(), artifactVersions);
        doGetPossibleUpgrades(profile.getFabs(), artifactVersions);
        doGetPossibleUpgrades(profile.getRepositories(), artifactVersions);
    }

    private void doGetPossibleUpgrades(List<String> artifacts, Map<String, Set<String>> artifactVersions) {
        for (String artifact : artifacts) {
            doGetPossibleUpgrades(artifact, artifactVersions);
        }
    }

    private void doGetPossibleUpgrades(String artifact, Map<String, Set<String>> artifactVersions) {
        String mvn = getMavenArtifact(artifact);
        if (mvn != null && !artifactVersions.containsKey(mvn)) {
            Set<String> allVersions = new TreeSet<String>();
            String[] mvnParts = mvn.split(":");
            org.osgi.framework.Version artifactVersion = VersionTable.getVersion(mvnParts[2]);
            VersionRange upgradeRange = new VersionRange(false, artifactVersion, VersionTable.getVersion(artifactVersion.getMajor() + 1, 0, 0), true);
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
                            if (upgradeRange.contains(ver)) {
                                allVersions.add(v.trim());
                            }
                        }
                    } finally {
                        is.close();
                    }
                } catch (Exception e) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Unable to retrieve versions for artifact: " + artifact, e);
                    } else if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Unable to retrieve versions for artifact: " + artifact + ": " + e.getMessage());
                    }
                }
            }
            artifactVersions.put(mvn, allVersions);
        }
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

}