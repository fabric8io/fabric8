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
package io.fabric8.kubernetes.template;

import io.fabric8.utils.Strings;

/**
 * Represents a DTO which can be posted as JSON for creating a new application via a wizard or command line tool
 */
public class CreateAppDTO extends GenerateTemplateDTO {
    private String branch;
    private String path;
    private String summaryMarkdown;
    private String readMeMarkdown;

    @Override
    public String toString() {
        return "CreateAppDTO{" +
                "branch='" + branch + '\'' +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", dockerImage='" + dockerImage + '\'' +
                ", containerName='" + containerName + '\'' +
                ", template='" + template + '\'' +
                ", labels=" + labels +
                ", ports=" + ports +
                ", summaryMarkdown='" + summaryMarkdown + '\'' +
                ", readMeMarkdown='" + readMeMarkdown + '\'' +
                ", templateVariables=" + templateVariables +
                '}';
    }

    public String getBranch() {
        if (Strings.isNullOrBlank(branch)) {
            branch = "master";
        }
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPath() {
        if (Strings.isNullOrBlank(path)) {
            path = "/";
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSummaryMarkdown() {
        if (Strings.isNullOrBlank(summaryMarkdown)) {
            // leave blank for now?
            summaryMarkdown = "";
        }
        return summaryMarkdown;
    }

    public void setSummaryMarkdown(String summaryMarkdown) {
        this.summaryMarkdown = summaryMarkdown;
    }

    public String getReadMeMarkdown() {
        if (Strings.isNullOrBlank(readMeMarkdown)) {
            // leave blank for now?
            readMeMarkdown = "";
        }
        return readMeMarkdown;
    }

    public void setReadMeMarkdown(String readMeMarkdown) {
        this.readMeMarkdown = readMeMarkdown;
    }
}
