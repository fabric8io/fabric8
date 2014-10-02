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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.felix.utils.version.VersionCleaner;


/**
 * Definition of the Feature.
 * <p/>
 * <p/>
 * <p>Java class for feature complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="feature">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="details" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="config" type="{http://karaf.apache.org/xmlns/features/v1.0.0}config" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="configfile" type="{http://karaf.apache.org/xmlns/features/v1.0.0}configFile" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="feature" type="{http://karaf.apache.org/xmlns/features/v1.0.0}dependency" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="bundle" type="{http://karaf.apache.org/xmlns/features/v1.0.0}bundle" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="conditional" type="{http://karaf.apache.org/xmlns/features/v1.0.0}conditional" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="capability" type="{http://karaf.apache.org/xmlns/features/v1.0.0}capability" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="requirement" type="{http://karaf.apache.org/xmlns/features/v1.0.0}requirement" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://karaf.apache.org/xmlns/features/v1.0.0}featureName" />
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="resolver" type="{http://karaf.apache.org/xmlns/features/v1.0.0}resolver" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "feature", propOrder = {
        "details",
        "config",
        "configfile",
        "feature",
        "bundle",
        "conditional",
        "capability",
        "requirement",
        "scoping"
        })
public class Feature extends Content {

    public static final String VERSION_SEPARATOR = "/";
    public static final String DEFAULT_VERSION = "0.0.0";


    protected String details;
    @XmlAttribute(required = true)
    protected String name;
    @XmlTransient
    protected String version;
    @XmlAttribute
    protected String description;
    @XmlAttribute
    protected String resolver;
    @XmlAttribute
    protected String install;
    @XmlAttribute(name = "start-level")
    protected Integer startLevel;
    @XmlAttribute
    protected Boolean hidden;
    protected List<Conditional> conditional;
    protected List<Capability> capability;
    protected List<Requirement> requirement;
    protected Scoping scoping;

    public Feature() {
    }

    public Feature(String name) {
        this.name = name;
    }

    public Feature(String name, String version) {
        this.name = name;
        this.version = VersionCleaner.clean(version);
    }


    public static Feature valueOf(String str) {
        if (str.contains(VERSION_SEPARATOR)) {
            String strName = str.substring(0, str.indexOf(VERSION_SEPARATOR));
            String strVersion = str.substring(str.indexOf(VERSION_SEPARATOR)
                    + VERSION_SEPARATOR.length(), str.length());
            return new Feature(strName, strVersion);
        } else {
            return new Feature(str);
        }


    }


    public String getId() {
        return getName() + VERSION_SEPARATOR + getVersion();
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the version property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getVersion() {
        if (version == null) {
            return DEFAULT_VERSION;
        } else {
            return version;
        }
    }

    /**
     * Sets the value of the version property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @XmlAttribute
    public void setVersion(String value) {
        this.version = VersionCleaner.clean(value);
    }

    /**
     * Since version has a default value ("0.0.0"), returns
     * whether or not the version has been set.
     */
    public boolean hasVersion() {
        return this.version != null;
    }

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDescription(String value) {
        this.description = value;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the value of the resolver property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getResolver() {
        return resolver;
    }

    public String getInstall() {
        return install;
    }

    public void setInstall(String install) {
        this.install = install;
    }

    /**
     * Sets the value of the resolver property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setResolver(String value) {
        this.resolver = value;
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
     * Gets the value of the hidden property.
     */
    public boolean isHidden() {
        return hidden == null ? false : hidden;
    }

    /**
     * Sets the value of the hidden property.
     */
    public void setHidden(Boolean value) {
        this.hidden = value;
    }

    /**
     * Gets the value of the conditional property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the feature property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConditionals().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Conditional }
     */
    public List<Conditional> getConditional() {
        if (conditional == null) {
            conditional = new ArrayList<>();
        }
        return this.conditional;
    }

    public List<Capability> getCapabilities() {
        if (capability == null) {
            capability = new ArrayList<>();
        }
        return this.capability;
    }

    public List<Requirement> getRequirements() {
        if (requirement == null) {
            requirement = new ArrayList<>();
        }
        return this.requirement;
    }

    public Scoping getScoping() {
        return scoping;
    }

    public void setScoping(Scoping scoping) {
        this.scoping = scoping;
    }

    public String toString() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Feature feature = (Feature) o;
        if (name != null ? !name.equals(feature.name) : feature.name != null) {
            return false;
        }
        if (version != null ? !version.equals(feature.version) : feature.version != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @SuppressWarnings("rawtypes")
    protected void interpolation(Properties properties) {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String val = properties.getProperty(key);
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(val);
            while (matcher.find()) {
                String rep = System.getProperty(matcher.group(1));
                if (rep != null) {
                    val = val.replace(matcher.group(0), rep);
                    matcher.reset(val);
                }
            }
            properties.put(key, val);
        }
    }

}
