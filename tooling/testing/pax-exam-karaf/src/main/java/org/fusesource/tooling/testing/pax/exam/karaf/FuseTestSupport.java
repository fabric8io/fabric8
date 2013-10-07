/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.tooling.testing.pax.exam.karaf;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.*;

import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public class FuseTestSupport {

    public static final Long DEFAULT_TIMEOUT = 10000L;
    public static final Long SYSTEM_TIMEOUT = 30000L;
    public static final Long DEFAULT_WAIT = 10000L;
    public static final Long PROVISION_TIMEOUT = 300000L;
    public static final Long COMMAND_TIMEOUT = 30000L;

    protected ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    protected BundleContext bundleContext;

      protected Bundle installBundle(String groupId, String artifactId) throws Exception {
        MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId);
        return bundleContext.installBundle(mvnUrl.getURL());
    }


    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        for (Bundle b : bundleContext.getBundles()) {
            System.err.println("Bundle: " + b.getSymbolicName());
        }
        throw new RuntimeException("Bundle " + symbolicName + " does not exist");
    }


    /**
     * Make available system properties that are configured for the test, to the test container.
     * <p>Note:</p> If not obvious the container runs in in forked mode and thus system properties passed
     * form command line or surefire plugin are not available to the container without an approach like this.
     * @param propertyName
     * @return
     */
    public static Option copySystemProperty(String propertyName) {
        return editConfigurationFilePut("etc/system.properties", propertyName, System.getProperty(propertyName) != null ? System.getProperty(propertyName) : "");
    }

    /**
     * Create an provisioning option for the specified maven artifact
     * (groupId and artifactId), using the version found in the list
     * of dependencies of this maven project.
     *
     * @param groupId    the groupId of the maven bundle
     * @param artifactId the artifactId of the maven bundle
     * @return the provisioning option for the given bundle
     */
    protected static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle(groupId, artifactId).versionAsInProject();
    }

    /**
     * Create an provisioning option for the specified maven artifact
     * (groupId and artifactId), using the version found in the list
     * of dependencies of this maven project.
     *
     * @param groupId    the groupId of the maven bundle
     * @param artifactId the artifactId of the maven bundle
     * @param version    the version of the maven bundle
     * @return the provisioning option for the given bundle
     */
    protected static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId, String version) {
        return CoreOptions.mavenBundle(groupId, artifactId).version(version);
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     * @param command
     * @return
     */
    protected String executeCommand(final String command) {
       return executeCommand(command,COMMAND_TIMEOUT,false);
    }

     /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     * @param command The command to execute.
     * @param timeout The amount of time in millis to wait for the command to execute.
     * @param silent  Specifies if the command should be displayed in the screen.
     * @return
     */
    protected String executeCommand(final String command, final Long timeout, final Boolean silent) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, printStream);
        commandSession.put("APPLICATION", System.getProperty("karaf.name", "root"));
        commandSession.put("USER", "karaf");
        FutureTask<String> commandFuture = new FutureTask<String>(
                new Callable<String>() {
                    public String call() {
                        try {
                            if (!silent) {
                                System.out.println(command);
                                System.out.flush();
                            }
                            commandSession.execute(command);
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                        printStream.flush();
                        return byteArrayOutputStream.toString();
                    }
                });

        try {
            executor.submit(commandFuture);
            response =  commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }

        return response;
    }



    /**
     * Executes multiple commands inside a Single Session.
     * Commands have a default timeout of 10 seconds.
     * @param commands
     * @return
     */
    protected String executeCommands(final String ...commands) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, printStream);
        commandSession.put("APPLICATION", System.getProperty("karaf.name", "root"));
        commandSession.put("USER", "karaf");
        FutureTask<String> commandFuture = new FutureTask<String>(
                new Callable<String>() {
                    public String call() {
                        try {
                            for(String command:commands) {
                             System.out.println(command);
							 System.out.flush();
                             commandSession.execute(command);
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                        return byteArrayOutputStream.toString();
                    }
                });

        try {
            executor.submit(commandFuture);
            response =  commandFuture.get(COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }

        return response;
    }

    /**
     * Installs a feature and checks that feature is properly installed.
     *
     * @param feature
     * @throws Exception
     */
    public void installAndCheckFeature(String feature) throws Exception {
        System.err.println(executeCommand("features:install " + feature));
        FeaturesService featuresService = getOsgiService(FeaturesService.class);
        System.err.println(executeCommand("osgi:list -t 0"));
        assertTrue("Expected " + feature + " feature to be installed.", featuresService.isInstalled(featuresService.getFeature(feature)));
    }

    /**
     * Uninstalls a feature and checks that feature is properly uninstalled.
     *
     * @param feature
     * @throws Exception
     */
    public void unInstallAndCheckFeature(String feature) throws Exception {
        System.err.println(executeCommand("features:uninstall " + feature));
        FeaturesService featuresService = getOsgiService(FeaturesService.class);
        System.err.println(executeCommand("osgi:list -t 0"));
        assertFalse("Expected " + feature + " feature to be installed.", featuresService.isInstalled(featuresService.getFeature(feature)));
    }


    /**
     * This is used to customize the Probe that will contain the test.
     * We need to enable dynamic import of provisional bundles, to use the Console.
     *
     * @param probe
     * @return
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

}
