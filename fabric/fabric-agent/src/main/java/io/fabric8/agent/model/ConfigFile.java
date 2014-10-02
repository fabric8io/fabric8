/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Additional configuration files which should be created during feature installation.
 * <p/>
 * <p/>
 * <p>Java class for configFile complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="configFile">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="finalname" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="override" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "configFile", propOrder = {"value"})
public class ConfigFile {

    @XmlValue
    protected String value;
    @XmlAttribute(required = true)
    protected String finalname;
    @XmlAttribute
    protected Boolean override;

    /**
     * Gets the value of the value property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLocation() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLocation(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the finalname property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getFinalname() {
        return finalname;
    }

    /**
     * Sets the value of the finalname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFinalname(String value) {
        this.finalname = value;
    }

    /**
     * Gets the value of the override property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public boolean isOverride() {
        return override == null ? false : override;
    }

    /**
     * Sets the value of the override property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setOverride(Boolean value) {
        this.override = value;
    }
}
