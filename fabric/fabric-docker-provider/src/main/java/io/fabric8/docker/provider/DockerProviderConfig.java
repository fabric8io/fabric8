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
package io.fabric8.docker.provider;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

/**
 * Represents the configuration for a Docker Provider to specify in the profile which docker image to use
 */
@Component(name = DockerConstants.DOCKER_PROVIDER_PID,
        label = "Docker Provider",
        description = "The configuration of the docker image to use in this profile",
        immediate = true, metatype = true)
public class DockerProviderConfig {
    @Property(label = "Image",
            description = "The docker image name used to create a docker container")
    private String image;

    @Property(label = "Command",
            description = "The command to be used to start the docker container which overrides the entry point inside the image")
    private String cmd;

    @Property(label = "Java Library Path",
            description = "The path that java libraries should be installed into inside the docker image for Java containers or application servers")
    private String javaLibraryPath;

    @Property(label = "Home path",
            description = "The home directory inside the docker image")
    private String homePath;

    @Property(label = "Image repository",
            description = "The docker image repository")
    private String imageRepository;

    @Property(label = "Image entry point",
            description = "The docker image entry point command used")
    private String imageEntryPoint;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getJavaLibraryPath() {
        return javaLibraryPath;
    }

    public void setJavaLibraryPath(String javaLibraryPath) {
        this.javaLibraryPath = javaLibraryPath;
    }

    public String getHomePath() {
        return homePath;
    }

    public void setHomePath(String homePath) {
        this.homePath = homePath;
    }

    public String getImageRepository() {
        return imageRepository;
    }

    public void setImageRepository(String imageRepository) {
        this.imageRepository = imageRepository;
    }

    public String getImageEntryPoint() {
        return imageEntryPoint;
    }

    public void setImageEntryPoint(String imageEntryPoint) {
        this.imageEntryPoint = imageEntryPoint;
    }
}
