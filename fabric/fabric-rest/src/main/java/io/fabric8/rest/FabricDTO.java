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
package io.fabric8.rest;

import io.fabric8.api.Container;
import io.fabric8.api.Containers;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profiles;
import io.fabric8.api.jmx.ContainerDTO;
import io.fabric8.core.jmx.Links;

import java.net.URI;
import java.util.List;

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
    private String requirementsLink;
    private String registryLink;
    private String zooKeeperLink;

    public FabricDTO(FabricService fabricService, URI baseUri) {
    }

    public FabricDTO(FabricService fabricService) {
        this.zookeeperUrl = fabricService.getZookeeperUrl();
        this.currentContainerName = fabricService.getCurrentContainerName();
        this.defaultVersion = fabricService.getDefaultVersionId();
        this.environment = fabricService.getEnvironment();
        this.mavenRepoURI = asString(fabricService.getMavenRepoURI());
        this.mavenRepoUploadURI = asString(fabricService.getMavenRepoUploadURI());
        this.defaultJvmOptions = fabricService.getDefaultJvmOptions();
    }

    public FabricDTO(FabricService fabricService, String containersLink, String versionsLink, String statusLink, String requirementsLink, String registryLink, String zooKeeperLink) {
        this(fabricService);
        this.containersLink = containersLink;
        this.versionsLink = versionsLink;
        this.statusLink = statusLink;
        this.requirementsLink = requirementsLink;
        this.registryLink = registryLink;
        this.zooKeeperLink = zooKeeperLink;
    }

    public static ContainerDTO createContainerDTO(Container container, String baseApiLink) {
        ContainerDTO answer = new ContainerDTO();
        String containerId = container.getId();
        answer.setId(containerId);
        answer.setType(container.getType());

        answer.setChildren(Containers.containerIds(container.getChildren()));
        List<String> profileIds = Profiles.profileIds(container.getProfiles());
        String profileLinkPrefix = baseApiLink + "/version/" + Profiles.versionId(container.getVersion()) + "/profile/";
        answer.setProfiles(Links.mapIdsToLinks(profileIds, profileLinkPrefix));
        answer.setVersion(Profiles.versionId(container.getVersion()));
        answer.setParent(Containers.containerId(container.getParent()));

        answer.setIp(container.getIp());
        answer.setLocalIp(container.getLocalIp());
        answer.setManualIp(container.getManualIp());
        answer.setPublicIp(container.getPublicIp());
        answer.setLocalHostName(container.getLocalHostname());
        answer.setPublicHostName(container.getPublicHostname());
        answer.setResolver(container.getResolver());
        answer.setMaximumPort(container.getMaximumPort());
        answer.setMinimumPort(container.getMinimumPort());

        answer.setGeoLocation(container.getGeoLocation());
        answer.setLocation(container.getLocation());

        answer.setProcessId(container.getProcessId());
        answer.setDebugPort(container.getDebugPort());
        answer.setHttpUrl(container.getHttpUrl());
        answer.setJmxUrl(container.getJmxUrl());
        answer.setJolokiaUrl(container.getJolokiaUrl());
        answer.setSshUrl(container.getSshUrl());

        answer.setProvisionException(container.getProvisionException());
        answer.setProvisionResult(container.getProvisionResult());
        answer.setProvisionStatus(container.getProvisionStatus());
        answer.setProvisionList(container.getProvisionList());
        answer.setJmxDomains(container.getJmxDomains());

        answer.setAlive(container.isAlive());
        answer.setAliveAndOK(container.isAliveAndOK());
        answer.setEnsembleServer(container.isEnsembleServer());
        answer.setManaged(container.isManaged());
        answer.setProvisioningComplete(container.isProvisioningComplete());
        answer.setProvisioningPending(container.isProvisioningPending());
        answer.setRoot(container.isRoot());

        answer.setStartLink(baseApiLink + "/container/" + containerId + "/start");
        return answer;
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

    public String getRequirementsLink() {
        return requirementsLink;
    }

    public void setRequirementsLink(String requirementsLink) {
        this.requirementsLink = requirementsLink;
    }

    public String getRegistryLink() {
        return registryLink;
    }

    public void setRegistryLink(String registryLink) {
        this.registryLink = registryLink;
    }

    public String getZooKeeperLink() {
        return zooKeeperLink;
    }

    public void setZooKeeperLink(String zooKeeperLink) {
        this.zooKeeperLink = zooKeeperLink;
    }

    protected static String asString(URI uri) {
        return uri != null ? uri.toString() : null;
    }

}
