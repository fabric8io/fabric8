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
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public class Content {

    protected List<Config> config;
    protected List<ConfigFile> configfile;
    protected List<Dependency> feature;
    protected List<BundleInfo> bundle;

    /**
     * Gets the value of the config property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the config property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfig().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Config }
     */
    public List<Config> getConfig() {
        if (config == null) {
            config = new ArrayList<Config>();
        }
        return this.config;
    }

    /**
     * Gets the value of the configfile property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the configfile property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfigfile().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link ConfigFile }
     */
    public List<ConfigFile> getConfigfile() {
        if (configfile == null) {
            configfile = new ArrayList<>();
        }
        return this.configfile;
    }

    /**
     * Gets the value of the feature property.
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
     *    getFeature().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Dependency }
     */
    public List<Dependency> getFeature() {
        if (feature == null) {
            feature = new ArrayList<>();
        }
        return this.feature;
    }

    /**
     * Gets the value of the bundle property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bundle property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBundle().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link BundleInfo }
     */
    public List<BundleInfo> getBundle() {
        if (bundle == null) {
            bundle = new ArrayList<>();
        }
        return this.bundle;
    }

    public List<Dependency> getDependencies() {
        return Collections.<Dependency>unmodifiableList(getFeature());
    }

    public List<BundleInfo> getBundles() {
        return Collections.<BundleInfo>unmodifiableList(getBundle());
    }

    public List<Config> getConfigurations() {
    	return Collections.<Config>unmodifiableList(getConfig());
    }

    public List<ConfigFile> getConfigurationFiles() {
        return Collections.<ConfigFile>unmodifiableList(getConfigfile());
    }

}
