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

import java.net.URL;
import java.util.Collection;

/**
 * The parameters used to install a jar process
 */
public class JarInstallParameters {
    private URL controllerJson;
    private String groupId;
    private String artifactId;
    private String version;
    private String extension = "jar";
    private String classifier;
    private boolean offline;
    private String[] optionalDependencyPatterns = {};
    private String[] excludeDependencyFilterPatterns = {};
    private String mainClass;

    public URL getControllerJson() {
        return controllerJson;
    }

    public void setControllerJson(URL controllerJson) {
        this.controllerJson = controllerJson;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public String[] getExcludeDependencyFilterPatterns() {
        return excludeDependencyFilterPatterns;
    }

    public void setExcludeDependencyFilterPatterns(String[] excludeDependencyFilterPatterns) {
        this.excludeDependencyFilterPatterns = excludeDependencyFilterPatterns;
    }

    public String[] getOptionalDependencyPatterns() {
        return optionalDependencyPatterns;
    }

    public void setOptionalDependencyPatterns(String[] optionalDependencyPatterns) {
        this.optionalDependencyPatterns = optionalDependencyPatterns;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
}
