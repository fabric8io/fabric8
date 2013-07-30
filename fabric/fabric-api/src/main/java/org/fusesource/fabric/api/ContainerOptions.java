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
package org.fusesource.fabric.api;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContainerOptions implements Serializable {

    public static String BIND_ADDRESS = "bind.address";
    public static String PROFILES = "profiles";

    public static class Builder<B extends Builder> implements Cloneable {

        String bindAddress = "0.0.0.0";
        String resolver;
        String globalResolver;
        String manualIp;
        int minimumPort = 0;
        int maximumPort = 65535;
        Set<String> profiles = new LinkedHashSet<String>();

        public B fromSystemProperties() {
            this.bindAddress = System.getProperty(BIND_ADDRESS, "0.0.0.0");
            this.minimumPort = Integer.parseInt(System.getProperty("minimum.port", String.valueOf(minimumPort)));
            this.maximumPort = Integer.parseInt(System.getProperty("maximum.port", String.valueOf(maximumPort)));
            this.profiles(System.getProperty(PROFILES, ""));
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

        public ContainerOptions build() {
            return new ContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles);
        }

        public B clone() throws CloneNotSupportedException {
            return (B) super.clone();
        }
    }

    final String bindAddress;
    final String resolver;
    final String globalResolver;
    final String manualIp;
    final int minimumPort;
    final int maximumPort;
    final Set<String> profiles;

    public static Builder builder() {
        return new Builder();
    }

    ContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles) {
        this.bindAddress = bindAddress;
        this.resolver = resolver;
        this.globalResolver = globalResolver;
        this.manualIp = manualIp;
        this.minimumPort = minimumPort;
        this.maximumPort = maximumPort;
        this.profiles = profiles;
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
                '}';
    }
}
