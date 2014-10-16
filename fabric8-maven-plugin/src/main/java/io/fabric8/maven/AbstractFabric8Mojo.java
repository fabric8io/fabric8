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
package io.fabric8.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Abstract base class for Fabric8 based Mojos
 */
public abstract class AbstractFabric8Mojo extends AbstractMojo {

    /**
     * Name of the created app zip file
     */
    @Parameter(property = "fabric8.zip.file", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.zip")
    protected File zipFile;
    /**
     * The custom kubernetes JSON file
     */
    @Parameter(property = "kubernetesJson", defaultValue = "${basedir}/target/classes/kubernetes.json")
    private File kubernetesJson;

    @Component
    private MavenProject project;

    /**
     * The number of replicas of this container if we are auto generating the kubernetes JSON file (creating
     * a <a href="http://fabric8.io/v2/replicationControllers.html">Replication Controller</a> if this value
     * is greater than 0 or a <a href="http://fabric8.io/v2/pods.html">pod</a> if not).
     */
    @Parameter(property = "fabric8.replicas", defaultValue = "1")
    private Integer replicas;


    /**
     * Whether or not we should ignoreProject this maven project from goals like fabric8:deploy
     */
    @Parameter(property = "fabric8.ignoreProject", defaultValue = "false")
    private boolean ignoreProject;

    public File getKubernetesJson() {
        return kubernetesJson;
    }

    public MavenProject getProject() {
        return project;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public boolean isIgnoreProject() {
        return ignoreProject;
    }

    public File getZipFile() {
        return zipFile;
    }
}
