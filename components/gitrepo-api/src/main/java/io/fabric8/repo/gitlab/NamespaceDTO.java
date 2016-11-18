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
package io.fabric8.repo.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.repo.git.EntitySupport;
import io.fabric8.utils.Objects;

/**
 */
public class NamespaceDTO extends EntitySupport {
    private String path;
    private String kind;

    @Override
    public String toString() {
        return "NamespaceDTO{" +
                "path='" + path + '\'' +
                ", kind='" + kind + '\'' +
                '}';
    }

    @JsonIgnore
    public boolean isUser() {
        return Objects.equal("user", kind);
    }

    @JsonIgnore
    public boolean isGroup() {
        return Objects.equal("group", kind);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
