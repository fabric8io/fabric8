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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEnsembleOptions extends ContainerOptions {

    public static String ZOOKEEPER_SERVER_PORT = "zookeeper.server.port";
    public static final String ROLE_DELIMITER = ",";

    public static class Builder<B extends Builder> extends ContainerOptions.Builder<B> {

        int zooKeeperServerPort = 2181;
        String zookeeperPassword = generatePassword();
        boolean agentEnabled = true;
        boolean autoImportEnabled = true;
        String importPath = System.getProperty("karaf.home", ".") + File.separatorChar + "fabric" + File.separatorChar + "import";
        Map<String, String> users = new HashMap<String, String>();

        @Override
        public B fromSystemProperties() {
            super.fromSystemProperties();
            this.zooKeeperServerPort = Integer.parseInt(System.getProperty(ZOOKEEPER_SERVER_PORT, "2181"));
            return (B) this;
        }

        public B getZooKeeperServerPort(final int getZooKeeperServerPort) {
            this.zooKeeperServerPort = getZooKeeperServerPort;
            return (B) this;
        }

        public B zookeeperPassword(final String zookeeperPassword) {
            this.zookeeperPassword = zookeeperPassword;
            return (B) this;
        }

        public B users(final Map<String, String> users) {
            this.users = users;
            return (B) this;
        }

        public B withUser(String user, String password, String role) {
            this.users.put(user, password + ROLE_DELIMITER + role);
            return (B) this;
        }


        public B agentEnabled(final boolean agentEnabled) {
            this.agentEnabled = agentEnabled;
            return (B) this;
        }

        public B autoImportEnabled(final boolean autoImportEnabled) {
            this.autoImportEnabled = autoImportEnabled;
            return (B) this;
        }


        public B importPath(final String importPath) {
            this.importPath = importPath;
            return (B) this;
        }

        /**
         * Generate a random String that can be used as a Zookeeper password.
         *
         * @return
         */
        private static String generatePassword() {
            StringBuilder password = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                long l = Math.round(Math.floor(Math.random() * (26 * 2 + 10)));
                if (l < 10) {
                    password.append((char) ('0' + l));
                } else if (l < 36) {
                    password.append((char) ('A' + l - 10));
                } else {
                    password.append((char) ('a' + l - 36));
                }
            }
            return password.toString();
        }


        public CreateEnsembleOptions build() {
            return new CreateEnsembleOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users);
        }
    }

    final int zooKeeperServerPort;
    final String zookeeperPassword;
    final boolean agentEnabled;
    final boolean autoImportEnabled;
    final String importPath;
    final Map<String, String> users;

    public static Builder<? extends Builder> builder() {
        return new Builder<Builder>();
    }

    CreateEnsembleOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, List<String> profiles, int zooKeeperServerPort, String zookeeperPassword, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles);
        this.zooKeeperServerPort = zooKeeperServerPort;
        this.zookeeperPassword = zookeeperPassword;
        this.agentEnabled = agentEnabled;
        this.autoImportEnabled = autoImportEnabled;
        this.importPath = importPath;
        this.users = users;
    }

    public int getZooKeeperServerPort() {
        return zooKeeperServerPort;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    public boolean isAgentEnabled() {
        return agentEnabled;
    }

    public boolean isAutoImportEnabled() {
        return autoImportEnabled;
    }

    public String getImportPath() {
        return importPath;
    }

    public Map<String, String> getUsers() {
        return users;
    }
}
