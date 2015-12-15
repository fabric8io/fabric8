/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.git.dto;

/**
 * Represents information about a commit log or history
 */
public class StatusDTO extends GitDTOSupport {
    private final String operation;
    private final String file;

    public StatusDTO(String file, String operation) {
        this.file = file;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "StatusDTO{" +
                "file='" + file + '\'' +
                ", operation='" + operation + '\'' +
                '}';
    }

    public String getFile() {
        return file;
    }

    public String getOperation() {
        return operation;
    }
}
