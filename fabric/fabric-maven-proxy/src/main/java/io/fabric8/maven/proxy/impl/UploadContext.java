/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.maven.proxy.impl;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class UploadContext {
    static final UploadContext ERROR = new UploadContext();

    private boolean status;
    private File file;
    private Map<String, String> headers;

    private String groupId;
    private String artifactId;
    private String version;
    private String type;

    private UploadContext() {
    }

    UploadContext(File file) {
        this.status = true;
        this.file = file;
    }

    public boolean status() {
        return status;
    }

    public File file() {
        return file;
    }

    public void addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
    }

    public Map<String, String> headers() {
        return (headers == null) ? Collections.<String, String>emptyMap() : headers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toArtifact() {
        return String.format("%s:%s:%s:%s", groupId, artifactId, type, version);
    }
}
