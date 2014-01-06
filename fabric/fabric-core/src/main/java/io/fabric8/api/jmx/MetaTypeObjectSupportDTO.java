/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.jmx;

import org.fusesource.insight.log.support.Strings;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Base class for DTOs for MetaTypeObject
 */
public abstract class MetaTypeObjectSupportDTO {
    private String id;
    private String name;
    private String description;

    public MetaTypeObjectSupportDTO() {
    }

    public MetaTypeObjectSupportDTO(String id) {
        this.id = id;
    }

    public MetaTypeObjectSupportDTO(ObjectClassDefinition objectDef) {
        this.id = objectDef.getID();
        this.name = objectDef.getName();
        this.description = objectDef.getDescription();
    }

    public void appendObjectDefinition(ObjectClassDefinition objectDef) {
        if (Strings.isEmpty(id)) {
            this.id = objectDef.getID();
        }
        if (Strings.isEmpty(name)) {
            this.name = objectDef.getName();
        }
        if (Strings.isEmpty(description)) {
            this.description = objectDef.getDescription();
        }
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
