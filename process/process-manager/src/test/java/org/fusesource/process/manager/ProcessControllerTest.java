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
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class ProcessControllerTest {
    // TODO we really should install this from the mvn tarball
    protected File installDir = new File("../samples/process-sample-camel-spring/target/process-sample-camel-spring-99-master-SNAPSHOT");

    @Test
    public void startStopCamelSample() throws Exception {
        //installDir.mkdirs();

        if (!installDir.exists()) {
            fail("Installation does not exist: " + installDir);
        }

        // now lets start the process
        DefaultProcessController controller = new DefaultProcessController();
        controller.setBaseDir(installDir);

        int rc = controller.start();
        assertEquals("Return code", 0, rc);

        Thread.sleep(2000);

        rc = controller.stop();
        assertEquals("Return code", 0, rc);
    }
}
