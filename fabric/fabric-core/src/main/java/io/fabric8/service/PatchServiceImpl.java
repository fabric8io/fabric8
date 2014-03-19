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
package io.fabric8.service;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.fabric8.api.FabricService;
import io.fabric8.api.PatchService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.utils.Base64Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchServiceImpl implements PatchService {

    private static final String PATCH_ID = "id";
    private static final String PATCH_DESCRIPTION = "description";
    private static final String PATCH_BUNDLES = "bundle";
    private static final String PATCH_COUNT = "count";
    private static final String PATCH_RANGE = "range";

    private static final Logger LOGGER = LoggerFactory.getLogger(PatchServiceImpl.class);

    private final FabricService fabric;

    public PatchServiceImpl(FabricService fabric) {
        this.fabric = fabric;
    }

    @Override
    public void applyPatch(Version version, URL patch, String login, String password) {
        try {
            // Load patch
            URI uploadUri = fabric.getMavenRepoUploadURI();
            List<PatchDescriptor> descriptors = new ArrayList<PatchDescriptor>();
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(patch.openStream()));
            try {
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (entryName.startsWith("repository/")) {
                            String fileName = entryName.substring("repository/".length());
                            while (fileName.startsWith("/")) {
                                fileName = fileName.substring(1);
                            }
                            URL uploadUrl = uploadUri.resolve(fileName).toURL();
                            URLConnection con = uploadUrl.openConnection();
                            if (con instanceof HttpURLConnection) {
                                ((HttpURLConnection) con).setRequestMethod("PUT");
                            }
                            if (login != null && password != null) {
                                con.setRequestProperty("Authorization", "Basic " + Base64Encoder.encode(login + ":" + password));
                            }
                            con.setDoInput(true);
                            con.setDoOutput(true);
                            con.connect();
                            OutputStream os = con.getOutputStream();
                            try {
                                try {
                                    copy(zis, os);
                                    if (con instanceof HttpURLConnection) {
                                        int code = ((HttpURLConnection) con).getResponseCode();
                                        if (code < 200 || code >= 300) {
                                            throw new IOException("Error uploading patched jars: " + ((HttpURLConnection) con).getResponseMessage());
                                        }
                                    }
                                } finally {
                                    zis.closeEntry();
                                }
                            } finally {
                                close(os);
                            }
                        } else if (entryName.endsWith(".patch") && !entryName.contains("/")) {
                            try {
                                Properties patchMetadata = new Properties();
                                patchMetadata.load(zis);
                                descriptors.add(new PatchDescriptor(patchMetadata));
                            } finally {
                                zis.closeEntry();
                            }
                        }
                    }
                    entry = zis.getNextEntry();
                }
            } finally {
                close(zis);
            }
            // Create patch profile
            Profile[] profiles = version.getProfiles();
            for (PatchDescriptor descriptor : descriptors) {
                String profileId = "patch-" + descriptor.getId();
                Profile profile = null;
                for (Profile p : profiles) {
                    if (profileId.equals(p.getId())) {
                        profile = p;
                        break;
                    }
                }
                if (profile == null) {
                    profile = version.createProfile(profileId);
                    profile.setOverrides(descriptor.getBundles());
                    Profile defaultProfile = version.getProfile("default");
                    List<Profile> parents = new ArrayList<Profile>(Arrays.asList(defaultProfile.getParents()));
                    if (!parents.contains(profile)) {
                        parents.add(profile);
                        defaultProfile.setParents(parents.toArray(new Profile[parents.size()]));
                    }
                } else {
                    LOGGER.info("The patch {} has already been applied to version {}, ignoring.", descriptor.getId(), version.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to apply patch", e);
        }
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
