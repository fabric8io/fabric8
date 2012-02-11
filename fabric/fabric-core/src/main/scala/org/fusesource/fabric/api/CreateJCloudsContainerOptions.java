/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.api;

/**
 * Arguments for creating a new container via JClouds
 */
public class CreateJCloudsContainerOptions extends CreateContainerBasicOptions<CreateJCloudsContainerOptions> {
    private static final long serialVersionUID = 4489740280396972109L;


    private String imageId;
    private String hardwareId;
    private String locationId;
    private String group;
    private String user;
    private String providerName;
    private JCloudsInstanceType instanceType;
    private String identity;
    private String credential;
    private String owner;
    private Integer servicePort = 0;

    public CreateJCloudsContainerOptions() {
        this.providerType = "jclouds";
    }

    @Override
    public String toString() {
        return "CreateJCloudsContainerArguments{" +
                "imageId='" + imageId + '\'' +
                ", hardwareId='" + hardwareId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", group='" + group + '\'' +
                ", user='" + user + '\'' +
                ", instanceType='" + instanceType + '\'' +
                '}';
    }


    public CreateJCloudsContainerOptions imageId(final String imageId) {
        this.imageId = imageId;
        return this;
    }

    public CreateJCloudsContainerOptions hardwareId(final String hardwareId) {
        this.hardwareId = hardwareId;
        return this;
    }

    public CreateJCloudsContainerOptions locationId(final String locationId) {
        this.locationId = locationId;
        return this;
    }

    public CreateJCloudsContainerOptions group(final String group) {
        this.group = group;
        return this;
    }

    public CreateJCloudsContainerOptions user(final String user) {
        this.user = user;
        return this;
    }

    public CreateJCloudsContainerOptions providerName(final String providerName) {
        this.providerName = providerName;
        return this;
    }

    public CreateJCloudsContainerOptions instanceType(final JCloudsInstanceType instanceType) {
        this.instanceType = instanceType;
        return this;
    }

    public CreateJCloudsContainerOptions identity(final String identity) {
        this.identity = identity;
        return this;
    }

    public CreateJCloudsContainerOptions credential(final String credential) {
        this.credential = credential;
        return this;
    }

    public CreateJCloudsContainerOptions owner(final String owner) {
        this.owner = owner;
        return this;
    }

    public CreateJCloudsContainerOptions servicePort(final Integer servicePort) {
        this.servicePort = servicePort;
        return this;
    }

    public String getImageId() {
        return imageId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getGroup() {
        return group;
    }

    public String getUser() {
        return user;
    }

    public String getProviderName() {
        return providerName;
    }

    public JCloudsInstanceType getInstanceType() {
        return instanceType;
    }

    public String getIdentity() {
        return identity;
    }

    public String getCredential() {
        return credential;
    }

    public String getOwner() {
        return owner;
    }

    public Integer getServicePort() {
        return servicePort;
    }
}
