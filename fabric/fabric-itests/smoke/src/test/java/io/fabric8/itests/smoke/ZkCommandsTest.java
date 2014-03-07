/**
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
package io.fabric8.itests.smoke;

import io.fabric8.itests.paxexam.support.FabricTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

import static java.lang.System.err;
import static java.net.InetAddress.getLocalHost;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

@RunWith(JUnit4TestRunner.class)
public class ZkCommandsTest extends FabricTestSupport {

    @Test
    public void shouldSetDefaultZNodeValue() throws Exception {
        // Given
        err.print(executeCommand("fabric:create -n"));

        // When
        err.print(executeCommand("zk:create /fabric/foo"));

        // Then
        String defaultZNodeValue = executeCommand("zk:get /fabric/foo").trim();
        assertEquals(getLocalHost().getHostAddress(), defaultZNodeValue);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                scanFeatures("default", "fabric-zookeeper-commands")
        };
    }
}
