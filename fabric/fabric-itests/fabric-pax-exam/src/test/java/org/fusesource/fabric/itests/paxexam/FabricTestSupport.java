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

package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.SshContainerBuilder;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.osgi.service.blueprint.container.BlueprintContainer;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

public class FabricTestSupport extends FuseTestSupport {

    public static final String FABRIC_ITEST_GROUP_ID = "FABRIC_ITEST_GROUP_ID";
    public static final String FABRIC_ITEST_ARTIFACT_ID = "FABRIC_ITEST_ARTIFACT_ID";

    public static final String GROUP_ID = System.getenv().containsKey(FABRIC_ITEST_GROUP_ID) ? System.getenv(FABRIC_ITEST_GROUP_ID) : "org.fusesource.fabric";
    public static final String ARTIFACT_ID = System.getenv().containsKey(FABRIC_ITEST_ARTIFACT_ID) ? System.getenv(FABRIC_ITEST_ARTIFACT_ID) : "fuse-fabric";

    static final String KARAF_GROUP_ID = "org.apache.karaf";
    static final String KARAF_ARTIFACT_ID = "apache-karaf";


    /**
     * Creates a child {@ling Agent} witht the given name.
     *
     * @param name   The name of the child {@ling Agent}.
     * @param parent
     * @return
     */
    protected Container createChildContainer(String name, String parent, String profileName) throws Exception {
        return createChildContainer(name, parent, profileName, null);
    }

    protected Container createChildContainer(String name, String parent, String profileName, String jvmOpts) throws Exception {
        FabricService fabricService = getFabricService();

        Thread.sleep(DEFAULT_WAIT);

        Container parentContainer = fabricService.getContainer(parent);
        assertNotNull(parentContainer);

        CreateContainerOptions args = CreateContainerOptionsBuilder.child().name(name).parent(parent).zookeeperPassword(fabricService.getZookeeperPassword()).jmxUser("admin").jmxPassword("admin");
        if (jvmOpts != null) {
            args.setJvmOpts(jvmOpts);
        } else {
            args.setJvmOpts("-Xms1024m -Xmx1024m");
        }

        CreateContainerMetadata[] metadata = fabricService.createContainers(args);
        if (metadata.length > 0) {
            if (metadata[0].getFailure() != null) {
                throw new Exception("Error creating child container:" + name, metadata[0].getFailure());
            }
            Container container = metadata[0].getContainer();
            Version version = fabricService.getDefaultVersion();
            Profile profile = fabricService.getProfile(version.getName(), profileName);
            assertNotNull("Expected to find profile with name:" + profileName, profile);
            container.setProfiles(new Profile[]{profile});
            waitForProvisionSuccess(container, PROVISION_TIMEOUT);
            return container;
        }
        throw new Exception("Could container not created");
    }

