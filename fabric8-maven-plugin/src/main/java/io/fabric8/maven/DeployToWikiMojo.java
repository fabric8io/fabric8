/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.maven;

import java.io.Console;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import io.fabric8.maven.support.Apps;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.SettingsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deploys the generated App Zip file into the wiki
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.INSTALL)
public class DeployToWikiMojo extends AbstractFabric8Mojo {
    private static final transient Logger LOG = LoggerFactory.getLogger(DeployToWikiMojo.class);

    public static final String DEFAULT_CONSOLE_URL = "http://dockerhost:8484/hawtio/";

    @Parameter(defaultValue = "${settings}", readonly = true)
    Settings mavenSettings;

    @Parameter(defaultValue = "${user.home}/.m2/settings.xml")
    private File mavenSettingsFile;

    @Component
    SettingsWriter mavenSettingsWriter;

    @Component
    ArtifactDeployer deployer;

    @Component
    protected ArtifactResolver resolver;

    @Parameter(property = "localRepository", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    protected List remoteRepositories;

    /**
     * The server ID in ~/.m2/settings/xml used for the username and password to login to
     * the fabric8 console
     */
    @Parameter(property = "fabric8.serverId", defaultValue = "fabric8.console")
    private String serverId;

    /**
     * The URL for accessing jolokia on the fabric.
     */
    @Parameter(property = "fabric8.consoleUrl", defaultValue = "${env.FABRIC8_CONSOLE}", required = false)
    private String consoleUrl;

    /**
     * The git branch inside the fabric8 console to post the App Zip to
     */
    @Parameter(property = "fabric8.branch", defaultValue = "master")
    private String branch;

    /**
     * The path inside the fabric8 console to post the App Zip to
     */
    @Parameter(property = "fabric8.deployPath", defaultValue = "/")
    private String deployPath;

    @VisibleForTesting
    Server fabricServer;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isIgnoreProject()) return;

        try {
            boolean newUserAdded = false;

            fabricServer = mavenSettings.getServer(serverId);

            if (Strings.isNullOrBlank(consoleUrl)) {
                consoleUrl = DEFAULT_CONSOLE_URL;
            }

            // we may have username and password from consoleUrl
            String jolokiaUsername = null;
            String jolokiaPassword = null;
            try {
                URL url = new URL(consoleUrl);
                String s = url.getUserInfo();
                if (Strings.isNotBlank(s) && s.indexOf(':') > 0) {
                    int idx = s.indexOf(':');
                    jolokiaUsername = s.substring(0, idx);
                    jolokiaPassword = s.substring(idx + 1);
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Option consoleUrl is invalid due " + e.getMessage());
            }

            // jolokia url overrides username/password configured in maven settings
            if (jolokiaUsername != null) {
                if (fabricServer == null) {
                    fabricServer = new Server();
                }
                getLog().info("Using username: " + jolokiaUsername + " and password from provided consoleUrl option");
                fabricServer.setUsername(jolokiaUsername);
                fabricServer.setPassword(jolokiaPassword);
            }

            if (fabricServer == null) {
                boolean create = false;
                if (mavenSettings.isInteractiveMode() && mavenSettingsWriter != null) {
                    System.out.println("Maven settings file: " + mavenSettingsFile.getAbsolutePath());
                    System.out.println();
                    System.out.println();
                    System.out.println("There is no <server> section in your ~/.m2/settings.xml file for the server id: " + serverId);
                    System.out.println();
                    System.out.println("You can enter the username/password now and have the settings.xml updated or you can do this by hand if you prefer.");
                    System.out.println();
                    while (true) {
                        String value = readInput("Would you like to update the settings.xml file now? (y/n): ").toLowerCase();
                        if (value.startsWith("n")) {
                            System.out.println();
                            System.out.println();
                            break;
                        } else if (value.startsWith("y")) {
                            create = true;
                            break;
                        }
                    }
                    if (create) {
                        System.out.println("Please let us know the login details for this server: " + serverId);
                        System.out.println();
                        String userName = readInput("Username: ");
                        String password = readPassword("Password: ");
                        String password2 = readPassword("Repeat Password: ");
                        while (!password.equals(password2)) {
                            System.out.println("Passwords do not match, please try again.");
                            password = readPassword("Password: ");
                            password2 = readPassword("Repeat Password: ");
                        }
                        System.out.println();
                        fabricServer = new Server();
                        fabricServer.setId(serverId);
                        fabricServer.setUsername(userName);
                        fabricServer.setPassword(password);
                        mavenSettings.addServer(fabricServer);
                        if (mavenSettingsFile.exists()) {
                            int counter = 1;
                            while (true) {
                                File backupFile = new File(mavenSettingsFile.getAbsolutePath() + ".backup-" + counter++ + ".xml");
                                if (!backupFile.exists()) {
                                    System.out.println("Copied original: " + mavenSettingsFile.getAbsolutePath() + " to: " + backupFile.getAbsolutePath());
                                    Files.copy(mavenSettingsFile, backupFile);
                                    break;
                                }
                            }
                        }
                        Map<String, Object> config = new HashMap<String, Object>();
                        mavenSettingsWriter.write(mavenSettingsFile, config, mavenSettings);
                        System.out.println("Updated settings file: " + mavenSettingsFile.getAbsolutePath());
                        System.out.println();

                        newUserAdded = true;
                    }
                }
            }
            if (fabricServer == null) {
                String message = "No <server> element can be found in ~/.m2/settings.xml for the server <id>" + serverId + "</id> so we cannot connect to fabric8!\n\n" +
                        "Please add the following to your ~/.m2/settings.xml file (using the correct user/password values):\n\n" +
                        "<servers>\n" +
                        "  <server>\n" +
                        "    <id>" + serverId + "</id>\n" +
                        "    <username>admin</username>\n" +
                        "    <password>admin</password>\n" +
                        "  </server>\n" +
                        "</servers>\n";
                getLog().error(message);
                throw new MojoExecutionException(message);
            }

            if (!isIgnoreProject()) {
                uploadAppZip(newUserAdded);
            } else {
                getLog().info("Ignoring this project so not uploading the App Zip");
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected String readInput(String prompt) {
        Console console = System.console();
        System.out.print(prompt);
        return console.readLine();
    }

    protected String readPassword(String prompt) {
        Console console = System.console();
        System.out.print(prompt);
        char[] pw = console.readPassword();
        return new String(pw);
    }

    @SuppressWarnings("unchecked")
    protected void uploadAppZip(boolean newUserAdded) throws Exception {
        File file = getZipFile();
        if (!file.exists()) {
            getLog().error("No App Zip file at " + file.getAbsolutePath() + ". Did you execute the fabric8:zip goal?");
            return;
        }
        if (!file.isFile()) {
            getLog().error("Invalid App Zip file at " + file.getAbsolutePath() + ". This should be a file not a directory!");
            return;
        }
        String user = fabricServer.getUsername();
        String password = fabricServer.getPassword();
        if (Strings.isNullOrBlank(user)) {
            getLog().warn("No <username> value defined for the server " + serverId + " in your ~/.m2/settings.xml. Please add a value!");
        }
        if (Strings.isNullOrBlank(password)) {
            getLog().warn("No <password> value defined for the server " + serverId + " in your ~/.m2/settings.xml. Please add a value!");
        }

        Apps.postFileToGit(file, user, password, consoleUrl, branch, deployPath, LOG);
    }

}
