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
package io.fabric8.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the status of the auto scaling of a profile
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoScaleProfileStatus implements Comparable<AutoScaleProfileStatus> {
    private String profile;
    private String status;
    private String dependentProfile;
    private Integer currentInstances;
    private Integer minimumInstances;
    private String message;

    public AutoScaleProfileStatus() {
    }

    public AutoScaleProfileStatus(String profile) {
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AutoScaleProfileStatus that = (AutoScaleProfileStatus) o;

        if (!profile.equals(that.profile)) return false;

        return true;
    }

    @Override
    public int compareTo(AutoScaleProfileStatus o) {
        return this.profile.compareTo(o.profile);
    }

    @Override
    public int hashCode() {
        return profile.hashCode();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getDependentProfile() {
        return dependentProfile;
    }

    public void setDependentProfile(String dependentProfile) {
        this.dependentProfile = dependentProfile;
    }

    public Integer getCurrentInstances() {
        return currentInstances;
    }

    public void setCurrentInstances(Integer currentInstances) {
        this.currentInstances = currentInstances;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public void setMinimumInstances(Integer minimumInstances) {
        this.minimumInstances = minimumInstances;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void provisioned() {
        clear();
        this.status = "success";
    }

    public void provisioning() {
        clear();
        this.status = "provisioning";
    }

    public void creatingContainer() {
        clear();
        this.status = "creating a container";
    }

    public void destroyingContainer() {
        clear();
        this.status = "destroying a container";
    }

    protected void clear() {
        message = "";
        dependentProfile = null;
        currentInstances = null;
        currentInstances = null;
    }

    public void missingDependency(String dependentProfile, Integer currentInstances, Integer minimumInstances) {
        this.status = "waiting";
        this.dependentProfile = dependentProfile;
        this.currentInstances = currentInstances;
        this.minimumInstances = minimumInstances;
        message = "Waiting for profile " + dependentProfile + " to have " + minimumInstances + " instance(s) which currently has " + currentInstances;
    }

    public void noSuitableHost(String requirementsText) {
        this.status = "waiting";
        message = "Waiting for suitable host to become available for requirements: " + requirementsText;
    }
}
