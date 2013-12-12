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
package io.fabric8.osgimetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import io.fabric8.watcher.Processor;
import io.fabric8.watcher.file.FileWatcher;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Maven2MetadataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(Maven2MetadataProvider.class);


    private static final Set<String> HEADERS = new HashSet<String>(Arrays.<String>asList(
            Constants.BUNDLE_MANIFESTVERSION,
            Constants.BUNDLE_SYMBOLICNAME,
            Constants.BUNDLE_VERSION,
            Constants.REQUIRE_BUNDLE,
            Constants.FRAGMENT_HOST,
            Constants.IMPORT_PACKAGE,
            Constants.EXPORT_PACKAGE,
            Constants.DYNAMICIMPORT_PACKAGE,
            Constants.REQUIRE_CAPABILITY,
            Constants.PROVIDE_CAPABILITY,
            Constants.EXPORT_SERVICE,
            Constants.IMPORT_SERVICE
    ));

    private final Path root;
    private final FileWatcher watcher;
    private final Map<String, Map<String, String>> metadatas;

    public Maven2MetadataProvider(String path, String dirMatcher, String fileMatcher) throws IOException {
        metadatas = new ConcurrentHashMap<String, Map<String, String>>();
        root = Paths.get(path);
        watcher = new FileWatcher();
        watcher.setRoot(root);
        if (dirMatcher != null && !dirMatcher.isEmpty()) {
            watcher.setDirMatchPattern(dirMatcher);
        }
        if (fileMatcher != null && !fileMatcher.isEmpty()) {
            watcher.setFileMatchPattern(fileMatcher);
        }
        watcher.setProcessor(new Processor() {
            @Override
            public void process(Path path) {
                scan(path);
            }
            @Override
            public void onRemove(Path path) {
                unscan(path);
            }
        });
        watcher.init();
    }

    public void destroy() {
        watcher.destroy();
    }

    public long getLastModified() {
        return watcher.getLastModified();
    }

    public Map<String, Map<String, String>> getMetadatas() {
        return metadatas;
    }

    private void scan(Path path) {
        try {
            Manifest man;
            JarFile jar = new JarFile(path.toFile());
            try {
                man = jar.getManifest();
            } finally {
                jar.close();
            }
            if (man == null) {
                return;
            }
            // Only scan OSGi r4 bundles
            if ("2".equals(man.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION))) {
                // Only scan valid addressable jars
                String uri = getResourceUri(path);
                if (uri != null) {
                    Map<String, String> headers = new HashMap<String, String>();
                    for (Map.Entry attr : man.getMainAttributes().entrySet()) {
                        if (HEADERS.contains(attr.getKey().toString())) {
                            headers.put(attr.getKey().toString(), attr.getValue().toString());
                        }
                    }
                    Map<String, String> prev = metadatas.put(uri, headers);
                    if (prev == null) {
                        LOGGER.debug("ADD {}", uri);
                    } else {
                        LOGGER.debug("UPD {}", uri);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.info("Unable to scan resource " + path, e);
        }
    }

    private void unscan(final Path file) {
        String uri = getResourceUri(file);
        if (uri != null) {
            Map<String, String> prev = metadatas.remove(uri);
            if (prev != null) {
                LOGGER.debug("DEL {}", uri);
            }
        }
    }

    private String getResourceUri(Path path) {
        return convertToMavenUrl(root.relativize(path).toString());
    }

    private static String convertToMavenUrl(String location) {
        String[] p = location.split("/");
        if (p.length >= 4 && p[p.length-1].startsWith(p[p.length-3] + "-" + p[p.length-2])) {
            String artifactId = p[p.length-3];
            String version = p[p.length-2];
            String classifier;
            String type;
            String artifactIdVersion = artifactId + "-" + version;
            StringBuilder sb = new StringBuilder();
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
                sb.append(type);
                if (classifier != null) {
                    sb.append('/').append(classifier);
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }

}
