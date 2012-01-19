/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
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
