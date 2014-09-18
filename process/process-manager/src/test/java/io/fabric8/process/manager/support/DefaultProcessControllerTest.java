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

import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;
import io.fabric8.process.manager.service.ProcessManagerService;
import io.fabric8.process.manager.support.command.CommandFailedException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static java.util.UUID.randomUUID;

public class DefaultProcessControllerTest {

    File installDir = new File("target", randomUUID().toString());

    DefaultProcessController controller;
    InstallTask postInstall;

    @Before
    public void setUp() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

        InstallOptions installOptions = new InstallOptions.InstallOptionsBuilder().build();
        String processId = new ProcessManagerService(installDir).installJar(installOptions, postInstall).getId();
        controller = new DefaultProcessController(processId, new ProcessConfig(), new File(installDir, processId));
    }

    @Test(expected = CommandFailedException.class)
    public void shouldFailedToRunLaunchScriptWithoutMainClass() throws Exception {
        // Null command == launch script
        controller.runConfigCommandValueOrLaunchScriptWith(null, "start");
    }

}
