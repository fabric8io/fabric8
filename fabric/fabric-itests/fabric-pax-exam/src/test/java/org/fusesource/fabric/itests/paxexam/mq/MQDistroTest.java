/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.itests.paxexam.mq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.*;
import static org.ops4j.pax.exam.CoreOptions.maven;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQDistroTest {

    static final String MQ_GROUP_ID = "org.fusesource.mq";
    static final String MQ_ARTIFACT_ID = "fuse-mq";

    @Test
    public void testMQ() throws Exception {

    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                mqDistributionConfiguration(), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)
        };
    }

    protected Option mqDistributionConfiguration() {
        return karafDistributionConfiguration().frameworkUrl(
                maven().groupId(MQ_GROUP_ID).artifactId(MQ_ARTIFACT_ID).versionAsInProject().type("tar.gz"))
                .karafVersion("2.2.2").name("Fabric MQ Distro").unpackDirectory(new File("target/paxexam/unpack/"));
    }

}
