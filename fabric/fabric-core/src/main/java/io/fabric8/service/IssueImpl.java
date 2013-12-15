/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.service;

import java.util.List;

import io.fabric8.api.Issue;

public class IssueImpl implements Issue {
    private final String description;
    private final List<String> keys;
    private final List<String> artifacts;

    public IssueImpl(String description, List<String> keys, List<String> artifacts) {
        this.description = description;
        this.keys = keys;
        this.artifacts = artifacts;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<String> getArtifacts() {
        return artifacts;
    }

    @Override
    public String toString() {
        return "Issue[" +
                "description='" + description + '\'' +
                ", keys=" + keys +
                ", artifacts=" + artifacts +
                ']';
    }
}
