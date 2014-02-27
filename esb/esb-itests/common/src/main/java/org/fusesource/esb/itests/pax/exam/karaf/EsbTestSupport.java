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

package org.fusesource.esb.itests.pax.exam.karaf;

import java.io.File;
import org.apache.karaf.tooling.exam.options.DoNotModifyLogOption;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;

public class EsbTestSupport extends FuseTestSupport {
    static final String GROUP_ID = "org.jboss.fuse";
    static final String ARTIFACT_ID = "jboss-fuse-minimal";
    private String version = "6.1.0.redhat-SNAPSHOT";

    protected void installQuickstartBundle(String bundle) throws Exception {
        String featureInstallOutput = executeCommand("osgi:install -s mvn:org.jboss.quickstarts.fuse/" + bundle + "/" + version);
        System.out.println(featureInstallOutput);
        assertFalse(featureInstallOutput.isEmpty());
        String featureListOutput = executeCommand("osgi:list -l | grep " + bundle);
        System.out.println(featureListOutput);
        assertFalse(featureListOutput.isEmpty());
    }
    
    protected void installUninstallCommand(String feature) throws Exception {
        installUninstallCommand(feature, false);
    }
    
    protected void installUninstallCommand(String feature, boolean refresh) throws Exception {
        String installFeatureCmd = "features:install -v ";
        if (!refresh) {
            installFeatureCmd = installFeatureCmd + "-r ";
        }
        String featureInstallOutput = executeCommand(installFeatureCmd + feature);
        System.out.println(featureInstallOutput);
        assertFalse(featureInstallOutput.isEmpty());
        String featureListOutput = executeCommand("features:list -i | grep " + feature);
        System.out.println(featureListOutput);
        assertFalse(featureListOutput.isEmpty());
        System.out.println(executeCommand("features:uninstall " + feature));
        featureListOutput = executeCommand("features:list -i | grep " + feature);
        System.out.println(featureListOutput);
        assertTrue(featureListOutput.isEmpty());
    }
    
    protected Option[] esbDistributionConfiguration() {
        return esbDistributionConfiguration(null);
    }

    /**
     * Create an {@link org.ops4j.pax.exam.Option} for using a ESB distribution.
     *
     * @return
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

    protected String getEsbVersion() {
        return version;
    }
}
