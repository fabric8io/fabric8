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
package io.fabric8.deployer.dto;

import io.fabric8.insight.log.support.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a dependency tree
 */
public class DependencyDTO {
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String type;
    private String scope;
    private boolean optional;
    private List<DependencyDTO> children = new ArrayList<DependencyDTO>();

    @Override
    public String toString() {
        return "DependencyDTO{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                ", type='" + type + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }

    public void addChild(DependencyDTO childDTO) {
        children.add(childDTO);
    }

    /**
     * Returns the maven URL for the artifact without the version
     */
    public String toBundleUrlWithoutVersion() {
        String prefix = "mvn:";
        if ("war".equals(type)) {
            prefix = "war:" + prefix;
        } else if ("bundle".equals(type)) {
            // use bundles
        } else if (Strings.isEmpty(type) || "jar".equals(type)) {
            prefix = "fab:" + prefix;
        }
        return prefix + groupId + "/" + artifactId + "/";
    }

    /**
     * Returns the maven URL for the artifact
     */
    public String toBundleUrl() {
        // TODO ignores type and classifier
        return toBundleUrlWithoutVersion() + version;
    }

    /**
     * Returns the maven URL for the artifact including any type or modifier
     */
    public String toBundleUrlWithType() {
        String answer = toBundleUrl();
        if (type != null && (!type.equals("jar") && !type.equals("bundle"))) {
            answer += "/" + type;
            if (classifier != null) {
                answer += "/" + classifier;
            }
        }
        return answer;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public List<DependencyDTO> getChildren() {
        return children;
    }

    public void setChildren(List<DependencyDTO> children) {
        this.children = children;
    }

}
