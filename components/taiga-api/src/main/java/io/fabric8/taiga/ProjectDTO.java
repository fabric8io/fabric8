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
package io.fabric8.taiga;

import java.util.List;

/**
 */
public class ProjectDTO extends EntitySupport {
    private String name;
    private String slug;
    private String description;
    private List<UserDTO> users;
    private boolean isBacklogActivated;
    private boolean isKanbanActivated;
    private boolean isWikiActivated;
    private boolean isIssuesActivated;

    @Override
    public String toString() {
        return "ProjectDTO{" +
                "name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBacklogActivated() {
        return isBacklogActivated;
    }

    public void setIsBacklogActivated(boolean isBacklogActivated) {
        this.isBacklogActivated = isBacklogActivated;
    }

    public boolean isIssuesActivated() {
        return isIssuesActivated;
    }

    public void setIsIssuesActivated(boolean isIssuesActivated) {
        this.isIssuesActivated = isIssuesActivated;
    }

    public boolean isKanbanActivated() {
        return isKanbanActivated;
    }

    public void setIsKanbanActivated(boolean isKanbanActivated) {
        this.isKanbanActivated = isKanbanActivated;
    }

    public boolean isWikiActivated() {
        return isWikiActivated;
    }

    public void setIsWikiActivated(boolean isWikiActivated) {
        this.isWikiActivated = isWikiActivated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }
}
