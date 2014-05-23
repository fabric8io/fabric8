/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.manager.support;

import java.io.File;
import java.net.MalformedURLException;
import java.util.jar.Manifest;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import aQute.lib.osgi.Jar;
import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.config.ProcessConfig;
import io.fabric8.process.manager.InstallOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.fabric8.process.manager.InstallOptions.InstallOptionsBuilder;

public class JarInstallerTest extends Assert {

    File installDir = new File("target", randomUUID().toString());
    JarInstaller jarInstaller;
    InstallOptions installOptions;

    @Before
    public void setUp() throws MalformedURLException {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

        installOptions = new InstallOptionsBuilder().url("mvn:org.apache.camel/camel-xstream/2.12.0/jar").build();
        jarInstaller = new JarInstaller(installOptions, newSingleThreadExecutor());
    }

    @Test
    public void shouldInstallJarDependencies() throws Exception {
        InstallContext installContext = new InstallContext(null, installDir, false);
        jarInstaller.install(installContext, new ProcessConfig(), "1", installDir);
        assertTrue(new File(installDir, "lib/xstream-1.4.4.jar").exists());
    }

    @Test
    public void shouldCopyMainJar() throws Exception {
        // When
        InstallContext installContext = new InstallContext(null, installDir, false);
        jarInstaller.install(installContext, new ProcessConfig(), "1", installDir);

        // Then
        Manifest manifest = new Jar(new File(installDir, "lib/main.jar")).getManifest();
        assertEquals("org.apache.camel.camel-xstream", manifest.getMainAttributes().getValue("Bundle-SymbolicName"));
    }


}
