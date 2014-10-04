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
package io.fabric8.process.manager;


import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import io.fabric8.api.Container;

import static com.google.common.base.Objects.firstNonNull;

/**
 * The parameters used to install a jar process
 */
public class InstallOptions implements Serializable {


    private static final long serialVersionUID = 4943127368399800099L;
    public static final String DEFAULT_EXTRACT_CMD = "tar zxf";

    public static class InstallOptionsBuilder<T extends InstallOptionsBuilder> {
        private String id;
        private String name;
        private URL url;
        private URL controllerUrl;
        private String controllerJson;
        private String extractCmd = DEFAULT_EXTRACT_CMD;
        private String[] postUnpackCmds;
        private String[] postInstallCmds;
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
        private Map<String, String> environment = new HashMap<String, String>();
        private String[] jvmOptions = {};
        private Map<String,File> jarFiles = new HashMap<String, File>();
        private Container container;
        private DownloadStrategy downloadStrategy;

        public T id(final String id) {
            this.id = id;
            return (T) this;
        }

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

        public T controllerJson(final String controllerJson) {
            this.controllerJson = controllerJson;
            return (T) this;
        }

        public T extractCmd(final String extract) {
            this.extractCmd = extract;
            return (T) this;
        }

        public T postUnpackCmds(final String[] commands) {
            this.postUnpackCmds = commands;
            return (T) this;
        }

