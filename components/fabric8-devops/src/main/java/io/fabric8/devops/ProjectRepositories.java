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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.devops.ProjectConfigs.parseYamlValues;

/**
 * Helper methods for working with {@link ProjectRepository} objects
 */
public class ProjectRepositories {

    public static List<ProjectRepository> loadProjectRepositories(File yamlFile) throws IOException {
        if (yamlFile.exists() && yamlFile.isFile()) {
            return parseYamlValues(yamlFile, ProjectRepository.class);
        } else {
            return new ArrayList<>();
        }
    }

    public static String createBuildName(String username, String repoName) {
        return repoName;
    }
}
