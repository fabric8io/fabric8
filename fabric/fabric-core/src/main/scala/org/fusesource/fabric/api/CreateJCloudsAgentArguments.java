/**
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
package org.fusesource.fabric.api;

import java.io.Serializable;

/**
 * Arguments for creating a new agent via JClouds
 */
public class CreateJCloudsAgentArguments extends BasicCreateAgentArguements implements CreateAgentArguments, Serializable {
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

    @Override
    public String toString() {
        return "CreateJCloudsAgentArguments{" +
                "imageId='" + imageId + '\'' +
                ", hardwareId='" + hardwareId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", group='" + group + '\'' +
                ", user='" + user + '\'' +
                ", instanceType='" + instanceType + '\'' +
                '}';
    }


    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public JCloudsInstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(JCloudsInstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
