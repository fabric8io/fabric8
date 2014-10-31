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
package io.fabric8.wiki;

import io.hawt.git.GitFacade;
import org.junit.Test;

import java.io.File;

/**
 * A simple test case to create a new hawtio wiki that can be used for developing hawtio using the
 * contents from the Apps from this hawtio build
 */
public class CreateHawtioWikiTest {
    GitFacade git = new GitFacade();

    @Test
    public void testCreateHawtioWiki() throws Exception {
        git.setInitialImportURLs("mvn:io.fabric8/wiki/2.0.0-SNAPSHOT/zip");
        git.setCloneRemoteRepoOnStartup(false);
        File configDir = new File(getBasedir() + "/target/hawtioConfig");
        configDir.mkdirs();

        git.setConfigDirectory(configDir);
        git.init();

        System.out.println("Created testing hawtio wiki at: " + configDir.getAbsolutePath());
    }

    public static String getBasedir() {
        return System.getProperty("basedir", ".");
    }
}
