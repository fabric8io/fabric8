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
package io.fabric8.devops.connector;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.ProjectConfigs;
import io.fabric8.devops.ProjectRepositories;
import io.fabric8.devops.ProjectRepository;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;

/**
 */
public class DevOpsConnectors {

    /**
     * Returns a DevOpsConnector for the given project repository
     */
    public static DevOpsConnector createDevOpsConnector(ProjectRepository project) {
        DevOpsConnector connector = new DevOpsConnector();
        connector.setGitUrl(project.getGitUrl());
        String repoName = project.getRepoName();
        connector.setRepoName(repoName);
        String username = project.getUser();
        connector.setUsername(username);

        String buildName = ProjectRepositories.createBuildName(username, repoName);

        if (project.isGitHubProject()) {
            // lets default the issue tracker
            String url = project.getUrl();
            if (Strings.isNotBlank(url)) {
                connector.setIssueTrackerUrl(URLUtils.pathJoin(url, "issues"));
                connector.setTeamUrl(URLUtils.pathJoin(url, "graphs/contributors"));
                connector.setReleasesUrl(URLUtils.pathJoin(url, "tags"));
                connector.setRepositoryBrowseLink(url);
            }

            ProjectConfig config = ProjectConfigs.loadFromUrl(URLUtils.pathJoin(url, "blob/master/fabric8.yml"));
            if (config == null) {
                config = new ProjectConfig();

                // lets add a dummy build so we can at least build snapshots on demand in OpenShift
                config.setPipeline("maven/Deploy.groovy");
            }
            config.setBuildName(buildName);
            connector.setProjectConfig(config);
            connector.setRegisterWebHooks(false);

            System.out.println("Created config " + config.getBuildName() + " with flow " + config.getPipeline());
        }
        return connector;
    }
}
