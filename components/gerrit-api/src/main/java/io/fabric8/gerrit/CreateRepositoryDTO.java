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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.repo.git.DtoSupport;

/**
 */
public class CreateRepositoryDTO extends DtoSupport {
    private String name;
    private String description;
    private boolean create_empty_commit;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCreate_empty_commit() {
        return create_empty_commit;
    }

    public void setCreate_empty_commit(boolean create_empty_commit) {
        this.create_empty_commit = create_empty_commit;
    }

}
