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
package io.fabric8.docker.provider.javacontainer;

/**
 */
public class JavaContainerOptions {
    private final String baseImage;
    private final String imageRepository;
    private final String newImageTag;
    private final String javaLibraryPath;
    private final String homePath;
    private final String entryPoint;

    public JavaContainerOptions(String baseImage, String imageRepository, String newImageTag, String javaLibraryPath, String homePath, String entryPoint) {
        this.baseImage = baseImage;
        this.imageRepository = imageRepository;
        this.newImageTag = newImageTag;
        this.javaLibraryPath = javaLibraryPath;
        this.homePath = homePath;
        this.entryPoint = entryPoint;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public String getImageRepository() {
        return imageRepository;
    }

    /**
     * The tag name of the newly created image
     */
    public String getNewImageTag() {
        return newImageTag;
    }

    /**
     * Returns the path where java libraries are to be copied
     */
    public String getJavaLibraryPath() {
        return javaLibraryPath;
    }

    /**
     * Returns the home directory where things get installed and we apply any overlay files to
     */
    public String getHomePath() {
        return homePath;
    }

    /**
     * The image entry point command
     */
    public String getEntryPoint() {
        return entryPoint;
    }
}
