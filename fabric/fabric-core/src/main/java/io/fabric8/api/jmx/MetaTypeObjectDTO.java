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
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class MetaTypeObjectDTO extends MetaTypeObjectSupportDTO {
    private List<MetaTypeAttributeDTO> attributes = new ArrayList<MetaTypeAttributeDTO>();

    public MetaTypeObjectDTO() {
    }

    public MetaTypeObjectDTO(ObjectClassDefinition objectDef) {
        super(objectDef);
        addAttributes(objectDef);
    }

    /**
     * Appends any metadata from an additional object definition from a different bundle
     */
    public void appendObjectDefinition(ObjectClassDefinition objectDef) {
        super.appendObjectDefinition(objectDef);
        addAttributes(objectDef);
    }

    protected void addAttributes(ObjectClassDefinition objectDef) {
        addAttributes(objectDef.getAttributeDefinitions(ObjectClassDefinition.REQUIRED), true);
        addAttributes(objectDef.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL), false);
    }

    protected void addAttributes(AttributeDefinition[] attributeDefinitions, boolean required) {
        if (attributeDefinitions != null) {
            for (AttributeDefinition attributeDefinition : attributeDefinitions) {
                attributes.add(new MetaTypeAttributeDTO(attributeDefinition, required));
            }
        }
    }

    public List<MetaTypeAttributeDTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<MetaTypeAttributeDTO> attributes) {
        this.attributes = attributes;
    }

}
