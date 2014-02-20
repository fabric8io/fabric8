/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.api.jcip.Immutable;
import io.fabric8.api.jcip.ThreadSafe;

@Immutable
@ThreadSafe
public class ContainerOptions implements Serializable {

    public static String BIND_ADDRESS = "bind.address";
    public static String PROFILES = "profiles";
    public static String VERSION = "version";
    public static String DEFAULT_VERSION = "1.0";

    final String bindAddress;
    final String resolver;
    final String globalResolver;
    final String manualIp;
    final int minimumPort;
    final int maximumPort;
    final String version;

    // keep these immutable
    final Set<String> profiles;
    final Map<String, String> dataStoreProperties;

    ContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties) {
        this.bindAddress = bindAddress;
        this.resolver = resolver;
        this.globalResolver = globalResolver;
        this.manualIp = manualIp;
        this.minimumPort = minimumPort;
        this.maximumPort = maximumPort;
        this.version = version;
        this.profiles = Collections.unmodifiableSet(new HashSet<String>(profiles));;
        this.dataStoreProperties = Collections.unmodifiableMap(new HashMap<String, String>(dataStoreProperties != null ? dataStoreProperties : Collections.<String, String>emptyMap()));
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public String getResolver() {
        return resolver;
    }

    public String getGlobalResolver() {
        return globalResolver;
    }

    public String getManualIp() {
        return manualIp;
    }

    public int getMinimumPort() {
        return minimumPort;
    }

    public int getMaximumPort() {
        return maximumPort;
    }

    public Set<String> getProfiles() {
        return profiles;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getDataStoreProperties() {
        return dataStoreProperties;
    }

    @Override
    public String toString() {
        return "ContainerOptions{" +
                "bindAddress='" + bindAddress + '\'' +
                ", resolver='" + resolver + '\'' +
                ", globalResolver='" + globalResolver + '\'' +
                ", manualIp='" + manualIp + '\'' +
                ", minimumPort=" + minimumPort +
                ", maximumPort=" + maximumPort +
                ", profiles=" + profiles +
                ", dataStoreProperties=" + dataStoreProperties +
                '}';
    }

    public static class Builder<B extends Builder<?>> implements Cloneable {

        @JsonProperty
        String bindAddress = "0.0.0.0";
        @JsonProperty
        String resolver;
        @JsonProperty
        String globalResolver;
        @JsonProperty
        String manualIp;
        @JsonProperty
        int minimumPort = 0;
        @JsonProperty
        int maximumPort = 65535;
        @JsonProperty
        Set<String> profiles = new LinkedHashSet<String>();
        @JsonProperty
        String version = ContainerOptions.DEFAULT_VERSION;
        @JsonProperty
        Map<String, String> dataStoreProperties = new HashMap<String, String>();


        public Builder() {

        }

        public B fromRuntimeProperties(RuntimeProperties sysprops) {
            this.bindAddress = sysprops.getProperty(BIND_ADDRESS, "0.0.0.0");
            this.minimumPort = Integer.parseInt(sysprops.getProperty("minimum.port", String.valueOf(minimumPort)));
            this.maximumPort = Integer.parseInt(sysprops.getProperty("maximum.port", String.valueOf(maximumPort)));
            this.profiles(sysprops.getProperty(PROFILES, ""));
            this.version(sysprops.getProperty(VERSION, DEFAULT_VERSION));
            return (B) this;
        }

        public B bindAddress(final String bindAddress) {
            this.bindAddress = bindAddress;
            return (B) this;
        }

        public B resolver(final String resolver) {
            this.resolver = resolver;
            return (B) this;
        }

        public B globalResolver(final String globalResolver) {
            this.globalResolver = globalResolver;
            return (B) this;
        }

        public B manualIp(final String manualIp) {
            this.manualIp = manualIp;
            return (B) this;
        }

        public B minimumPort(int minimumPort) {
            this.minimumPort = minimumPort;
            return (B) this;
        }

        public B minimumPort(Integer minimumPort) {
            this.minimumPort = minimumPort;
            return (B) this;
        }

        public B minimumPort(Long minimumPort) {
            this.minimumPort = minimumPort.intValue();
            return (B) this;
        }

        public B maximumPort(int maximumPort) {
            this.maximumPort = maximumPort;
            return (B) this;
        }

        public B maximumPort(Integer maximumPort) {
            this.maximumPort = maximumPort;
            return (B) this;
        }

        public B maximumPort(Long maximumPort) {
            this.maximumPort = maximumPort.intValue();
            return (B) this;
        }

        public B profiles(final Set<String> profiles) {
            this.profiles = profiles;
            return (B) this;
        }

        public B profiles(final List<String> profiles) {
            this.profiles = new LinkedHashSet<String>(profiles);
            return (B) this;
        }

        public B profiles(final String... profiles) {
            this.profiles.clear();
            for (String p : profiles) {
                this.profiles.add(p.trim());
            }
            return (B) this;
        }

        public B profiles(final String profiles) {
            this.profiles.clear();
            String[] allProfiles = profiles.trim().split(" +");
            for (String p : allProfiles) {
                this.profiles.add(p.trim());
            }
            return (B) this;
        }

        public B version(final String version) {
            this.version = version;
            return (B) this;
        }

        public B dataStoreProperties(Map<String, String> dataStoreType) {
            this.dataStoreProperties = dataStoreType;
            return (B) this;
        }

        public B dataStoreProperty(String key, String value) {
            this.dataStoreProperties.put(key, value);
            return (B) this;
        }

        public B dataStoreType(String type) {
            this.dataStoreProperties.put(DataStore.DATASTORE_TYPE_PROPERTY, type);
            return (B) this;
        }

        public void setBindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
        }

        public void setResolver(String resolver) {
            this.resolver = resolver;
        }

        public void setGlobalResolver(String globalResolver) {
            this.globalResolver = globalResolver;
        }

        public void setManualIp(String manualIp) {
            this.manualIp = manualIp;
        }

        public void setMinimumPort(int minimumPort) {
            this.minimumPort = minimumPort;
        }

        public void setMaximumPort(int maximumPort) {
            this.maximumPort = maximumPort;
        }

        public void setProfiles(Set<String> profiles) {
            this.profiles = profiles;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getBindAddress() {
            return bindAddress;
        }

        public String getResolver() {
            return resolver;
        }

        public String getGlobalResolver() {
            return globalResolver;
        }

        public String getManualIp() {
            return manualIp;
        }

        public int getMinimumPort() {
            return minimumPort;
        }

        public int getMaximumPort() {
            return maximumPort;
        }

        public Set<String> getProfiles() {
            return profiles;
        }

        public String getVersion() {
            return version;
        }

        public Map<String, String> getDataStoreProperties() {
            return dataStoreProperties;
        }

        public void setDataStoreProperties(Map<String, String> dataStoreProperties) {
            this.dataStoreProperties = dataStoreProperties;
        }

        public ContainerOptions build() {
            return new ContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties);
        }

        public B clone() throws CloneNotSupportedException {
            return (B) super.clone();
        }
    }
}
