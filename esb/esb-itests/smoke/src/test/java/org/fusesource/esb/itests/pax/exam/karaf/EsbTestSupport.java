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

import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

public class EsbTestSupport extends FuseTestSupport {
    static final String GROUP_ID = "org.jboss.fuse";
    static final String ARTIFACT_ID = "jboss-fuse-minimal";

    static final String KARAF_GROUP_ID = "org.apache.karaf";
    static final String KARAF_ARTIFACT_ID = "apache-karaf";

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
     * Create an {@link org.ops4j.pax.exam.Option} for using a ESB distribution.
     *
     * @return
     */
    protected Option esbDistributionConfiguration() {
        return new DefaultCompositeOption(
                new Option[]{
                        karafDistributionConfiguration()
                                .frameworkUrl(
                                        maven()
                                                .groupId(GROUP_ID)
                                                .artifactId(ARTIFACT_ID)
                                                .versionAsInProject()
                                                .type("zip"))
                                .karafVersion(getKarafVersion())
                                .name("JBoss Fuse Distro")
                                .unpackDirectory(new File("target/paxexam/unpack/"))
                                .useDeployFolder(false),
                        // Don't bother with local console output as it just ends up cluttering the logs
                        configureConsole().ignoreLocalConsole(),
                        useOwnExamBundlesStartLevel(50),
                        editConfigurationFilePut("etc/config.properties", "karaf.startlevel.bundle", "50"),
                        //We need to delegate org.osgi.framework to bootloader because, when pax exam loads karaf, it puts
                        //felix framework jar file in the top of the list, felix jar has BundleContext, just as karaf.jar.
                        //When ServiceInjector tries to inject BundleContext into test class, "BundleContext.class == type"
                        //returns false and BundleContext can never be resolved. The problem is that the first BundleContext
                        //is loaded by one classloader from karaf.jar and the second BundleContext by another classloader from felix jar,
                        //By delegating org.osgi.framework we make sure that equality returns true, since both classes are loaded by the same classloader
                        bootDelegationPackage("org.osgi.framework"),
                        mavenBundle("org.fusesource.tooling.testing","pax-exam-karaf", MavenUtils.getArtifactVersion("org.fusesource.tooling.testing", "pax-exam-karaf"))
                        // Remember that the test executes in another process.  If you want to debug it, you need
                        // to tell Pax Exam to launch that process with debugging enabled.  Launching the test class itself with
                        // debugging enabled (for example in Eclipse) will not get you the desired results.
                        //debugConfiguration("5000", true),
                });
    }
}
