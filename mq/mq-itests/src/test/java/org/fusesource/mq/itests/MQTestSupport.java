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
package org.fusesource.mq.itests;


import org.apache.karaf.tooling.exam.options.DoNotModifyLogOption;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.SshContainerBuilder;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;

import java.io.File;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;

public class MQTestSupport extends FabricTestSupport {

    static final String MQ_GROUP_ID = "org.jboss.amq";
    static final String MQ_ARTIFACT_ID = "jboss-a-mq";
    static final String WEB_CONSOLE_URL = "http://localhost:8181/activemqweb/";


    protected Option[] mqDistributionConfiguration() {
        return new Option[]{
                karafDistributionConfiguration().frameworkUrl(
                        maven().groupId(MQ_GROUP_ID).artifactId(MQ_ARTIFACT_ID).versionAsInProject().type("zip"))
                        .karafVersion(getKarafVersion()).name("JBoss MQ Distro").unpackDirectory(new File("target/paxexam/unpack/")).useDeployFolder(false),
                useOwnExamBundlesStartLevel(50),
                envAsSystemProperty(ContainerBuilder.CONTAINER_TYPE_PROPERTY, "child"),
                envAsSystemProperty(ContainerBuilder.CONTAINER_NUMBER_PROPERTY, "1"),
                envAsSystemProperty(SshContainerBuilder.SSH_HOSTS_PROPERTY),
                envAsSystemProperty(SshContainerBuilder.SSH_USERS_PROPERTY),
                envAsSystemProperty(SshContainerBuilder.SSH_PASSWORD_PROPERTY),
                envAsSystemProperty(SshContainerBuilder.SSH_RESOLVER_PROPERTY),

                editConfigurationFilePut("etc/config.properties", "karaf.startlevel.bundle", "50"),
                editConfigurationFilePut("etc/users.properties", "admin", "admin,admin"),
                editConfigurationFilePut("etc/system.properties", "activemq.jmx.user", "admin"),
                editConfigurationFilePut("etc/system.properties", "activemq.jmx.password", "admin"),
                editConfigurationFilePut("etc/config.properties", "karaf.startup.message", "Loading JBoss AMQ from: ${karaf.home}"),
                mavenBundle("io.fabric8.itests", "fabric-itests-common", MavenUtils.getArtifactVersion("io.fabric8.itests", "fabric-itests-common")),
                mavenBundle("org.fusesource.tooling.testing", "pax-exam-karaf", MavenUtils.getArtifactVersion("org.fusesource.tooling.testing", "pax-exam-karaf")),
                new DoNotModifyLogOption(),
                keepRuntimeFolder()
        };
    }
}
