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
package io.fabric8.devops;


import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ProjectRepositoryTest {
    @Test
    public void testParseYaml() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File file = new File(basedir, "src/test/resources/projects.yml");
        assertThat(file).exists();

        List<ProjectRepository> projects = ProjectRepositories.loadProjectRepositories(file);
        assertThat(projects).isNotNull().isNotEmpty();

        for (ProjectRepository project : projects) {
            System.out.println("Got project: " + project);

            assertThat(project.isGitHubProject()).isTrue();
            assertThat(project.getGitUrl()).isNotEmpty();
            assertThat(project.getRepoName()).isNotEmpty();
            assertThat(project.getUser()).isNotEmpty();
        }
    }

}
