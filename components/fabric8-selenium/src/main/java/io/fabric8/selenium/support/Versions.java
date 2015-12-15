/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.selenium.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A helper class for returning versions of maven artifacts
 */
public class Versions {
    private static Properties versions;
    private static File versionsFile;

    public static Properties getVersions() {
        if (versions == null) {
            versions = new Properties();

            String basedir = System.getProperty("basedir", ".");
            versionsFile = new File(basedir, "target/test-classes/versions.properties");
            assertTrue("Versions file does not exist: " + versionsFile.getPath(), versionsFile.exists() && versionsFile.isFile());

            try {
                versions.load(new FileInputStream(versionsFile));
            } catch (IOException e) {
                throw new AssertionError("Failed to load " + versionsFile.getPath() + ". " + e, e);
            }
        }
        return versions;
    }

    public static String getVersion(String name) {
        String answer = getVersions().getProperty(name);
        assertNotNull("Missing version value in file " + versionsFile.getPath() + " for key `" + name + "`", name);
        return answer;
    }
}
