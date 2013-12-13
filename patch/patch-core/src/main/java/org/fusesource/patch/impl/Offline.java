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
package org.fusesource.patch.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Version;

import static org.fusesource.patch.impl.Offline.Artifact.isSameButVersion;
import static org.fusesource.patch.impl.Utils.close;
import static org.fusesource.patch.impl.Utils.copy;
import static org.fusesource.patch.impl.Utils.readLines;
import static org.fusesource.patch.impl.Utils.writeLines;

public class Offline {

    private static final String OVERRIDE_RANGE = ";range=";

    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;

    private final File karafBase;
    private final Logger logger;

    public interface Logger {
        void log(int level, String message);
    }

    public static class SysLogger implements Logger {
        @Override
        public void log(int level, String message) {
            switch (level) {
                case Offline.DEBUG: System.out.println("DEBUG: " + message); break;
                case Offline.INFO:  System.out.println("INFO:  " + message); break;
                case Offline.WARN:  System.out.println("WARN:  " + message); break;
                case Offline.ERROR: System.out.println("ERROR: " + message); break;
            }
        }
    }

    public Offline(File karafBase) {
        this(karafBase, new SysLogger());
    }

    public Offline(File karafBase, Logger logger) {
        this.karafBase = karafBase;
        this.logger = logger;
    }

    public void apply(File patchZip) throws IOException {
        ZipFile zipFile = new ZipFile(patchZip);
        try {
            List<PatchData> patches = extractPatch(zipFile);
            if (patches.isEmpty()) {
                log(WARN, "No patch to apply");
            } else {
                for (PatchData data : patches) {
                    applyPatch(data, zipFile);
                }
            }
        } finally {
            close(zipFile);
        }
    }

    public void applyConfigChanges(PatchData patch) throws IOException {
        applyPatch(patch, null);
    }

