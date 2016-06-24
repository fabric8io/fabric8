/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.devops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.utils.Maps;
import io.fabric8.utils.Strings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the project configuration YAML file which allows a project to be configured
 * for <a href="http://fabric8.io/devops/">Fabric8 DevOps</a>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProjectConfig {
    private String pipeline;
    private String chatRoom;
    private String issueProjectName;
    private String buildName;
    private Boolean codeReview;
    private Map<String, String> links;
    private Map<String, String> buildParameters;
    private LinkedHashMap<String, String> environments;
    private Boolean useLocalFlow;

    @Override
    public String toString() {
        return "ProjectConfig{" +
                "flow=" + pipeline +
                ", chatRoom='" + chatRoom + '\'' +
                ", buildName='" + buildName + '\'' +
                ", issueProjectName='" + issueProjectName + '\'' +
                '}';
    }

    public void addLink(String name, String url) {
        if (links == null) {
            links = new TreeMap<>();
        }
        links.put(name, url);
    }

    public String getLink(String name) {
        if (links != null) {
            return links.get(name);
        }
        return null;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return codeReview == null && Strings.isNullOrBlank(chatRoom) && Strings.isNullOrBlank(issueProjectName) && Strings.isNullOrBlank(pipeline)
                && Maps.isNullOrEmpty(buildParameters) && Maps.isNullOrEmpty(environments) && useLocalFlow != null;
    }

    public boolean hasCodeReview() {
        return codeReview != null && codeReview.booleanValue();
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
    }

    public Boolean getCodeReview() {
        return codeReview;
    }

    public void setCodeReview(Boolean codeReview) {
        this.codeReview = codeReview;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public String getIssueProjectName() {
        return issueProjectName;
    }

    public void setIssueProjectName(String issueProjectName) {
        this.issueProjectName = issueProjectName;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public Map<String, String> getBuildParameters() {
        return buildParameters;
    }

    public void setBuildParameters(Map<String, String> buildParameters) {
        this.buildParameters = buildParameters;
    }

    public LinkedHashMap<String, String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(LinkedHashMap<String, String> environments) {
        this.environments = environments;
    }

    public void setUseLocalFlow(Boolean useLocalFlow) {
        this.useLocalFlow = useLocalFlow;
    }

    public Boolean getUseLocalFlow() {
        return useLocalFlow;
    }

    public boolean isUseLocalFlow() {
        return useLocalFlow != null && useLocalFlow.booleanValue();
    }
}
