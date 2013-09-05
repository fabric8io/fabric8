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

import static junit.framework.Assert.assertNotNull;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.SshContainerBuilder;
import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;

public class WildflyTestSupport extends FuseTestSupport {

    public static final String FABRIC_ITEST_GROUP_ID = "FABRIC_ITEST_GROUP_ID";
    public static final String FABRIC_ITEST_ARTIFACT_ID = "FABRIC_ITEST_ARTIFACT_ID";

    public static final String GROUP_ID = System.getenv().containsKey(FABRIC_ITEST_GROUP_ID) ? System.getenv(FABRIC_ITEST_GROUP_ID) : "org.fusesource.fabric";
    public static final String ARTIFACT_ID = System.getenv().containsKey(FABRIC_ITEST_ARTIFACT_ID) ? System.getenv(FABRIC_ITEST_ARTIFACT_ID) : "fuse-fabric";

    public FabricService getFabricService() {
        FabricService fabricService = ServiceLocator.getOsgiService(FabricService.class);
        assertNotNull(fabricService);
        return fabricService;
    }

    protected String getKarafVersion() {
        //TODO: This is a hack because pax-exam-karaf will not work with non numeric characters in the version.
        //We will need to change it once pax-exam-karaf get fixed (version 0.4.0 +).
        return "2.2.5";
    }

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
                logLevel(LogLevelOption.LogLevel.ERROR),
                keepRuntimeFolder()
        };
    }

    private Option envAsSystemProperty(String name) {
        return envAsSystemProperty(name, "");
    }

    private Option envAsSystemProperty(String name, String defaultValue) {
        String value = System.getenv(name);
        return editConfigurationFilePut("etc/system.properties", name, (value != null && !value.isEmpty()) ? value : defaultValue);
    }

    protected void waitForProvisionSuccess(Container container) throws Exception {
    	waitForProvisionSuccess(container, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    protected void waitForProvisionSuccess(Container container, long timeout, TimeUnit unit) throws Exception {
        System.err.println("Waiting for container: " + container.getId() + " to succesfully provision");
        boolean success = container.isAlive() && container.getProvisionStatus().equals("success");
        for (long t = 0; !success && t < unit.toMillis(timeout); t += 1000) {
            if (container.getProvisionException() != null) {
                throw new Exception(container.getProvisionException());
            }
            Thread.sleep(1000);
            System.err.println("DataStore:" + container.getFabricService().getDataStore());
            success = container.isAlive() && container.getProvisionStatus().equals("success");
            System.err.println("Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus());
        }
        System.err.println("Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus());
        if (!success) {
            throw new Exception("Could not provision " + container.getId() + " Alive:" + container.isAlive() + " Status:" + container.getProvisionStatus());
        }
    }

	protected Profile getProfile(Container container, String name) throws Exception {
		return getProfile(container, name, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	protected Profile getProfile(Container container, String name, long timeout, TimeUnit unit) throws Exception {
		Profile result = null;
        for (long t = 0;  result == null && t < unit.toMillis(timeout); t += 200) {
			Profile[] profiles = container.getProfiles();
			for (Profile aux : profiles) {
				if (name.equals(aux.getId())) {
					result = aux;
					break;
				}
			}
            Thread.sleep(200);
        }
		return result;
	}
}