    protected void destroyChildContainer(String name) throws InterruptedException {
        try {
            //Wait for zookeeper service to become available.
            Thread.sleep(DEFAULT_WAIT);
            //We want to check if container exists before we actually delete them.
            //We need this because getContainer will create a container object if container doesn't exists.
            if (getZookeeper().exists(ZkPath.CONTAINER.getPath(name)) != null) {
                Container container = getFabricService().getContainer(name);
                //We want to go through container destroy method so that cleanup methods are properly invoked.
                container.destroy();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Waits for a container to successfully provision.
     *
     * @param container
     * @param timeout
     * @throws Exception
     */
    public void waitForProvisionSuccess(Container container, Long timeout) throws Exception {
        System.err.println("Waiting for container: " + container.getId() + " to succesfully provision");
        for (long t = 0; (!(container.isAlive() && container.getProvisionStatus().equals("success") && container.getSshUrl() != null) && t < timeout); t += 2000L) {
            if (container.getProvisionException() != null) {
                throw new Exception(container.getProvisionException());
            }
            Thread.sleep(2000L);
            System.err.println("Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " SSH URL:" + container.getSshUrl());
        }
        if (!container.isAlive() || !container.getProvisionStatus().equals("success") || container.getSshUrl() == null) {
            throw new Exception("Could not provision " + container.getId() + " after " + timeout + " seconds. Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus() + " Ssh URL:" + container.getSshUrl());
        }
    }


    /**
     * Creates a child container, waits for succesfull provisioning and asserts, its asigned the right profile.
     *
     * @param name
     * @param parent
     * @param profile
     * @throws Exception
     */
    public Container createAndAssertChildContainer(String name, String parent, String profile) throws Exception {
        return createAndAssertChildContainer(name, parent, profile, null);
    }

    public Container createAndAssertChildContainer(String name, String parent, String profile, String jvmOpts) throws Exception {
        FabricService fabricService = getFabricService();

        Container child1 = createChildContainer(name, parent, profile, jvmOpts);
        Container result = fabricService.getContainer(name);
        assertEquals("Containers should have the same id", child1.getId(), result.getId());
        return result;
    }

    /**
     * Cleans a containers profile by switching to default profile and reseting the profile.
     *
     * @param containerName
     * @param profileName
     * @throws Exception
     */
    public boolean containerSetProfile(String containerName, String profileName) throws Exception {
        return containerSetProfile(containerName, profileName, true);
    }

    /**
     * Cleans a containers profile by switching to default profile and reseting the profile.
     *
     * @param containerName
     * @param profileName
     * @throws Exception
     */
    public boolean containerSetProfile(String containerName, String profileName, Boolean waitForProvision) throws Exception {
        System.out.println("Switching profile: " + profileName + " on container:" + containerName);
        FabricService fabricService = getFabricService();

        Container container = fabricService.getContainer(containerName);
        Version version = container.getVersion();
        Profile[] profiles = new Profile[]{fabricService.getProfile(version.getName(), profileName)};
        Profile[] currentProfiles = container.getProfiles();

        Arrays.sort(profiles);
        Arrays.sort(currentProfiles);

        boolean same = true;
        if (profiles.length != currentProfiles.length) {
            same = false;
        } else {
            for (int i = 0; i < currentProfiles.length; i++) {
                if (!currentProfiles[i].configurationEquals(profiles[i])) {
                    same = false;
                }
            }
        }

        if (!same && waitForProvision) {
            //This is required so that waitForProvisionSuccess doesn't retrun before the deployment agent kicks in.
            ZooKeeperUtils.set(getZookeeper(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(containerName), "switching profile");
            container.setProfiles(profiles);
            waitForProvisionSuccess(container, PROVISION_TIMEOUT);
        }
        return same;
    }

    public void addStagingRepoToDefaultProfile() {
        executeCommand("fabric:profile-edit -p org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.repositories=" +
                "http://repo1.maven.org/maven2," +
                "http://repo.fusesource.com/nexus/content/repositories/releases," +
                "http://repo.fusesource.com/nexus/content/repositories/snapshots@snapshots@noreleases," +
                "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases," +
                "http://svn.apache.org/repos/asf/servicemix/m2-repo," +
                "http://repository.springsource.com/maven/bundles/release," +
                "http://repository.springsource.com/maven/bundles/external," +
                "http://scala-tools.org/repo-releases," +
                "http://repo.fusesource.com/nexus/content/groups/ea" +
                " default");
    }

    public FabricService getFabricService() {
        FabricService fabricService = ServiceLocator.getOsgiService(FabricService.class);
        assertNotNull(fabricService);
        return fabricService;
    }

    public IZKClient getZookeeper() {
        IZKClient zookeeper = ServiceLocator.getOsgiService(IZKClient.class);
        assertNotNull(zookeeper);
        return zookeeper;
    }

    protected void waitForFabricCommands() {
        getOsgiService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=org.fusesource.fabric.fabric-commands)", DEFAULT_TIMEOUT);
    }


    /**
     * Returns the Version of Karaf to be used.
     *
     * @return
     */
    protected String getKarafVersion() {
        //TODO: This is a hack because pax-exam-karaf will not work with non numeric characters in the version.
        //We will need to change it once pax-exam-karaf get fixed (version 0.4.0 +).
        return "2.2.5";
    }

    /**
     * Create an {@link Option} for using a Fabric distribution.
     *
     * @return
     */
    protected Option[] fabricDistributionConfiguration() {
        return new Option[]{
                karafDistributionConfiguration().frameworkUrl(
                        maven().groupId(GROUP_ID).artifactId(ARTIFACT_ID).versionAsInProject().type("zip"))
                        .karafVersion(getKarafVersion()).name("Fabric Karaf Distro").unpackDirectory(new File("target/paxexam/unpack/")),
                useOwnExamBundlesStartLevel(50),
                envAsSystemProperty(ContainerBuilder.CONTAINER_TYPE_PROPERTY, "child"),
                envAsSystemProperty(ContainerBuilder.CONTAINER_NUMBER_PROPERTY, "1"),
                envAsSystemProperty(SshContainerBuilder.SSH_HOSTS_PROPERTY),
                envAsSystemProperty(SshContainerBuilder.SSH_USERS_PROPERTY),
                envAsSystemProperty(SshContainerBuilder.SSH_PASSWORD_PROPERTY),
                envAsSystemProperty(SshContainerBuilder.SSH_RESOLVER_PROPERTY),

                editConfigurationFilePut("etc/config.properties", "karaf.startlevel.bundle", "50"),
                editConfigurationFilePut("etc/users.properties", "admin", "admin,admin"),
                mavenBundle("org.fusesource.tooling.testing", "pax-exam-karaf", MavenUtils.getArtifactVersion("org.fusesource.tooling.testing", "pax-exam-karaf")),
                logLevel(LogLevelOption.LogLevel.WARN),
                keepRuntimeFolder()
        };
    }

    public Object getMBean(Container container, ObjectName mbeanName, Class clazz) throws Exception {
        JMXServiceURL url = new JMXServiceURL(container.getJmxUrl());
        Map env = new HashMap();
        String[] creds = {"admin", "admin"};
        env.put(JMXConnector.CREDENTIALS, creds);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        return JMX.newMBeanProxy(mbsc, mbeanName, clazz, true);
    }

    private Option envAsSystemProperty(String name) {
        return envAsSystemProperty(name, "");
    }

    private Option envAsSystemProperty(String name, String defaultValue) {
        String value = System.getenv(name);
        return editConfigurationFilePut("etc/system.properties", name, (value != null && !value.isEmpty()) ? value : defaultValue);
    }

}

