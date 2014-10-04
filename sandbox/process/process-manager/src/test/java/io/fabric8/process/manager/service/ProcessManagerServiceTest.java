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
package io.fabric8.process.manager.service;

import com.google.common.io.Files;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.charset.Charset;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

public class ProcessManagerServiceTest extends Assert {

    File installDir = new File("target", randomUUID().toString());

    ProcessManagerService processManagerService;

    InstallOptions installOptions;
    InstallTask postInstall;
    String firstJvmOption = "-Dfoo=bar";

    String secondJvmOption = "-server";

    @Before
    public void setUp() throws MalformedURLException, MalformedObjectNameException {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

        processManagerService = new ProcessManagerService(installDir);

        installOptions = new InstallOptions.InstallOptionsBuilder().
                jvmOptions(firstJvmOption, secondJvmOption).
                url("mvn:org.apache.camel/camel-xstream/2.12.0/jar").build();
    }

    @Test
    public void shouldGenerateJvmConfig() throws Exception {
        // When
        processManagerService.installJar(installOptions, postInstall);
        String generatedJvmConfig = Files.toString(new File(installDir, "1/etc/jvm.config"), Charset.forName("UTF-8"));

        // Then
        assertEquals(format("%s %s ", firstJvmOption, secondJvmOption), generatedJvmConfig);
    }

}
