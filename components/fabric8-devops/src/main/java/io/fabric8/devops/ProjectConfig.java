/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.devops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.utils.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the project configuration YAML file which allows a project to be configured
 * for <a href="http://fabric8.io/devops/">Fabric8 DevOps</a>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProjectConfig {

    private List<String> flows = new ArrayList<>();
    private String chatRoom;
    private String issueProjectName;
    private Boolean codeReview;

    @Override
    public String toString() {
        return "ProjectConfig{" +
                "flows=" + flows +
                ", chatRoom='" + chatRoom + '\'' +
                ", issueProjectName='" + issueProjectName + '\'' +
                '}';
    }

    public void addFlow(String flow) {
        if (flows == null) {
            flows = new ArrayList<>();
        }
        flows.add(flow);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return codeReview == null && Strings.isNullOrBlank(chatRoom) && Strings.isNullOrBlank(issueProjectName) && (flows == null || flows.isEmpty());
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

    public List<String> getFlows() {
        return flows;
    }

    public void setFlows(List<String> flows) {
        this.flows = flows;
    }

    public String getIssueProjectName() {
        return issueProjectName;
    }

    public void setIssueProjectName(String issueProjectName) {
        this.issueProjectName = issueProjectName;
    }

}
