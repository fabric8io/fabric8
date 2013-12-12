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
package io.fabric8.api.jmx;

/**
 * Returns the health status of profiles
 */
public class HealthStatus {
    private final String healthId;
    private final String profile;
    private final String level;
    private final String message;
    private final int instances;
    private final Integer minimumInstances;
    private final Integer maximumInstances;
    private final double healthPercent;

    public HealthStatus(String healthId, String profile, String level, String message, int instances, Integer minimumInstances, Integer maximumInstances, double healthPercent) {
        this.healthId = healthId;
        this.profile = profile;
        this.level = level;
        this.message = message;
        this.instances = instances;
        this.minimumInstances = minimumInstances;
        this.maximumInstances = maximumInstances;
        this.healthPercent = healthPercent;
    }

    public String getHealthId() {
        return healthId;
    }

    public double getHealthPercent() {
        return healthPercent;
    }

    public int getInstances() {
        return instances;
    }

    public String getLevel() {
        return level;
    }

    public Integer getMaximumInstances() {
        return maximumInstances;
    }

    public String getMessage() {
        return message;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public String getProfile() {
        return profile;
    }
}
