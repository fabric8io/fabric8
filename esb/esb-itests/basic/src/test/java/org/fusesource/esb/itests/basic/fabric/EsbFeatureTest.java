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
package org.fusesource.esb.itests.basic.fabric;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import io.fabric8.itests.paxexam.support.FabricFeaturesTest;

import java.io.File;

import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

public class EsbFeatureTest extends FabricFeaturesTest {

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(esbDistributionConfiguration("jboss-fuse-minimal")),
        };
    }

    /**
     * Create an {@link org.ops4j.pax.exam.Option} for using a ESB distribution.
     */
    protected Option[] esbDistributionConfiguration(String distroArtifactId) {
        if (distroArtifactId == null) {
            distroArtifactId = ARTIFACT_ID;
        }
        return new Option[] {karafDistributionConfiguration().frameworkUrl(maven().groupId("org.jboss.fuse").artifactId(distroArtifactId).versionAsInProject().type("zip"))
                .karafVersion(MavenUtils.getArtifactVersion("org.jboss.fuse", distroArtifactId)).name("JBoss Fuse").unpackDirectory(new File("target/exam")).useDeployFolder(false),
                useOwnExamBundlesStartLevel(50),
                editConfigurationFilePut("etc/config.properties", "karaf.startlevel.bundle", "50"),
                editConfigurationFilePut("etc/config.properties", "karaf.startup.message", "Loading Fuse from: ${karaf.home}"),
                editConfigurationFilePut("etc/users.properties", "admin", "admin,admin"),
                mavenBundle("org.fusesource.tooling.testing", "pax-exam-karaf", MavenUtils.getArtifactVersion("org.fusesource.tooling.testing", "pax-exam-karaf")),
                mavenBundle("org.jboss.fuse.itests","esb-itests-common", MavenUtils.getArtifactVersion("org.jboss.fuse.itests", "esb-itests-common")),
                mavenBundle("io.fabric8.itests", "fabric-itests-common", MavenUtils.getArtifactVersion("io.fabric8.itests", "fabric-itests-common")),
                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