    protected List<PatchData> extractPatch(ZipFile zipFile) throws IOException {
        List<PatchData> patches = new ArrayList<PatchData>();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String entryName = entry.getName();
                if (entryName.endsWith(".patch") && !entryName.contains("/")) {
                    InputStream fis = zipFile.getInputStream(entry);
                    try {
                        PatchData patch = PatchData.load(fis);
                        patches.add(patch);
                    } finally {
                        close(fis);
                    }
                }
            }
        }
        return patches;
    }

    protected void applyPatch(PatchData patch, ZipFile zipFile) throws IOException {
        log(DEBUG, "Applying patch: " + patch.getId() + " / " + patch.getDescription());

        File startupFile = new File(karafBase, "etc/startup.properties");
        File overridesFile = new File(karafBase, "etc/overrides.properties");

        List<String> startup = readLines(new File(karafBase, "etc/startup.properties"));
        List<String> overrides = readLines(overridesFile);

        List<Artifact> toExtract = new ArrayList<Artifact>();
        List<Artifact> toDelete = new ArrayList<Artifact>();

        for (String bundle : patch.getBundles()) {

            Artifact artifact = mvnurlToArtifact(bundle, true);
            if (artifact == null) {
                continue;
            }

            // Compute patch bundle version and range
            VersionRange range;
            Version oVer = VersionTable.getVersion(artifact.getVersion());
            String vr = patch.getVersionRange(bundle);
            String override;
            if (vr != null && !vr.isEmpty()) {
                override = bundle + OVERRIDE_RANGE + vr;
                range = VersionRange.parseVersionRange(vr);
            } else {
                override = bundle;
                Version v1 = new Version(oVer.getMajor(), oVer.getMinor(), 0);
                Version v2 = new Version(oVer.getMajor(), oVer.getMinor() + 1, 0);
                range = new VersionRange(false, v1, v2, true);
            }

            // Process overrides.properties
            boolean matching = false;
            boolean added = false;
            for (int i = 0; i < overrides.size(); i++) {
                String line = overrides.get(i).trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    Artifact overrideArtifact = mvnurlToArtifact(line, true);
                    if (overrideArtifact != null) {
                        Version ver = VersionTable.getVersion(overrideArtifact.getVersion());
                        if (isSameButVersion(artifact, overrideArtifact) && range.contains(ver)) {
                            matching = true;
                            if (ver.compareTo(oVer) < 0) {
                                // Replace old override with the new one
                                overrides.set(i, override);
                                if (!added) {
                                    log(DEBUG, "Replacing with artifact: " + override);
                                    added = true;
                                }
                                // Remove old file
                                toDelete.add(overrideArtifact);
                                toExtract.remove(overrideArtifact);
                            }
                        }
                    } else {
                        log(WARN, "Unable to convert to artifact: " + line);
                    }
                }
            }
            // If there was not matching bundles, add it
            if (!matching) {
                overrides.add(override);
                log(DEBUG, "Adding artifact: " + override);
            }

            // Process startup.properties
            for (int i = 0; i < startup.size(); i++) {
                String line = startup.get(i).trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    int index = line.indexOf('=');
                    String mvnUrl = pathToMvnurl(line.substring(0, index));
                    if (mvnUrl != null) {
                        Artifact startupArtifact = mvnurlToArtifact(mvnUrl, true);
                        if (startupArtifact != null) {
                            Version ver = VersionTable.getVersion(startupArtifact.getVersion());
                            if (isSameButVersion(artifact, startupArtifact) && range.contains(ver)) {
                                matching = true;
                                // Now check versions
                                if (ver.compareTo(oVer) < 0) {
                                    line = artifact.getPath() + line.substring(index);
                                    startup.set(i, line);
                                    log(DEBUG, "Overwriting startup.properties with: " + artifact);
                                    added = true;
                                }
                            }
                        }
                    }
                }
            }

            // Extract artifact
            if (!matching || added) {
                toExtract.add(artifact);
            }
            // TODO: process framework ?
            // TODO: process lib folder ?

        }

        // Extract / delete artifacts if needed
        if (zipFile != null) {
            for (Artifact artifact : toExtract) {
                log(DEBUG, "Extracting artifact: " + artifact);
                ZipEntry entry = zipFile.getEntry("repository/" + artifact.getPath());
                if (entry == null) {
                    log(ERROR, "Could not find artifact in patch zip: " + artifact);
                    continue;
                }
                File f = new File(karafBase, "system/" + artifact.getPath());
                if (!f.isFile()) {
                    f.getParentFile().mkdirs();
                    InputStream fis = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(f);
                    try {
                        copy(fis, fos);
                    } finally {
                        close(fis, fos);
                    }
                }
            }
            for (Artifact artifact : toDelete) {
                String fileName = artifact.getPath();
                File file = new File(karafBase, "system/" + fileName);
                if (file.exists()) {
                    log(DEBUG, "Removing old artifact " + artifact);
                    file.delete();
                } else {
                    log(WARN, "Could not find: " + file);
                }
            }
        }

        overrides = new ArrayList<String>(new HashSet<String>(overrides));
        Collections.sort(overrides);
        writeLines(overridesFile, overrides);
        writeLines(startupFile, startup);

    }

    protected void log(int level, String message) {
        logger.log(level, message);
    }

    protected String pathToMvnurl(String path) {
        String[] p = path.split("/");
        if (p.length >= 4 && p[p.length-1].startsWith(p[p.length-3] + "-" + p[p.length-2])) {
            String artifactId = p[p.length-3];
            String version = p[p.length-2];
            String classifier;
            String type;
            String artifactIdVersion = artifactId + "-" + version;
            StringBuffer sb = new StringBuffer();
            if (p[p.length-1].charAt(artifactIdVersion.length()) == '-') {
                classifier = p[p.length-1].substring(artifactIdVersion.length() + 1, p[p.length-1].lastIndexOf('.'));
            } else {
                classifier = null;
            }
            type = p[p.length-1].substring(p[p.length-1].lastIndexOf('.') + 1);
            sb.append("mvn:");
            for (int j = 0; j < p.length - 3; j++) {
                if (j > 0) {
                    sb.append('.');
                }
                sb.append(p[j]);
            }
            sb.append('/').append(artifactId).append('/').append(version);
            if (!"jar".equals(type) || classifier != null) {
                sb.append('/');
                if (!"jar".equals(type)) {
                    sb.append(type);
                }
                if (classifier != null) {
                    sb.append('/').append(classifier);
                }
            }
            return sb.toString();
        }
        return null;
    }

    static Artifact mvnurlToArtifact(String resourceLocation, boolean skipNonMavenProtocols) {
        resourceLocation = resourceLocation.replace("\r\n", "").replace("\n", "").replace(" ", "").replace("\t", "");
        final int index = resourceLocation.indexOf("mvn:");
        if (index < 0) {
            if (skipNonMavenProtocols) {
                return null;
            }
            throw new IllegalArgumentException("Resource URL is not a maven URL: " + resourceLocation);
        } else {
            resourceLocation = resourceLocation.substring(index + "mvn:".length());
        }
        // Truncate the URL when a '#', a '?' or a '$' is encountered
        final int index1 = resourceLocation.indexOf('?');
        final int index2 = resourceLocation.indexOf('#');
        int endIndex = -1;
        if (index1 > 0) {
            if (index2 > 0) {
                endIndex = Math.min(index1, index2);
            } else {
                endIndex = index1;
            }
        } else if (index2 > 0) {
            endIndex = index2;
        }
        if (endIndex >= 0) {
            resourceLocation = resourceLocation.substring(0, endIndex);
        }
        final int index3 = resourceLocation.indexOf('$');
        if (index3 > 0) {
            resourceLocation = resourceLocation.substring(0, index3);
        }

        String[] parts = resourceLocation.split("/");
        if (parts.length > 2) {
            String groupId = parts[0];
            String artifactId = parts[1];
            String version = parts[2];
            String type = "jar";
            String classifier = null;
            if (parts.length > 3) {
                type = parts[3];
                if (parts.length > 4) {
                    classifier = parts[4];
                }
            }
            return new Artifact(groupId, artifactId, version, type, classifier);
        }
        throw new IllegalArgumentException("Bad maven url: " + resourceLocation);
    }

    public static class Artifact {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String type;
        private final String classifier;

        public Artifact(String groupId, String artifactId, String version, String type, String classifier) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.type = type;
            this.classifier = classifier;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getType() {
            return type;
        }

        public String getClassifier() {
            return classifier;
        }

        public boolean hasClassifier() {
            return classifier != null;
        }

        public String getPath() {
            return groupId.replace('.', '/')
                    + '/'
                    + artifactId
                    + '/'
                    + version
                    + '/'
                    + artifactId
                    + (classifier != null ? "-" + classifier : "")
                    + '-'
                    + version
                    + '.'
                    + type;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId)
                    .append(":")
                    .append(artifactId)
                    .append(":")
                    .append(version);
            if (!"jar".equals(type) || classifier != null) {
                sb.append(":").append(type);
                if (classifier != null) {
                    sb.append(":").append(classifier);
                }
            }
            return sb.toString();
        }

        public static boolean isSameButVersion(Artifact a1, Artifact a2) {
            return a1.getGroupId().equals(a2.getGroupId())
                    && a1.getArtifactId().equals(a2.getArtifactId())
                    && a1.hasClassifier() == a2.hasClassifier()
                    && (!a1.hasClassifier() || a1.getClassifier().equals(a2.getClassifier()))
                    && a1.getType().equals(a2.getType());
        }

    }
}