        public T postInstallCmds(final String[] commands) {
            this.postInstallCmds = commands;
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

        public T mainClass(final Class mainClass) {
            this.mainClass = mainClass.getName();
            return (T) this;
        }

        public T jarFiles(final Map<String,File> jarFiles) {
            this.jarFiles = new HashMap<String,File>(jarFiles);
            return (T) this;
        }

        public T container(final Container container) {
            this.container = container;
            if (container != null) {
                id(container.getId());
            }
            return (T) this;
        }

        public T downloadStrategy(final DownloadStrategy downloadStrategy) {
            this.downloadStrategy = downloadStrategy;
            return (T) this;
        }

        public String getId() {
            return id;
        }

        public URL getControllerUrl() {
            return controllerUrl;
        }

        public String getControllerJson() {
            return controllerJson;
        }

        public String getExtractCmd() {
            return extractCmd;
        }

        public String[] getPostUnpackCmds() {
            return postUnpackCmds;
        }

        public String[] getPostInstallCmds() {
            return postInstallCmds;
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

        public Map<String, String> getEnvironment() {
            return environment;
        }

        public String[] getJvmOptions() {
            return jvmOptions;
        }

        public Container getContainer() {
            return container;
        }

        public DownloadStrategy getDownloadStrategy() {
            return downloadStrategy;
        }

        public InstallOptionsBuilder properties(final Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public InstallOptionsBuilder environment(final Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public InstallOptionsBuilder jvmOptions(String... jvmOptions) {
            this.jvmOptions = jvmOptions;
            return this;
        }

        public InstallOptionsBuilder jvmOptionsString(String jvmOptions) {
            this.jvmOptions = jvmOptions.split("\\s+?");
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
                return new InstallOptions(id, getName(), getUrl(), controllerUrl, controllerJson, extractCmd, postUnpackCmds, postInstallCmds, offline, optionalDependencyPatterns, excludeDependencyFilterPatterns, mainClass, properties, environment, jvmOptions, jarFiles, container, downloadStrategy);
        }

        public Map<String, File> getJarFiles() {
            return jarFiles;
        }

    }

    public static InstallOptionsBuilder builder() {
        return new InstallOptionsBuilder();
    }

    private final String id;
    private final String name;
    private final URL url;
    private final URL controllerUrl;
    private final String controllerJson;
    private final String extractCmd;
    private final String[] postUnpackCmds;
    private final String[] postInstallCmds;
    private final boolean offline;
    private final String[] optionalDependencyPatterns;
    private final String[] excludeDependencyFilterPatterns;
    private final String mainClass;
    private final Map<String, Object> properties;
    private final Map<String, String> environment;
    private final String[] jvmOptions;
    private final Map<String, File> jarFiles;
    private final Container container;
    private final DownloadStrategy downloadStrategy;


    public InstallOptions(String id, String name, URL url, URL controllerUrl, String controllerJson, String extractCmd, String[] postUnpackCmds, String[] postInstallCmds, boolean offline, String[] optionalDependencyPatterns, String[] excludeDependencyFilterPatterns, String mainClass, Map<String, Object> properties, Map<String, String> environment, String[] jvmOptions, Map<String, File> jarFiles, Container container, DownloadStrategy downloadStrategy) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.controllerUrl = controllerUrl;
        this.controllerJson = controllerJson;
        this.extractCmd = extractCmd;
        this.postUnpackCmds = postUnpackCmds;
        this.postInstallCmds = postInstallCmds;
        this.offline = offline;
        this.optionalDependencyPatterns = optionalDependencyPatterns;
        this.excludeDependencyFilterPatterns = excludeDependencyFilterPatterns;
        this.mainClass = mainClass;
        this.properties = properties;
        this.environment = environment;
        this.jvmOptions = jvmOptions;
        this.jarFiles = jarFiles;
        this.container = container;
        this.downloadStrategy = downloadStrategy;
    }

    @Override
    public String toString() {
        return "InstallOptions{" +
                "id='" + id + '\'' +
                ", url=" + url +
                ", mainClass='" + mainClass + '\'' +
                ", properties=" + properties +
                ", environment=" + environment +
                ", jvmOptions=" + Arrays.toString(jvmOptions) +
                ", jarFiles=" + jarFiles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstallOptions that = (InstallOptions) o;

        if (offline != that.offline) return false;
        if (controllerJson != null ? !controllerJson.equals(that.controllerJson) : that.controllerJson != null)
            return false;
        if (controllerUrl != null ? !controllerUrl.equals(that.controllerUrl) : that.controllerUrl != null)
            return false;
        if (environment != null ? !environment.equals(that.environment) : that.environment != null) return false;
        if (!Arrays.equals(excludeDependencyFilterPatterns, that.excludeDependencyFilterPatterns)) return false;
        if (extractCmd != null ? !extractCmd.equals(that.extractCmd) : that.extractCmd != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (jarFiles != null ? !jarFiles.equals(that.jarFiles) : that.jarFiles != null) return false;
        if (!Arrays.equals(jvmOptions, that.jvmOptions)) return false;
        if (mainClass != null ? !mainClass.equals(that.mainClass) : that.mainClass != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (!Arrays.equals(optionalDependencyPatterns, that.optionalDependencyPatterns)) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (controllerUrl != null ? controllerUrl.hashCode() : 0);
        result = 31 * result + (controllerJson != null ? controllerJson.hashCode() : 0);
        result = 31 * result + (extractCmd != null ? extractCmd.hashCode() : 0);
        result = 31 * result + (offline ? 1 : 0);
        result = 31 * result + (optionalDependencyPatterns != null ? Arrays.hashCode(optionalDependencyPatterns) : 0);
        result = 31 * result + (excludeDependencyFilterPatterns != null ? Arrays.hashCode(excludeDependencyFilterPatterns) : 0);
        result = 31 * result + (mainClass != null ? mainClass.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        result = 31 * result + (jvmOptions != null ? Arrays.hashCode(jvmOptions) : 0);
        result = 31 * result + (jarFiles != null ? jarFiles.hashCode() : 0);
        return result;
    }

    public String getId() {
        return id;
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

    public String getControllerJson() {
        return controllerJson;
    }

    public String getExtractCmd() {
        return extractCmd;
    }

    public String[] getPostUnpackCmds() {
        return postUnpackCmds;
    }

    public String[] getPostInstallCmds() {
        return postInstallCmds;
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

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public String[] getJvmOptions() {
        return jvmOptions;
    }

    public Map<String, File> getJarFiles() {
        return jarFiles;
    }

    public Container getContainer() {
        return container;
    }

    public DownloadStrategy getDownloadStrategy() {
        return downloadStrategy;
    }
}
