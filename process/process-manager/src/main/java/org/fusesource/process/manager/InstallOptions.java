/*
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
package org.fusesource.process.manager;


import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

import static com.google.common.base.Objects.firstNonNull;

/**
 * The parameters used to install a jar process
 */
public class InstallOptions implements Serializable {


    private static final long serialVersionUID = 4943127368399800099L;
    public static final String DEFAULT_EXTRACT_CMD = "tar zxf";

    public static class InstallOptionsBuilder<T extends InstallOptionsBuilder> {

        private String name;
        private URL url;
        private URL controllerUrl;
        private String extractCmd = DEFAULT_EXTRACT_CMD;
        private String groupId;
        private String artifactId;
        private String version = "LATEST";
        private String extension = "jar";
        private String classifier;
        private boolean offline;
        private String[] optionalDependencyPatterns = {};
        private String[] excludeDependencyFilterPatterns = {};
        private String mainClass;
        private Map<String, Object> properties = new HashMap<String , Object>();

        public T name(final String name) {
            this.name = name;
            return (T) this;
        }

        public T url(final URL url) {
            this.url = url;
            return (T) this;
        }

        public T url(final String url) throws MalformedURLException {
            this.url = new URL(url);
            return (T) this;
        }

        public T controllerUrl(final URL controllerUrl) {
            this.controllerUrl = controllerUrl;
            return (T) this;
        }

        public T controllerUrl(final String controllerUrl) throws MalformedURLException {
            this.controllerUrl = new URL(controllerUrl);
            return (T) this;
        }

        public T extractCmd(final String extract) {
            this.extractCmd = extract;
            return (T) this;
        }

        public T groupId(final String groupId) {
            this.groupId = groupId;
            return (T) this;
        }

        public T artifactId(final String artifactId) {
            this.artifactId = artifactId;
            return (T) this;
        }

        public T version(final String version) {
            if (!Strings.isNullOrEmpty(version)) {
                this.version = version;
            }
            return (T) this;
        }

        public T extension(final String extension) {
            if (!Strings.isNullOrEmpty(extension)) {
                this.extension = extension;
            }
            return (T) this;
        }

        public T classifier(final String classifier) {
            this.classifier = classifier;
            return (T) this;
        }

        public T offline(final boolean offline) {
            this.offline = offline;
            return (T) this;
        }

        public T optionalDependencyPatterns(final String... optionalDependencyPatterns) {
            this.optionalDependencyPatterns = firstNonNull(optionalDependencyPatterns, new String[0]);
            return (T) this;
        }

        public T excludeDependencyFilterPatterns(final String... excludeDependencyFilterPatterns) {
            this.excludeDependencyFilterPatterns = firstNonNull(excludeDependencyFilterPatterns, new String[0]);
            return (T) this;
        }

        public T mainClass(final String mainClass) {
            this.mainClass = mainClass;
            return (T) this;
        }

        public URL getControllerUrl() {
            return controllerUrl;
        }

        public String getExtractCmd() {
            return extractCmd;
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

        public String getExtension() {
            return extension;
        }

        public String getClassifier() {
            return classifier;
        }

        public boolean isOffline() {
            return offline;
        }

        public String[] getOptionalDependencyPatterns() {
            return optionalDependencyPatterns;
        }

        public String[] getExcludeDependencyFilterPatterns() {
            return excludeDependencyFilterPatterns;
        }

        public String getMainClass() {
            return mainClass;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public InstallOptionsBuilder properties(final Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }


        public URL getUrl() throws MalformedURLException {
            if (url != null) {
                return url;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("mvn:");
            boolean invalid = false;
            //Required: groupId
            if (groupId != null) {
                sb.append(groupId).append("/");
            } else {
                invalid = true;
            }
            //Required: artifactId
            if (artifactId != null) {
                sb.append(artifactId).append("/");
            } else {
                invalid = true;
            }
            //Required: version
            if (version != null) {
                sb.append(version);
            } else {
                invalid = true;
            }
            //Optiona: extention
            if (extension != null) {
                sb.append("/").append(extension);
            }

            //Optiona: extention
            if (classifier != null) {
                sb.append("/").append(classifier);
            }

            if (invalid) {
                return null;
            }

            return new URL(sb.toString());
        }

        public String getName() {
            if (!Strings.isNullOrEmpty(name)) {
                return name;
            } else if (!Strings.isNullOrEmpty(groupId) && !Strings.isNullOrEmpty(artifactId) && !Strings.isNullOrEmpty(version)) {
                return groupId + ":" + artifactId + ":" + version;
            } else {
                return null;
            }
        }

        public InstallOptions build() throws MalformedURLException {
                return new InstallOptions(getName(), getUrl(), controllerUrl, extractCmd, offline, optionalDependencyPatterns, excludeDependencyFilterPatterns, mainClass, properties);
        }
    }

    public static InstallOptionsBuilder builder() {
        return new InstallOptionsBuilder();
    }

    private final String name;
    private final URL url;
    private final URL controllerUrl;
    private final String extractCmd;
    private final boolean offline;
    private final String[] optionalDependencyPatterns;
    private final String[] excludeDependencyFilterPatterns;
    private final String mainClass;
    private final Map<String, Object> properties;

    public InstallOptions(String name, URL url, URL controllerUrl, String extractCmd, boolean offline, String[] optionalDependencyPatterns, String[] excludeDependencyFilterPatterns, String mainClass, Map<String, Object> properties) {
        this.name = name;
        this.url = url;
        this.controllerUrl = controllerUrl;
        this.extractCmd = extractCmd;
        this.offline = offline;
        this.optionalDependencyPatterns = optionalDependencyPatterns;
        this.excludeDependencyFilterPatterns = excludeDependencyFilterPatterns;
        this.mainClass = mainClass;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public URL getUrl() {
        return url;
    }

    public URL getControllerUrl() {
        return controllerUrl;
    }

    public String getExtractCmd() {
        return extractCmd;
    }

    public boolean isOffline() {
        return offline;
    }

    public String[] getExcludeDependencyFilterPatterns() {
        return excludeDependencyFilterPatterns;
    }

    public String[] getOptionalDependencyPatterns() {
        return optionalDependencyPatterns;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
