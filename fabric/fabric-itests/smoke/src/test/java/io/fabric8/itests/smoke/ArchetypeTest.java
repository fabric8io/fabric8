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

import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;
import java.util.Set;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ArchetypeTest extends FabricTestSupport {

    @Test
    public void testCreateArchetypes() throws Exception {
        File archetypesDir = getArchetypesFolder();
        File[] files = archetypesDir.listFiles();
        assertNotNull("Should have child folders inside " + archetypesDir.getAbsolutePath(), files);
        for (File file : files) {
            archetypeInfo = loadArtifactInfo(file);
        }

        System.err.println(executeCommand("fabric:create -n"));


        String jvmopts = "-Xms512m -XX:MaxPermSize=512m -Xmx2048m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5008";
        Set<Container> containers = ContainerBuilder.child(1).withName("child").withJvmOpts(jvmopts).assertProvisioningResult().build();
        try {
            Assert.assertEquals("One container", 1, containers.size());
            Container child = containers.iterator().next();
            Assert.assertEquals("child1", child.getId());
            Assert.assertEquals("root", child.getParent().getId());
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

    protected File getArchetypesFolder() {
        String basedir = System.getProperty("basedir", ".");
        File answer = new File(basedir, "../../tooling/archetypes");
        assertFolderExists(answer);
        return answer;
    }

    public static void assertFolderExists(File dir) {
        assertTrue("the folder does not exist! " + dir.getAbsolutePath(), dir.exists());
        assertTrue("the path is not a folder! " + dir.getAbsolutePath(), dir.isDirectory());
    }

    public static void assertFileExists(File file) {
        assertTrue("the file does not exist! " + file.getAbsolutePath(), file.exists());
        assertTrue("the path is not a file! " + file.getAbsolutePath(), file.isFile());
    }

    @Configuration
    public Option[] config() {
        return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()),
                KarafDistributionOption.debugConfiguration("5005", false) };
    }
}