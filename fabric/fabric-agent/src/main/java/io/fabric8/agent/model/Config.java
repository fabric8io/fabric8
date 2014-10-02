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

import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Configuration entries which should be created during feature installation. This
 * configuration may be used with OSGi Configuration Admin.
 * <p/>
 * <p/>
 * <p>Java class for config complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="config">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "config", propOrder = {"value"})
public class Config {

    @XmlValue
    protected String value;
    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute(required = false)
	private Boolean append = false;

    /**
     * Gets the value of the value property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setValue(String value) {
        this.value = value;
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
	 * @return the append
	 */
	public boolean isAppend() {
		return append;
	}

	/**
	 * @param append the append to set
	 */
	public void setAppend(boolean append) {
		this.append = append;
	}

	public Properties getProperties() {
		StringReader propStream = new StringReader(getValue());
		Properties props = new Properties();
		try {
			props.load(propStream);
		} catch (IOException e) {
			// ignore??
		}
		interpolation(props);
		return props;
	}

	@SuppressWarnings("rawtypes")
	private void interpolation(Properties properties) {
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
