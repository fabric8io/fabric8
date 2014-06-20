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
package io.fabric8.api.jmx;

import io.fabric8.api.FabricService;
import io.fabric8.api.Profiles;

import java.net.URI;

/**
 * A DTO for core fabric information
 */
public class FabricDTO {
    private String zookeeperUrl;
    private String currentContainerName;
    private String defaultVersion;
    private String environment;
    private String mavenRepoURI;
    private String mavenRepoUploadURI;
    private String defaultJvmOptions;
    private String containersLink;
    private String versionsLink;
    private String statusLink;

    public FabricDTO(FabricService fabricService, URI baseUri) {
    }

    public FabricDTO(FabricService fabricService) {
        this.zookeeperUrl = fabricService.getZookeeperUrl();
        this.currentContainerName = fabricService.getCurrentContainerName();
        this.defaultVersion = Profiles.versionId(fabricService.getDefaultVersion());
        this.environment = fabricService.getEnvironment();
        this.mavenRepoURI = asString(fabricService.getMavenRepoURI());
        this.mavenRepoUploadURI = asString(fabricService.getMavenRepoUploadURI());
        this.defaultJvmOptions = fabricService.getDefaultJvmOptions();
    }

    public FabricDTO(FabricService fabricService, String containersLink, String versionsLink, String statusLink) {
        this(fabricService);
        this.containersLink = containersLink;
        this.versionsLink = versionsLink;
        this.statusLink = statusLink;
    }

    @Override
    public String toString() {
        return "FabricDTO{" +
                "zookeeperUrl='" + zookeeperUrl + '\'' +
                ", currentContainerName='" + currentContainerName + '\'' +
                ", defaultVersion='" + defaultVersion + '\'' +
                ", mavenRepoURI='" + mavenRepoURI + '\'' +
                ", mavenRepoUploadURI='" + mavenRepoUploadURI + '\'' +
                '}';
    }

    public String getZookeeperUrl() {
        return zookeeperUrl;
    }

    public void setZookeeperUrl(String zookeeperUrl) {
        this.zookeeperUrl = zookeeperUrl;
    }

    public String getCurrentContainerName() {
        return currentContainerName;
    }

    public void setCurrentContainerName(String currentContainerName) {
        this.currentContainerName = currentContainerName;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMavenRepoURI() {
        return mavenRepoURI;
    }

    public void setMavenRepoURI(String mavenRepoURI) {
        this.mavenRepoURI = mavenRepoURI;
    }

    public String getMavenRepoUploadURI() {
        return mavenRepoUploadURI;
    }

    public void setMavenRepoUploadURI(String mavenRepoUploadURI) {
        this.mavenRepoUploadURI = mavenRepoUploadURI;
    }

    public String getDefaultJvmOptions() {
        return defaultJvmOptions;
    }

    public void setDefaultJvmOptions(String defaultJvmOptions) {
        this.defaultJvmOptions = defaultJvmOptions;
    }

    public String getContainersLink() {
        return containersLink;
    }

    public void setContainersLink(String containersLink) {
        this.containersLink = containersLink;
    }

    public String getVersionsLink() {
        return versionsLink;
    }

    public void setVersionsLink(String versionsLink) {
        this.versionsLink = versionsLink;
    }

    public String getStatusLink() {
        return statusLink;
    }

    public void setStatusLink(String statusLink) {
        this.statusLink = statusLink;
    }

    protected static String asString(URI uri) {
        return uri != null ? uri.toString() : null;
    }

}
