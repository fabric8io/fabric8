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

