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

import org.osgi.service.metatype.AttributeDefinition;

/**
 */
public class MetaTypeAttributeDTO {
    private String id;
    private String name;
    private String description;
    private boolean required;
    private int cardinality;
    private String typeName;
    private String[] defaultValue;
    private String[] optionLabels;
    private String[] optionValues;

    public static String typeName(int type) {
        switch (type) {
            case AttributeDefinition.BOOLEAN:
                return "boolean";
            case AttributeDefinition.BYTE:
                return "byte";
            case AttributeDefinition.CHARACTER:
                return "char";
            case AttributeDefinition.DOUBLE:
                return "double";
            case AttributeDefinition.FLOAT:
                return "float";
            case AttributeDefinition.INTEGER:
                return "int";
            case AttributeDefinition.LONG:
                return "long";
            case AttributeDefinition.PASSWORD:
                return "password";
            case AttributeDefinition.SHORT:
                return "short";
            case AttributeDefinition.STRING:
                return "string";
            case AttributeDefinition.BIGDECIMAL:
                return "bigdecimal";
            case AttributeDefinition.BIGINTEGER:
                return "bigint";
            default:
                return null;
        }
    }

    public MetaTypeAttributeDTO() {
    }

    public MetaTypeAttributeDTO(AttributeDefinition definition, boolean required) {
        this.required = required;
        this.id = definition.getID();
        this.name = definition.getName();
        this.description = definition.getDescription();
        this.cardinality = definition.getCardinality();
        this.typeName = typeName(definition.getType());
        this.defaultValue = definition.getDefaultValue();
        this.optionLabels = definition.getOptionLabels();
        this.optionValues = definition.getOptionValues();
    }

    @Override
    public String toString() {
        return "MetaTypeAttributeDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
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

    public int getCardinality() {
        return cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String[] getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String[] defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String[] getOptionLabels() {
        return optionLabels;
    }

    public void setOptionLabels(String[] optionLabels) {
        this.optionLabels = optionLabels;
    }

    public String[] getOptionValues() {
        return optionValues;
    }

    public void setOptionValues(String[] optionValues) {
        this.optionValues = optionValues;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
