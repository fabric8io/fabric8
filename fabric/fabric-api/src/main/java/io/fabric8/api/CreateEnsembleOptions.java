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

import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.api.jcip.Immutable;
import io.fabric8.api.jcip.ThreadSafe;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Immutable
@ThreadSafe
public class CreateEnsembleOptions extends ContainerOptions {

    public static final String AGENT_AUTOSTART = "agent.auto.start";
    public static final String ENSEMBLE_AUTOSTART = "ensemble.auto.start";
    public static final String PROFILES_AUTOIMPORT = "profiles.auto.import";
    public static final String PROFILES_AUTOIMPORT_PATH = "profiles.auto.import.path";
    public static final String DEFAULT_IMPORT_PATH = "fabric" + File.separatorChar + "import";
    public static final String ZOOKEEPER_PASSWORD = "zookeeper.password";
    public static String ZOOKEEPER_SERVER_PORT = "zookeeper.server.port";
    public static String ZOOKEEPER_SERVER_CONNECTION_PORT = "zookeeper.server.connection.port";
    public static final String ROLE_DELIMITER = ",";
    public static final long DEFAULT_MIGRATION_TIMEOUT = 120000L;
    public static final int DEFAULT_TICKTIME = 2000;
    public static final int DEFAULT_INIT_LIMIT = 10;
    public static final int DEFAULT_SYNC_LIMIT = 5;
    public static final String DEFAULT_DATA_DIR = "data/zookeeper";

    @JsonProperty
    final int zooKeeperServerPort;
    @JsonProperty
    final int zooKeeperServerConnectionPort;
    @JsonProperty
    final int zooKeeperServerTickTime;
    @JsonProperty
    final int zooKeeperServerInitLimit;
    @JsonProperty
    final int zooKeeperServerSyncLimit;
    @JsonProperty
    final String zooKeeperServerDataDir;
    @JsonProperty
    final String zookeeperPassword;
    @JsonProperty
    final boolean ensembleStart;
    @JsonProperty
    final boolean agentEnabled;
    @JsonProperty
    final boolean waitForProvision;
    @JsonProperty
    final long bootstrapTimeout;
    @JsonProperty
    final long migrationTimeout;
    @JsonProperty
    final boolean autoImportEnabled;
    @JsonProperty
    final String importPath;
    @JsonProperty
    final boolean clean;

    @JsonProperty
    final Map<String, String> users; // keep immutable

    public static Builder<? extends Builder<?>> builder() {
        return new Builder();
    }

    CreateEnsembleOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int zooKeeperServerPort, int zooKeeperServerConnectionPort,  int zooKeeperServerTickTime, int zooKeeperServerInitLimit, int zooKeeperServerSyncLimit, String zooKeeperServerDataDir, String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean waitForProvision, long bootstrapTimeout, long migrationTimeout, boolean autoImportEnabled, String importPath, Map<String, String> users, boolean clean) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties);
        this.zooKeeperServerPort = zooKeeperServerPort;
        this.zooKeeperServerConnectionPort = zooKeeperServerConnectionPort;
        this.zookeeperPassword = zookeeperPassword;
        this.ensembleStart = ensembleStart;
        this.agentEnabled = agentEnabled;
        this.waitForProvision = waitForProvision;
        this.bootstrapTimeout = bootstrapTimeout;
        this.migrationTimeout = migrationTimeout;
        this.autoImportEnabled = autoImportEnabled;
        this.importPath = importPath;
        this.zooKeeperServerTickTime = zooKeeperServerTickTime;
        this.zooKeeperServerInitLimit = zooKeeperServerInitLimit;
        this.zooKeeperServerSyncLimit = zooKeeperServerSyncLimit;
        this.zooKeeperServerDataDir = zooKeeperServerDataDir;
        this.users = Collections.unmodifiableMap(new HashMap<String, String>(users));
        this.clean = clean;
    }

    public int getZooKeeperServerPort() {
        return zooKeeperServerPort;
    }

    public int getZooKeeperServerConnectionPort() {
        return zooKeeperServerConnectionPort;
    }

    public int getZooKeeperServerTickTime() {
        return zooKeeperServerTickTime;
    }

    public int getZooKeeperServerInitLimit() {
        return zooKeeperServerInitLimit;
    }

    public int getZooKeeperServerSyncLimit() {
        return zooKeeperServerSyncLimit;
    }

    public String getZooKeeperServerDataDir() {
        return zooKeeperServerDataDir;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    public boolean isEnsembleStart() {
        return ensembleStart;
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

    public boolean isWaitForProvision() {
        return waitForProvision;
    }

    public long getBootstrapTimeout() {
        return bootstrapTimeout;
    }

    public long getMigrationTimeout() {
        return migrationTimeout;
    }

    public boolean isClean() {
        return clean;
    }

    @Override
    public String toString() {
        return super.toString() + " CreateEnsembleOptions{" +
                "zooKeeperServerPort=" + zooKeeperServerPort +
                ", zookeeperPassword='" + zookeeperPassword + '\'' +
                ", agentEnabled=" + agentEnabled +
                ", autoImportEnabled=" + autoImportEnabled +
                ", importPath='" + importPath + '\'' +
                ", users=" + users +
                '}';
    }

    public static class Builder<B extends Builder<?>> extends ContainerOptions.Builder<B> {

        @JsonProperty
        int zooKeeperServerPort = 2181;
        @JsonProperty
        int zooKeeperServerConnectionPort = 2181;
        @JsonProperty
        int zooKeeperServerTickTime = DEFAULT_TICKTIME;
        @JsonProperty
        int zooKeeperServerInitLimit = DEFAULT_INIT_LIMIT;
        @JsonProperty
        int zooKeeperServerSyncLimit = DEFAULT_SYNC_LIMIT;
        @JsonProperty
        String zooKeeperServerDataDir = DEFAULT_DATA_DIR;
        @JsonProperty
        String zookeeperPassword = generatePassword();
        @JsonProperty
        boolean ensembleStart = false;
        @JsonProperty
        boolean agentEnabled = true;
        @JsonProperty
        boolean waitForProvision = true;
        @JsonProperty
        long bootstrapTimeout = 120000L;
        @JsonProperty
        long migrationTimeout = DEFAULT_MIGRATION_TIMEOUT;
        @JsonProperty
        boolean autoImportEnabled = true;
        @JsonProperty
        String importPath = DEFAULT_IMPORT_PATH;
        @JsonProperty
        Map<String, String> users = new HashMap<String, String>();
        @JsonProperty
        boolean clean;

        @Override
        public B fromRuntimeProperties(RuntimeProperties sysprops) {
            super.fromRuntimeProperties(sysprops);
            this.ensembleStart = Boolean.parseBoolean(sysprops.getProperty(ENSEMBLE_AUTOSTART, "false"));
            this.agentEnabled = Boolean.parseBoolean(sysprops.getProperty(AGENT_AUTOSTART, "false"));
            this.zookeeperPassword =  sysprops.getProperty(ZOOKEEPER_PASSWORD, generatePassword());
            this.zooKeeperServerPort = Integer.parseInt(sysprops.getProperty(ZOOKEEPER_SERVER_PORT, "2181"));
            this.zooKeeperServerConnectionPort = Integer.parseInt(sysprops.getProperty(ZOOKEEPER_SERVER_CONNECTION_PORT, "2181"));
            this.importPath = sysprops.getProperty(PROFILES_AUTOIMPORT_PATH, DEFAULT_IMPORT_PATH);
            this.autoImportEnabled = Boolean.parseBoolean(sysprops.getProperty(PROFILES_AUTOIMPORT, "true"));
            return (B) this;
        }

        public B zooKeeperServerPort(int zooKeeperServerPort) {
            this.zooKeeperServerPort = zooKeeperServerPort;
            return (B) this;
        }

        public B zooKeeperServerPort(Integer zooKeeperServerPort) {
            this.zooKeeperServerPort = zooKeeperServerPort;
            return (B) this;
        }

        public B zooKeeperServerPort(Long zooKeeperServerPort) {
            this.zooKeeperServerConnectionPort = zooKeeperServerPort.intValue();
            return (B) this;
        }

        public B zooKeeperServerConnectionPort(int zooKeeperServerConnectionPort) {
            this.zooKeeperServerConnectionPort = zooKeeperServerConnectionPort;
            return (B) this;
        }

        public B zooKeeperServerConnectionPort(Integer zooKeeperServerConnectionPort) {
            this.zooKeeperServerConnectionPort = zooKeeperServerConnectionPort;
            return (B) this;
        }

        public B zooKeeperServerConnectionPort(Long zooKeeperServerConnectionPort) {
            this.zooKeeperServerConnectionPort = zooKeeperServerConnectionPort.intValue();
            return (B) this;
        }

        public B zooKeeperServerTickTime(int zooKeeperServerTickTime) {
            this.zooKeeperServerTickTime = zooKeeperServerTickTime;
            return (B) this;
        }

        public B zooKeeperServerInitLimit(int zooKeeperServerInitLimit) {
            this.zooKeeperServerInitLimit = zooKeeperServerInitLimit;
            return (B) this;
        }

        public B zooKeeperServerSyncLimit(int zooKeeperServerSyncLimit) {
            this.zooKeeperServerSyncLimit = zooKeeperServerSyncLimit;
            return (B) this;
        }


        public B zooKeeperServerDataDir(String zooKeeperServerDataDir) {
            this.zooKeeperServerDataDir = zooKeeperServerDataDir;
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

        public B ensembleStart(boolean ensembleStart) {
            this.ensembleStart = ensembleStart;
            return (B) this;
        }

        public B agentEnabled(boolean agentEnabled) {
            this.agentEnabled = agentEnabled;
            return (B) this;
        }

        public B agentEnabled(Boolean agentEnabled) {
            this.agentEnabled = agentEnabled;
            return (B) this;
        }

        public B autoImportEnabled(boolean autoImportEnabled) {
            this.autoImportEnabled = autoImportEnabled;
            return (B) this;
        }

        public B autoImportEnabled(Boolean autoImportEnabled) {
            this.autoImportEnabled = autoImportEnabled;
            return (B) this;
        }

        public B migrationTimeout(final long migrationTimeout) {
            this.migrationTimeout = migrationTimeout;
            return (B) this;
        }

        public B bootstrapTimeout(final long provisionTimeout) {
            this.bootstrapTimeout = provisionTimeout;
            return (B) this;
        }

        public B waitForProvision(final boolean waitForProvision) {
            this.waitForProvision = waitForProvision;
            return (B) this;
        }

        public B importPath(final String importPath) {
            this.importPath = importPath;
            return (B) this;
        }

        public B clean(boolean clean) {
            this.clean = clean;
            return (B) this;
        }

        public void setZooKeeperServerPort(int zooKeeperServerPort) {
            this.zooKeeperServerPort = zooKeeperServerPort;
        }

        public void setZookeeperPassword(String zookeeperPassword) {
            this.zookeeperPassword = zookeeperPassword;
        }

        public void setEnsembleStart(boolean ensembleStart) {
            this.ensembleStart = ensembleStart;
        }

        public void setAgentEnabled(boolean agentEnabled) {
            this.agentEnabled = agentEnabled;
        }

        public void setAutoImportEnabled(boolean autoImportEnabled) {
            this.autoImportEnabled = autoImportEnabled;
        }

        public void setImportPath(String importPath) {
            this.importPath = importPath;
        }

        public void setUsers(Map<String, String> users) {
            this.users = users;
        }

        public void setWaitForProvision(boolean waitForProvision) {
            this.waitForProvision = waitForProvision;
        }

        public void setBootstrapTimeout(long bootstrapTimeout) {
            this.bootstrapTimeout = bootstrapTimeout;
        }

        public int getZooKeeperServerPort() {
            return zooKeeperServerPort;
        }

        public int getZooKeeperServerConnectionPort() {
            return zooKeeperServerConnectionPort;
        }

        public String getZookeeperPassword() {
            return zookeeperPassword;
        }

        public boolean isEnsembleStart() {
            return ensembleStart;
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

        public boolean isWaitForProvision() {
            return waitForProvision;
        }

        public long getBootstrapTimeout() {
            return bootstrapTimeout;
        }

        public boolean isClean() {
            return clean;
        }

        /**
         * Generate a random String that can be used as a Zookeeper password.
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

        @Override
        public CreateEnsembleOptions build() {
            return new CreateEnsembleOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zooKeeperServerTickTime, zooKeeperServerInitLimit, zooKeeperServerSyncLimit, zooKeeperServerDataDir, zookeeperPassword, ensembleStart, agentEnabled, waitForProvision, bootstrapTimeout, migrationTimeout, autoImportEnabled, importPath, users, clean);
        }
    }

    /**
     * Generate a random String that can be used as a Zookeeper password.
     *
     * @return
     */
    public static String generatePassword() {
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
}
