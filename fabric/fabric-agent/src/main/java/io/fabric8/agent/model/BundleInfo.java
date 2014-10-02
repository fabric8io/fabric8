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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Deployable element to install.
 * <p/>
 * <p/>
 * <p>Java class for bundle complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="bundle">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>anyURI">
 *       &lt;attribute name="start-level" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="start" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="dependency" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "bundle", propOrder = {
        "value"
        })
public class BundleInfo {

    @XmlValue
    @XmlSchemaType(name = "anyURI")
    protected String value;
    @XmlAttribute(name = "start-level")
    protected Integer startLevel;
    @XmlAttribute
    protected Boolean start; // = true;
    @XmlAttribute
    protected Boolean dependency;


    public BundleInfo() {
    }

    public BundleInfo(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLocation() {
        return value.trim();
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
     * Gets the value of the startLevel property.
     *
     * @return possible object is
     * {@link Integer }
     */
    public int getStartLevel() {
        return startLevel == null ? 0 : startLevel;
    }

    /**
     * Sets the value of the startLevel property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setStartLevel(Integer value) {
        this.startLevel = value;
    }

    /**
     * Gets the value of the start property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public boolean isStart() {
        return start == null ? true : start;
    }

    /**
     * Sets the value of the start property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setStart(Boolean value) {
        this.start = value;
    }

    /**
     * Gets the value of the dependency property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public boolean isDependency() {
        return dependency == null ? false : dependency;
    }

    /**
     * Sets the value of the dependency property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setDependency(Boolean value) {
        this.dependency = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BundleInfo bundle = (BundleInfo) o;
        if (dependency != bundle.dependency) {
            return false;
        }
        if (start != bundle.start) {
            return false;
        }
        if ((startLevel != null ? startLevel : 0) != (bundle.startLevel != null ? bundle.startLevel : 0)) {
            return false;
        }
        if (value != null ? !value.equals(bundle.value) : bundle.value != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + getStartLevel();
        result = 31 * result + (isStart() ? 1 : 0);
        result = 31 * result + (isDependency() ? 1 : 0);
        return result;
    }
}
