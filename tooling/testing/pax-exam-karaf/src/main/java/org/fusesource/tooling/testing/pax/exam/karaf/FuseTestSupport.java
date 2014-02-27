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

import io.fabric8.api.ServiceLocator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.junit.Assert;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

public class FuseTestSupport {

    public static final Long DEFAULT_TIMEOUT = 30000L;
    public static final Long SYSTEM_TIMEOUT = 30000L;
    public static final Long DEFAULT_WAIT = 10000L;
    public static final Long PROVISION_TIMEOUT = 300000L;
    public static final Long COMMAND_TIMEOUT = 70000L;

    static final ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    protected BundleContext bundleContext;

    protected Bundle installBundle(String groupId, String artifactId) throws Exception {
        MavenArtifactProvisionOption mvnUrl = CoreOptions.mavenBundle(groupId, artifactId).versionAsInProject();
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
     */
    public static Option copySystemProperty(String propertyName) {
        return KarafDistributionOption.editConfigurationFilePut("etc/system.properties", propertyName, System.getProperty(propertyName) != null ? System.getProperty(propertyName) : "");
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
     */
    public static String tryCommand(final String command) {
        try {
            return executeCommands(COMMAND_TIMEOUT, false, command);
        } catch (Throwable t) {
            return "Error executing command:" + t.getMessage();
        }
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     */
    public static String executeCommand(final String command) {
        return executeCommands(COMMAND_TIMEOUT, false, command);
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     */
    public static String executeCommands(final String... commands) {
        return executeCommands(COMMAND_TIMEOUT, false, commands);
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     * @param command The command to execute.
     * @param timeout The amount of time in millis to wait for the command to execute.
     * @param silent  Specifies if the command should be displayed in the screen.
     */
    public static String executeCommand(final String command, final long timeout, final boolean silent) {
        return executeCommands(timeout, silent, command);
    }

    /**
    * Executes a shell command and returns output as a String.
    * Commands have a default timeout of 10 seconds.
    * @param timeout The amount of time in millis to wait for the command to execute.
    * @param silent  Specifies if the command should be displayed in the screen.
    * @param commands The command to execute.
    */
    public static String executeCommands(final long timeout, final boolean silent, final String... commands) {
        String response = null;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandProcessor commandProcessor = ServiceLocator.awaitService(FrameworkUtil.getBundle(FuseTestSupport.class).getBundleContext(), CommandProcessor.class);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, printStream);
        commandSession.put("APPLICATION", System.getProperty("karaf.name", "root"));
        commandSession.put("USER", "karaf");
        FutureTask<String> commandFuture = new FutureTask<String>(new Callable<String>() {
            public String call() throws Exception {
                for (String command : commands) {
                    boolean keepRunning = true;

                    if (!silent) {
                        System.out.println(command);
                        System.out.flush();
                    }

                    while (!Thread.currentThread().isInterrupted() && keepRunning) {
                        try {
                            commandSession.execute(command);
                            keepRunning = false;
                        } catch (Exception e) {
                            if (retryException(e)) {
                                keepRunning = true;
                                sleep(1000);
                            } else {
                                throw new CommandExecutionException(e);
                            }
                        }
                    }
                }
                printStream.flush();
                return byteArrayOutputStream.toString();
            }

        });

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw CommandExecutionException.launderThrowable(e.getCause());
        } catch (Exception e) {
            throw CommandExecutionException.launderThrowable(e);
        }

        return response;
    }

    private static boolean retryException(Exception e) {
        //The gogo runtime package is not exported, so we are just checking against the class name.
        return e.getClass().getName().equals("org.apache.felix.gogo.runtime.CommandNotFoundException");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Installs a feature and checks that feature is properly installed.
     */
    public void installAndCheckFeature(String feature) throws Exception {
        System.err.println(executeCommand("features:install " + feature));
        FeaturesService featuresService = ServiceLocator.awaitService(bundleContext, FeaturesService.class);
        System.err.println(executeCommand("osgi:list -t 0"));
        Assert.assertTrue("Expected " + feature + " feature to be installed.", featuresService.isInstalled(featuresService.getFeature(feature)));
    }

    /**
     * Uninstalls a feature and checks that feature is properly uninstalled.
     */
    public void unInstallAndCheckFeature(String feature) throws Exception {
        System.err.println(executeCommand("features:uninstall " + feature));
        FeaturesService featuresService = ServiceLocator.awaitService(bundleContext, FeaturesService.class);
        System.err.println(executeCommand("osgi:list -t 0"));
        Assert.assertFalse("Expected " + feature + " feature to be installed.", featuresService.isInstalled(featuresService.getFeature(feature)));
    }

    /**
     * This is used to customize the Probe that will contain the test.
     * We need to enable dynamic import of provisional bundles, to use the Console.
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

}
