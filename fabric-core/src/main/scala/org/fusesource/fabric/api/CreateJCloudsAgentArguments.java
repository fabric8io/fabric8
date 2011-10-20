/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.io.Serializable;

/**
 * Arguments for creating a new agent via JClouds
 */
public class CreateJCloudsAgentArguments implements CreateAgentArguments, Serializable {
    private static final long serialVersionUID = 1L;

    private boolean debugAgent;
    private String imageId;
    private String hardwareId;
    private String locationId;
    private String group;
    private String user;
    private String providerName;
    private JCloudsInstanceType instanceType;

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

    public boolean isDebugAgent() {
        return debugAgent;
    }

    public void setDebugAgent(boolean debugAgent) {
        this.debugAgent = debugAgent;
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
}
