/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.repo.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.repo.git.DtoSupport;

/**
 */
public class CreateProjectDTO extends DtoSupport {
    // required
    private int userId;
    private String name;

    // not required
    private String path;
    private int namespaceId;
    private String description;
    private Boolean issuesEnabled = true;
    private Boolean mergeRequestsEnabled = true;
    private Boolean buildsEnabled;
    private Boolean wikiEnabled = true;
    private Boolean snippetsEnabled = true;
    private Boolean containerRegistryEnabled;
    private Boolean sharedRunnersEnabled;
    @JsonProperty("public")
    private Boolean publicProject = true;
    private Integer visibilityLevel;
    private String importUrl;
    private Boolean publicBuilds;
    private Boolean onlyAllowMergeIfBuildSucceeds;
    private Boolean onlyAllowMergeIfAllDiscussionsAreResolved;
    private Boolean lfsEnabled;
    private Boolean requestAccessEnabled;

    public CreateProjectDTO() {
    }

    public CreateProjectDTO(int userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    @Override
    public String toString() {
        return "CreateProjectDTO{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", namespaceId=" + namespaceId +
                ", description='" + description + '\'' +
                '}';
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(int namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIssuesEnabled() {
        return issuesEnabled;
    }

    public void setIssuesEnabled(Boolean issuesEnabled) {
        this.issuesEnabled = issuesEnabled;
    }

    public Boolean getMergeRequestsEnabled() {
        return mergeRequestsEnabled;
    }

    public void setMergeRequestsEnabled(Boolean mergeRequestsEnabled) {
        this.mergeRequestsEnabled = mergeRequestsEnabled;
    }

    public Boolean getBuildsEnabled() {
        return buildsEnabled;
    }

    public void setBuildsEnabled(Boolean buildsEnabled) {
        this.buildsEnabled = buildsEnabled;
    }

    public Boolean getWikiEnabled() {
        return wikiEnabled;
    }

    public void setWikiEnabled(Boolean wikiEnabled) {
        this.wikiEnabled = wikiEnabled;
    }

    public Boolean getSnippetsEnabled() {
        return snippetsEnabled;
    }

    public void setSnippetsEnabled(Boolean snippetsEnabled) {
        this.snippetsEnabled = snippetsEnabled;
    }

    public Boolean getContainerRegistryEnabled() {
        return containerRegistryEnabled;
    }

    public void setContainerRegistryEnabled(Boolean containerRegistryEnabled) {
        this.containerRegistryEnabled = containerRegistryEnabled;
    }

    public Boolean getSharedRunnersEnabled() {
        return sharedRunnersEnabled;
    }

    public void setSharedRunnersEnabled(Boolean sharedRunnersEnabled) {
        this.sharedRunnersEnabled = sharedRunnersEnabled;
    }

    public Boolean getPublicProject() {
        return publicProject;
    }

    public void setPublicProject(Boolean publicProject) {
        this.publicProject = publicProject;
    }

    public Integer getVisibilityLevel() {
        return visibilityLevel;
    }

    public void setVisibilityLevel(Integer visibilityLevel) {
        this.visibilityLevel = visibilityLevel;
    }

    public String getImportUrl() {
        return importUrl;
    }

    public void setImportUrl(String importUrl) {
        this.importUrl = importUrl;
    }

    public Boolean getPublicBuilds() {
        return publicBuilds;
    }

    public void setPublicBuilds(Boolean publicBuilds) {
        this.publicBuilds = publicBuilds;
    }

    public Boolean getOnlyAllowMergeIfBuildSucceeds() {
        return onlyAllowMergeIfBuildSucceeds;
    }

    public void setOnlyAllowMergeIfBuildSucceeds(Boolean onlyAllowMergeIfBuildSucceeds) {
        this.onlyAllowMergeIfBuildSucceeds = onlyAllowMergeIfBuildSucceeds;
    }

    public Boolean getOnlyAllowMergeIfAllDiscussionsAreResolved() {
        return onlyAllowMergeIfAllDiscussionsAreResolved;
    }

    public void setOnlyAllowMergeIfAllDiscussionsAreResolved(Boolean onlyAllowMergeIfAllDiscussionsAreResolved) {
        this.onlyAllowMergeIfAllDiscussionsAreResolved = onlyAllowMergeIfAllDiscussionsAreResolved;
    }

    public Boolean getLfsEnabled() {
        return lfsEnabled;
    }

    public void setLfsEnabled(Boolean lfsEnabled) {
        this.lfsEnabled = lfsEnabled;
    }

    public Boolean getRequestAccessEnabled() {
        return requestAccessEnabled;
    }

    public void setRequestAccessEnabled(Boolean requestAccessEnabled) {
        this.requestAccessEnabled = requestAccessEnabled;
    }
}
