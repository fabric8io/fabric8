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
package io.fabric8.tooling.archetype.catalog;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * A simple DTO
 */
@XmlRootElement(name = "archetype")
@XmlType(propOrder = {
    "groupId",
    "artifactId",
    "version",
    "repository",
    "description"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class Archetype {

    @XmlElement
    public String groupId = "";
    @XmlElement
    public String artifactId = "";
    @XmlElement
    public String version = "";
    @XmlElement
    public String repository = "";
    @XmlElement
    public String description = "";

    public Archetype() {
    }

    public Archetype(String groupId, String artifactId, String version, String description) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Archtype(" + groupId + ":" + artifactId + ":" + version + ")";
    }

}
