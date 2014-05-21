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
package io.fabric8.process.manager;

import io.fabric8.common.util.Strings;
import io.fabric8.process.test.AbstractProcessTest;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static io.fabric8.api.FabricConstants.FABRIC_VERSION;

public class ProcessControllerTest extends AbstractProcessTest {

    @Test
    public void startStopCamelSample() throws Exception {
        InstallOptions options = InstallOptions.builder()
                                               .name("camel-sample")
                                               .url(new URL(null, "mvn:io.fabric8.samples/process-sample-camel-spring/" + FABRIC_VERSION + "/tar.gz"))
                                               .build();

        Installation install = processManagerService.install(options, null);

        String id = install.getId();
        assertTrue("ID should not be blank " + id, Strings.isNotBlank(id));
        File installDir = install.getInstallDir();
        if (!installDir.exists()) {
            fail("Installation does not exist: " + installDir);
        }

        // now lets start the process
        ProcessController controller = install.getController();

        int rc = startProcess(controller);
        assertEquals("Return code", 0, rc);

        Thread.sleep(2000);

        rc = stopProcess(controller);
        assertEquals("Return code", 0, rc);
    }
}
