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

import org.fusesource.process.manager.support.DefaultProcessController;
import org.fusesource.process.manager.support.ProcessManagerImpl;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ProcessControllerTest {
    protected ProcessManagerImpl processManager = new ProcessManagerImpl(new File("target/processes"));

    @Test
    public void startStopCamelSample() throws Exception {
        processManager.init();

        InstallTask postInstall = null;

        // TODO warning - hard coded version!!!
        String version = "99-master-SNAPSHOT";
        Installation install = processManager.install("mvn:org.fusesource.process.samples/process-sample-camel-spring/" + version + "/tar.gz", null, postInstall);

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
