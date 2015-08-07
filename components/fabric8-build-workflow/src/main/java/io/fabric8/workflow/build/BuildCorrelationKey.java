/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.workflow.build;

import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;

/**
 * Represents a build correlation key to uniquely identify a build job
 */
public class BuildCorrelationKey {
    private final String namespace;
    private final String buildName;
    private final String buildUuid;

    public static BuildCorrelationKey create(BuildFinishedEvent event) {
        return new BuildCorrelationKey(event.getNamespace(), event.getConfigName(), event.getUid());
    }

    public BuildCorrelationKey(String namespace, String buildName, String buildUuid) {
        this.namespace = namespace;
        this.buildName = buildName;
        this.buildUuid = buildUuid;
    }

    @Override
    public String toString() {
        return "BuildCorrelationKey{" +
                "namespace='" + namespace + '\'' +
                ", buildName='" + buildName + '\'' +
                ", buildUuid='" + buildUuid + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildCorrelationKey that = (BuildCorrelationKey) o;

        if (!buildName.equals(that.buildName)) return false;
        if (!buildUuid.equals(that.buildUuid)) return false;
        if (!namespace.equals(that.namespace)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + buildName.hashCode();
        result = 31 * result + buildUuid.hashCode();
        return result;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildUuid() {
        return buildUuid;
    }

    public String getNamespace() {
        return namespace;
    }
}
