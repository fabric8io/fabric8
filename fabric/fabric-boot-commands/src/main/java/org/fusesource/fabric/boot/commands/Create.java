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
package org.fusesource.fabric.boot.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.utils.properties.Properties;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.boot.commands.support.EnsembleCommandSupport;
import org.fusesource.fabric.utils.Ports;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.utils.shell.ShellUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Command(name = "create", scope = "fabric", description = "Creates a new fabric ensemble (ZooKeeper ensemble) and imports fabric profiles", detailedDescription = "classpath:create.txt")
public class Create extends EnsembleCommandSupport implements org.fusesource.fabric.boot.commands.service.Create {

    @Option(name = "--clean", description = "Clean local zookeeper cluster and configurations")
    private boolean clean;
    @Option(name = "--no-import", description = "Disable the import of the sample registry data")
    private boolean noImport;
    @Option(name = "--import-dir", description = "Directory of files to import into the newly created ensemble")
    private String importDir = getDefaultImportDir();
    @Option(name = "-v", aliases = {"--verbose"}, description = "Flag to enable verbose output of files being imported")
    boolean verbose = false;
    @Option(name = "-g", aliases = {"--global-resolver"}, description = "The global resolver policy, which becomes the default resolver policy applied to all new containers created in this fabric. Possible values are: localip, localhostname, publicip, publichostname, manualip. Default is localhostname.")
    String globalResolver;
    @Option(name = "-r", aliases = {"--resolver"}, description = "The local resolver policy. Possible values are: localip, localhostname, publicip, publichostname, manualip. Default is localhostname.")
    String resolver;
    @Option(name = "-m", aliases = {"--manual-ip"}, description = "An address to use, when using the manualip resolver.")
    String manualIp;
    @Option(name = "-b", aliases = {"--bind-address"}, description = "The default bind address.")
    String bindAddress;
    @Option(name = "-n", aliases = "--non-managed", multiValued = false, description = "Flag to keep the container non managed")
    private boolean nonManaged;
    @Option(name = "-t", aliases = {"--time"}, description = "How long to wait (milliseconds) for the ensemble to start up before trying to import the default data")
    long ensembleStartupTime = 2000L;
    @Option(name = "-p", aliases = "--profile", multiValued = true, description = "Chooses the profile of the container.")
    private Set<String> profiles = null;
    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = Ports.MIN_PORT_NUMBER;
    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = Ports.MAX_PORT_NUMBER;
    @Option(name = "--zookeeper-password", multiValued = false, description = "The ensemble password to use (one will be generated if not given)")
    private String zookeeperPassword;
    @Option(name = "--generate-zookeeper-password", multiValued = false, description = "Flag to enable automatic generation of password")
    private boolean generateZookeeperPassword = false;
    @Option(name = "--new-user", multiValued = false, description = "The username of a new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUser;
    @Option(name = "--new-user-password", multiValued = false, description = "The password of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserPassword;
    @Option(name = "--new-user-role", multiValued = false, description = "The role of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserRole = "admin";

    @Argument(required = false, multiValued = true, description = "List of containers. Empty list assumes current container only.")
    private List<String> containers;

    private static final String ROLE_DELIMITER = ",";

    @Override
    protected Object doExecute() throws Exception {
        CreateEnsembleOptions.Builder builder = CreateEnsembleOptions.builder();

        if (containers == null || containers.isEmpty()) {
            containers = Arrays.asList(System.getProperty(SystemProperties.KARAF_NAME));
        }

        if (clean) {
            service.clean();
        }

        if (!noImport && importDir != null) {
            builder.autoImportEnabled(true);
            builder.importPath(importDir);
        }

        if (globalResolver != null) {
            builder.globalResolver(globalResolver);
            System.setProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY, globalResolver);
        }

        if (resolver != null) {
            builder.resolver(resolver);
            System.setProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY, resolver);
        }

        if (manualIp != null) {
            builder.manualIp(manualIp);
            System.setProperty(ZkDefs.MANUAL_IP, manualIp);
        }

        if (bindAddress != null) {
            if (!bindAddress.contains(":")) {
                builder.bindAddress(bindAddress);
                System.setProperty(ZkDefs.BIND_ADDRESS, bindAddress);
            } else {
                String[] parts = bindAddress.split(":");
                builder.bindAddress(parts[0]);
                builder.zooKeeperServerPort(Integer.parseInt(parts[1]));
                System.setProperty(ZkDefs.BIND_ADDRESS, parts[0]);
            }
        }

        if (profiles != null && profiles.size() > 0) {
            builder.profiles(profiles);
        }

        if (nonManaged) {
            builder.agentEnabled(false);
        } else {
            builder.agentEnabled(true);
        }

        builder.minimumPort(minimumPort);
        builder.minimumPort(maximumPort);
        System.setProperty(ZkDefs.MINIMUM_PORT, String.valueOf(minimumPort));
        System.setProperty(ZkDefs.MAXIMUM_PORT, String.valueOf(maximumPort));

        newUser = newUser != null ? newUser : ShellUtils.retrieveFabricUser(session);
        newUserPassword = newUserPassword != null ? newUserPassword : ShellUtils.retrieveFabricUserPassword(session);

        Properties userProps = new Properties(new File(System.getProperty("karaf.home") + "/etc/users.properties"));

        if (userProps.isEmpty()) {
            String[] credentials = promptForNewUser(newUser, newUserPassword);
            newUser = credentials[0];
            newUserPassword = credentials[1];
        } else if (newUser == null || newUserPassword == null) {
            newUser = (String) userProps.keySet().iterator().next();
            newUserPassword = (String) userProps.get(newUser);
            if (newUserPassword.contains(ROLE_DELIMITER)) {
                newUserPassword = newUserPassword.substring(0, newUserPassword.indexOf(ROLE_DELIMITER));
            }
        }

        if (newUser == null) {
            System.out.println("No user specified. Cannot create a new fabric ensemble.");
            return null;
        }
        
        StringBuilder sb = new StringBuilder();

        // session is unset when this is called from FMC
        if (session != null) {
            ShellUtils.storeFabricCredentials(session, newUser, newUserPassword);
        }

        if (generateZookeeperPassword) {
            //do nothing use the generated password.
        } else if (zookeeperPassword == null) {
            zookeeperPassword = System.getProperty(SystemProperties.ZOOKEEPER_PASSWORD, newUserPassword);
            builder.zookeeperPassword(zookeeperPassword);
        }

        CreateEnsembleOptions options = builder.users(userProps)
                                               .withUser(newUser, newUserPassword , newUserRole)
                                               .build();

        if (containers != null && !containers.isEmpty()) {
            service.createCluster(containers, options);
        }

        ShellUtils.storeZookeeperPassword(session, options.getZookeeperPassword());
        if (zookeeperPassword == null && !generateZookeeperPassword) {
            sb.append("Zookeeper password: (reusing users ").append(newUser).append(" password:").append(options.getZookeeperPassword()).append(")\n");
            sb.append("(You can use the --zookeeper-password / --generate-zookeeper-password option to specify one.)\n");
        }   else if (generateZookeeperPassword) {
            sb.append("Generated zookeeper password:").append(options.getZookeeperPassword());
        }  else {
            sb.append("Using specified zookeeper password:").append(options.getZookeeperPassword());
        }
        System.out.println(sb.toString());
        return null;
    }

    private static String getDefaultImportDir() {
        return System.getProperty("karaf.home", ".") + File.separatorChar + "fabric" + File.separatorChar + "import";
    }

    @Override
    public Object run() throws Exception {
        return doExecute();
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    @Override
    public boolean isClean() {
        return clean;
    }

    @Override
    public void setClean(boolean clean) {
        this.clean = clean;
    }

    @Override
    public boolean isNoImport() {
        return noImport;
    }

    @Override
    public void setNoImport(boolean noImport) {
        this.noImport = noImport;
    }

    @Override
    public String getImportDir() {
        return importDir;
    }

    @Override
    public void setImportDir(String importDir) {
        this.importDir = importDir;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public long getEnsembleStartupTime() {
        return ensembleStartupTime;
    }

    @Override
    public void setEnsembleStartupTime(long ensembleStartupTime) {
        this.ensembleStartupTime = ensembleStartupTime;
    }

    @Override
    public List<String> getContainers() {
        return containers;
    }

    @Override
    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    @Override
    public int getMinimumPort() {
        return minimumPort;
    }

    @Override
    public void setMinimumPort(int minimumPort) {
        this.minimumPort = minimumPort;
    }

    @Override
    public int getMaximumPort() {
        return maximumPort;
    }

    @Override
    public void setMaximumPort(int maximumPort) {
        this.maximumPort = maximumPort;
    }

    @Override
    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    @Override
    public void setZookeeperPassword(String zookeeperPassword) {
        this.zookeeperPassword = zookeeperPassword;
    }

    @Override
    public String getNewUser() {
        return newUser;
    }

    @Override
    public void setNewUser(String newUser) {
        this.newUser = newUser;
    }

    @Override
    public String getNewUserPassword() {
        return newUserPassword;
    }

    @Override
    public void setNewUserPassword(String newUserPassword) {
        this.newUserPassword = newUserPassword;
    }

    @Override
    public String getNewUserRole() {
        return newUserRole;
    }

    @Override
    public void setNewUserRole(String newUserRole) {
        this.newUserRole = newUserRole;
    }

    public Set<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(Set<String> profiles) {
        this.profiles = profiles;
    }

    public boolean isNonManaged() {
        return nonManaged;
    }

    public void setNonManaged(boolean nonManaged) {
        this.nonManaged = nonManaged;
    }

    public String getGlobalResolver() {
        return globalResolver;
    }

    public void setGlobalResolver(String globalResolver) {
        this.globalResolver = globalResolver;
    }

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getManualIp() {
        return manualIp;
    }

    public void setManualIp(String manualIp) {
        this.manualIp = manualIp;
    }

    public boolean isGenerateZookeeperPassword() {
        return generateZookeeperPassword;
    }

    public void setGenerateZookeeperPassword(boolean generateZookeeperPassword) {
        this.generateZookeeperPassword = generateZookeeperPassword;
    }
}
