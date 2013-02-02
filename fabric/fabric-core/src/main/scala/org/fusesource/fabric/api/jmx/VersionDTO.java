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
package org.fusesource.fabric.api.jmx;

import org.fusesource.fabric.api.HasId;
import org.fusesource.fabric.api.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A DTO for returning versions from JSON MBeans
 */
public class VersionDTO implements HasId {
    private String id;
    private Properties attributes;
    private boolean defaultVersion;

    /**
     * Factory method which handles nulls gracefully
     */
    public static VersionDTO newInstance(Version version) {
        if (version != null) {
            return new VersionDTO(version);
        } else {
            return null;
        }
    }

    public static List<VersionDTO> newInstances(Version... versions) {
        List<VersionDTO> answer = new ArrayList<VersionDTO>();
        if (versions != null) {
            for (Version version : versions) {
                VersionDTO dto = newInstance(version);
                if (dto != null) {
                    answer.add(dto);
                }
            }
        }
        return answer;
    }

    public VersionDTO() {
    }

    public VersionDTO(Version version) {
        this.id = version.getName();
        this.attributes = version.getAttributes();
    }

    public String toString() {
        return "VersionDTO(" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionDTO that = (VersionDTO) o;
        if (!id.equals(that.id)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public Properties getAttributes() {
        return attributes;
    }

    public void setAttributes(Properties attributes) {
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(boolean defaultVersion) {
        this.defaultVersion = defaultVersion;
    }
}
