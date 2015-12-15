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
package io.fabric8.devops;

import io.fabric8.utils.Strings;

import java.util.Objects;

/**
 * Link to a project repository usually in a git repo somewhere
 */
public class ProjectRepository {
    private String kind;
    private String url;
    private String gitUrl;
    private String repoName;
    private String user;

    @Override
    public String toString() {
        return "ProjectRepository{" +
                "kind='" + kind + '\'' +
                ", user='" + getUser() + '\'' +
                ", repoName='" + getRepoName() + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGitUrl() {
        if (Strings.isNullOrBlank(gitUrl) && Strings.isNotBlank(url)) {
            if (isGitHubProject()) {
                gitUrl = url + ".git";
            }
        }
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getRepoName() {
        if (repoName == null) {
            extractOrganisationAndUserFromUrl();
        }
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getUser() {
        if (user == null) {
            extractOrganisationAndUserFromUrl();
        }
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public boolean isGitHubProject() {
        return Objects.equals(getKind(), Kinds.GITHUB);
    }

    protected void extractOrganisationAndUserFromUrl() {
        if (Strings.isNotBlank(url)) {
            String text = Strings.stripSuffix(url, "./");
            text = Strings.stripSuffix(text, ".git");
            text = Strings.stripSuffix(text, "/");
            String[] split = text.split("/");
            if (split != null && split.length > 1) {
                if (Strings.isNullOrBlank(user)) {
                    user = split[split.length - 2];
                }
                if (Strings.isNullOrBlank(repoName)) {
                    repoName = split[split.length - 1];
                }
            }
        }
    }

    public static class Kinds {
        public static final String GITHUB = "GitHubProjectRepository";
    }
}
