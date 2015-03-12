/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.model;

import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import io.fabric8.forge.rest.dto.ProjectDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Represents the storage of the known projects on the file system
 */
@Singleton
public class ProjectsModel {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectsModel.class);

    private final File projectsDir;
    private final File projectsFile;
    private List<ProjectDTO> projects;

    @Inject
    public ProjectsModel(@ConfigProperty(name = "FORGE_PROJECTS_DIRECTORY", defaultValue = "forgeProjects") String projectsFolder) throws IOException {
        this.projectsDir = new File(projectsFolder);
        this.projectsDir.mkdirs();
        this.projectsFile = new File(projectsDir, "projects.json");
        this.projects = Models.loadJsonValues(projectsFile, ProjectDTO.class);
    }

    public List<ProjectDTO> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    public File getProjectsDir() {
        return projectsDir;
    }

    public File getProjectsFile() {
        return projectsFile;
    }

    public ProjectDTO findByPath(String path) {
        if (Strings.isNotBlank(path)) {
            for (ProjectDTO project : projects) {
                if (Objects.equal(path, project.getPath())) {
                    return project;
                }
            }
            // we may not have the starting slash if we're using URI templates with the path inside
            if (!path.startsWith("/")) {
                return findByPath("/" + path);
            }
        }
        return null;
    }

    public void setProjects(List<ProjectDTO> projects) throws IOException {
        this.projects = projects;
        save(projects);
    }

    public void add(ProjectDTO element) throws IOException {
        if (!projects.contains(element)) {
            projects.add(element);
            save(projects);
        }
    }

    public void remove(ProjectDTO element) throws IOException {
        String path = element.getPath();
        ProjectDTO deleteElement = findByPath(path);
        if (deleteElement != null) {
            if (projects.remove(deleteElement)) {
                save(projects);
                LOG.info("Removed project " + deleteElement);
            } else {
                LOG.warn("Could not find project: " + element);
            }
        } else {
            LOG.warn("No project for path: " + path);
        }
    }

    public void remove(String path) throws IOException {
        ProjectDTO project = findByPath(path);
        if (project != null) {
            remove(project);
        } else {
            LOG.warn("Could not find a project for path: " + path);
        }
    }

    protected void save(List<ProjectDTO> projects) throws IOException {
        Models.saveJson(projectsFile, projects);
    }

}
