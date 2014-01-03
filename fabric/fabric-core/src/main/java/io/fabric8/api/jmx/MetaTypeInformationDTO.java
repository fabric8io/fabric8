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

import org.osgi.service.metatype.MetaTypeInformation;

import java.util.Arrays;

/**
 */
public class MetaTypeInformationDTO {
    private long bundleId;
    private String[] factoryPids;
    private String[] pids;
    private String[] locales;

    public MetaTypeInformationDTO() {
    }

    public MetaTypeInformationDTO(MetaTypeInformation info) {
        this.bundleId = info.getBundle().getBundleId();
        this.factoryPids = info.getFactoryPids();
        this.pids = info.getPids();
        this.locales = info.getLocales();
    }

    @Override
    public String toString() {
        return "MetaTypeInformationDTO{" +
                "bundleId=" + bundleId +
                ", factoryPids=" + Arrays.toString(factoryPids) +
                ", pids=" + Arrays.toString(pids) +
                '}';
    }

    public long getBundleId() {
        return bundleId;
    }

    public void setBundleId(long bundleId) {
        this.bundleId = bundleId;
    }

    public String[] getFactoryPids() {
        return factoryPids;
    }

    public void setFactoryPids(String[] factoryPids) {
        this.factoryPids = factoryPids;
    }

    public String[] getPids() {
        return pids;
    }

    public void setPids(String[] pids) {
        this.pids = pids;
    }

    public String[] getLocales() {
        return locales;
    }

    public void setLocales(String[] locales) {
        this.locales = locales;
    }
}
