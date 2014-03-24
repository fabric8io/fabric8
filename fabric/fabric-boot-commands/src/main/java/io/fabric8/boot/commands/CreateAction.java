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
package io.fabric8.boot.commands;

import io.fabric8.api.ContainerOptions;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DefaultRuntimeProperties;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ServiceProxy;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.Ports;
import io.fabric8.utils.SystemProperties;
import io.fabric8.utils.shell.ShellUtils;
import io.fabric8.zookeeper.ZkDefs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.shell.console.AbstractAction;
import org.osgi.framework.BundleContext;

import com.google.common.base.Strings;

@Command(name = "create", scope = "fabric", description = "Creates a new fabric ensemble (ZooKeeper ensemble) and imports fabric profiles", detailedDescription = "classpath:create.txt")
final class CreateAction extends AbstractAction {

    private static final String GIT_REMOTE_URL = "gitRemoteUrl";
    private static final String GIT_REMOTE_USER = "gitRemoteUser";
    private static final String GIT_REMOTE_PASSWORD = "gitRemotePassword";

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
    @Option(name = "--wait-for-provisioning", multiValued = false, description = "Flag to wait for the initial container provisioning")
    private boolean waitForProvisioning=false;
    @Option(name = "--bootstrap-timeout", multiValued = false, description = "How long to wait (milliseconds) for the initial fabric bootstrap")
    private long bootstrapTimeout =120000L;
    @Option(name = "-t", aliases = {"--time"}, description = "How long to wait (milliseconds) for the ensemble to start up before trying to import the default data")
    long ensembleStartupTime = 2000L;
    @Option(name = "-p", aliases = "--profile", multiValued = true, description = "Chooses the profile of the container.")
    private Set<String> profiles = null;
    @Option(name = "-v", aliases = "--version", multiValued = false, description = "Chooses the default version.")
    private String version = ContainerOptions.DEFAULT_VERSION;
    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = Ports.MIN_PORT_NUMBER;
    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = Ports.MAX_PORT_NUMBER;
    @Option(name = "--zookeeper-ticktime", multiValued = false, description = "The length of a single tick, which is the basic time unit used by ZooKeeper, as measured in milliseconds. It is used to regulate heartbeats, and timeouts. For example, the minimum session timeout will be two ticks")
    private int zooKeeperTickTime = CreateEnsembleOptions.DEFAULT_TICKTIME;
    @Option(name = "--zookeeper-init-limit", multiValued = false, description = "The amount of time, in ticks (see tickTime), to allow followers to connect and sync to a leader")
    private int zooKeeperInitLimit = CreateEnsembleOptions.DEFAULT_INIT_LIMIT;
    @Option(name = "--zookeeper-sync-limit", multiValued = false, description = "The amount of time, in ticks (see tickTime), to allow followers to sync with ZooKeeper")
    private int zooKeeperSyncLimit = CreateEnsembleOptions.DEFAULT_SYNC_LIMIT;
    @Option(name = "--zookeeper-data-dir", multiValued = false, description = "The location where ZooKeeper will store the in-memory database snapshots and, unless specified otherwise, the transaction log of updates to the database.")
    private String zooKeeperDataDir = CreateEnsembleOptions.DEFAULT_DATA_DIR;
    @Option(name = "--zookeeper-password", multiValued = false, description = "The ensemble password to use (one will be generated if not given)")
    private String zookeeperPassword;
    @Option(name = "--generate-zookeeper-password", multiValued = false, description = "Flag to enable automatic generation of password")
    private boolean generateZookeeperPassword = false;
    @Option(name = "--new-user", multiValued = false, description = "The username of a new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUser;
    @Option(name = "--new-user-password", multiValued = false, description = "The password of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserPassword;
    @Option(name = "--external-git-url", multiValued = false, description = "Specify an external git url.")
    private String externalGitUrl;
    @Option(name = "--external-git-user", multiValued = false, description = "Specify an external git user.")
    private String externalGitUser;
    @Option(name = "--external-git-passowrd", multiValued = false, description = "Specify an external git password.")
    private String externalGitPassword;
    @Option(name = "--new-user-role", multiValued = false, description = "The role of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserRole = "admin";

    @Argument(required = false, multiValued = true, description = "List of containers. Empty list assumes current container only.")
    private List<String> containers;

    private static final String ROLE_DELIMITER = ",";

    private final BundleContext bundleContext;
    private final ZooKeeperClusterBootstrap bootstrap;
    private final RuntimeProperties runtimeProperties;

    CreateAction(BundleContext bundleContext, ZooKeeperClusterBootstrap bootstrap, RuntimeProperties runtimeProperties) {
        this.bundleContext = bundleContext;
        this.bootstrap = bootstrap;
        this.runtimeProperties = runtimeProperties;
    }

    protected Object doExecute() throws Exception {

        String karafName = runtimeProperties.getProperty(SystemProperties.KARAF_NAME);
        CreateEnsembleOptions.Builder builder = CreateEnsembleOptions.builder()
                .zooKeeperServerTickTime(zooKeeperTickTime)
                .zooKeeperServerInitLimit(zooKeeperInitLimit)
                .zooKeeperServerSyncLimit(zooKeeperSyncLimit)
                .zooKeeperServerDataDir(zooKeeperDataDir)
                .fromRuntimeProperties(new DefaultRuntimeProperties())
                .bootstrapTimeout(bootstrapTimeout)
                .waitForProvision(waitForProvisioning)
                .clean(clean);

        builder.version(version);

        if (containers == null || containers.isEmpty()) {
            containers = Arrays.asList(karafName);
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

        //Configure External Git Repository.
        if (externalGitUrl != null) {
            builder.dataStoreProperty(GIT_REMOTE_URL, externalGitUrl);
        }
        if (externalGitUser != null) {
            builder.dataStoreProperty(GIT_REMOTE_USER, externalGitUser);
        }
        if (externalGitPassword != null) {
            builder.dataStoreProperty(GIT_REMOTE_PASSWORD, externalGitPassword);
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
            newUser = "" + userProps.keySet().iterator().next();
            newUserPassword = "" + userProps.get(newUser);
            if (newUserPassword.contains(ROLE_DELIMITER)) {
                newUserPassword = newUserPassword.substring(0, newUserPassword.indexOf(ROLE_DELIMITER));
            }
        }

        if (Strings.isNullOrEmpty(newUser)) {
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
            zookeeperPassword = PasswordEncoder.decode(System.getProperty(CreateEnsembleOptions.ZOOKEEPER_PASSWORD, PasswordEncoder.encode(newUserPassword)));
            builder.zookeeperPassword(zookeeperPassword);
        } else {
            builder.zookeeperPassword(zookeeperPassword);
        }

        CreateEnsembleOptions options = builder.users(userProps)
                                               .withUser(newUser, newUserPassword , newUserRole)
                                               .build();

        if (containers.size() == 1 && containers.contains(karafName)) {
            bootstrap.create(options);
        } else {
            ServiceProxy<ZooKeeperClusterService> serviceProxy = ServiceProxy.createServiceProxy(bundleContext, ZooKeeperClusterService.class);
            try {
                serviceProxy.getService().createCluster(containers, options);
            } finally {
                serviceProxy.close();
            }
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
        if (!nonManaged && !waitForProvisioning) {
            System.out.println("It may take a couple of seconds for the container to provision...");
            System.out.println("You can use the --wait-for-provisioning option, if you want this command to block until the container is provisioned.");
        }
        return null;
    }

    private String[] promptForNewUser(String user, String password) throws IOException {
        String[] response = new String[2];
        // If the username was not configured via cli, then prompt the user for the values
        if (user == null || password == null) {
            System.out.println("No user found in etc/users.properties or specified as an option. Please specify one ...");
        }
        while (user == null || user.isEmpty()) {
            user = ShellUtils.readLine(session, "New user name: ", false);
            if (user == null) {
                break;
            }
        }

        if (user != null && password == null) {
            String password1 = null;
            String password2 = null;
            while (password1 == null || !password1.equals(password2)) {
                password1 = ShellUtils.readLine(session, "Password for " + user + ": ", true);
                password2 = ShellUtils.readLine(session, "Verify password for " + user + ":", true);

                if (password1 == null || password2 == null) {
                    break;
                }

                if (password1 != null && password1.equals(password2)) {
                    password = password1;
                } else {
                    System.out.println("Passwords did not match. Please try again!");
                }
            }
        }
        response[0] = user;
        response[1] = password;
        return response;
    }

    private static String getDefaultImportDir() {
        return System.getProperty("karaf.home", ".") + File.separatorChar + "fabric" + File.separatorChar + "import";
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public boolean isNoImport() {
        return noImport;
    }

    public void setNoImport(boolean noImport) {
        this.noImport = noImport;
    }

    public String getImportDir() {
        return importDir;
    }

    public void setImportDir(String importDir) {
        this.importDir = importDir;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public long getEnsembleStartupTime() {
        return ensembleStartupTime;
    }

    public void setEnsembleStartupTime(long ensembleStartupTime) {
        this.ensembleStartupTime = ensembleStartupTime;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public int getMinimumPort() {
        return minimumPort;
    }

    public void setMinimumPort(int minimumPort) {
        this.minimumPort = minimumPort;
    }

    public int getMaximumPort() {
        return maximumPort;
    }

    public void setMaximumPort(int maximumPort) {
        this.maximumPort = maximumPort;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    public void setZookeeperPassword(String zookeeperPassword) {
        this.zookeeperPassword = zookeeperPassword;
    }

    public String getNewUser() {
        return newUser;
    }

    public void setNewUser(String newUser) {
        this.newUser = newUser;
    }

    public String getNewUserPassword() {
        return newUserPassword;
    }

    public void setNewUserPassword(String newUserPassword) {
        this.newUserPassword = newUserPassword;
    }

    public String getNewUserRole() {
        return newUserRole;
    }

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
