package io.fabric8.repo.git;

import java.util.List;
import java.util.Map;

public class GerritProjectInfoDTO {
        
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


        public enum ProjectState {
                ACTIVE,
                READ_ONLY,
                HIDDEN
        }

        public class WebLinkInfo {
                public String name;
                public String url;

                public WebLinkInfo(String name, String url) {
                        this.name = name;
                        this.url = url;
                }
        }


}

