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

package org.fusesource.fabric.itests.paxexam.examples;


import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleCamelProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("camel1");
        destroyChildContainer("broker1");
    }

    @Test
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("broker1", "root", "mq");
        createAndAssertChildContainer("camel1", "root", "example-camel");
        System.err.println(executeCommand("fabric:cluster-list"));
        Thread.sleep(5000);
        String output = executeCommand("fabric:container-connect -u admin -p admin camel1 camel:route-info route2");
        System.err.println(output);
        Assert.assertTrue("Expected at least 1 completed exchange.",output.contains("Exchanges Completed: ") && !output.contains("Exchanges Completed: 0"));
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
