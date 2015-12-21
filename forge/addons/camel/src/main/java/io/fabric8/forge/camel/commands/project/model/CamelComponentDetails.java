/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.camel.commands.project.model;

import io.fabric8.forge.addon.utils.JavaHelper;

public class CamelComponentDetails {
    private String componentClassQName;
    private String componentClassName;
    private String groupId;
    private String artifactId;
    private String version;

    public void setComponentClassQName(String componentClassQName) {
        this.componentClassQName = componentClassQName;
        this.componentClassName = JavaHelper.removeJavaPackageName(componentClassQName);
    }

    public String getComponentClassQName() {
        return componentClassQName;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
