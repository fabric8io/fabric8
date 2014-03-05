/*
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
package org.fusesource.process.manager;

import io.fabric8.internal.FabricConstants;
import org.fusesource.process.manager.service.ProcessManagerService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.url.mvn.Handler;

import javax.management.MalformedObjectNameException;

import java.io.File;
import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


@Ignore("[FABRIC-624][7.4] Fix process manager ProcessControllerTest")
public class ProcessControllerTest {
    protected ProcessManagerService processManager;

    @Before
    public void setUp() throws MalformedObjectNameException {
        processManager = new ProcessManagerService(new File("target/processes"));
    }

    @Test
    public void startStopCamelSample() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url.mvn");
        processManager.init();

        InstallTask postInstall = null;

        String version = FabricConstants.FABRIC_VERSION;

        InstallOptions options = InstallOptions.builder()
                                               .name("camel-sample")
                                               .url(new URL(null, "mvn:io.fabric8.samples/process-sample-camel-spring/" + version + "/tar.gz", new Handler()))
                                               .build();

        Installation install = processManager.install(options, postInstall);

        int id = install.getId();
        assertTrue("ID should be > 0 but was " + id, id > 0);
        File installDir = install.getInstallDir();
        if (!installDir.exists()) {
            fail("Installation does not exist: " + installDir);
        }

        // now lets start the process
        ProcessController controller = install.getController();

        int rc = controller.start();
        assertEquals("Return code", 0, rc);

        Thread.sleep(2000);

        rc = controller.stop();
        assertEquals("Return code", 0, rc);
    }
}
