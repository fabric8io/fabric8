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
package io.fabric8.gerrit;

import io.fabric8.repo.git.DtoSupport;

import java.util.List;
import java.util.Map;

public class ProjectInfoDTO extends DtoSupport {
        
        public String id;
        public String name;
        public String parent;
        public String description;
        public ProjectState state;
        public Map<String, String> branches;
        public List<WebLinkInfo> webLinks;


        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getParent() {
                return parent;
        }

        public void setParent(String parent) {
                this.parent = parent;
        }

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public ProjectState getState() {
                return state;
        }

        public void setState(ProjectState state) {
                this.state = state;
        }

        public Map<String, String> getBranches() {
                return branches;
        }

        public void setBranches(Map<String, String> branches) {
                this.branches = branches;
        }

        public List<WebLinkInfo> getWebLinks() {
                return webLinks;
        }

        public void setWebLinks(List<WebLinkInfo> webLinks) {
                this.webLinks = webLinks;
        }

}

