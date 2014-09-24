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
package io.fabric8.agent;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DeploymentBuilderTest {

    private DeploymentBuilder builder;

    @Before
    public void setUp() {
        this.builder = new DeploymentBuilder(null, null, 0l);
    }

    @Test
    public void testGetAttributes() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File file = File.createTempFile("testGetAttributes", "", new File(basedir + File.separator + "target"));
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().put(new Attributes.Name("Header"), "value");
        JarOutputStream jar = new JarOutputStream(new FileOutputStream(file), m);
        jar.finish();
        jar.close();

        Attributes attributes = this.builder.getAttributes("http://x", file);
        assertThat(attributes.getValue("Header").toString(), equalTo("value"));
        file.delete();
    }

    @Test
    public void testGetAttributesFromBrokenJarFile() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File file = File.createTempFile("testGetAttributes", "", new File(basedir + File.separator + "target"));
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(new byte[] { 0x00, 0x42 });
        fos.close();

        try {
            Attributes attributes = this.builder.getAttributes("http://x", file);
            fail("Should throw IOException");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("Source: \"http://x"));
            assertThat(e.getMessage(), containsString(file.getCanonicalPath()));
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAttributesFromNullFile() throws Exception {
        try {
            Attributes attributes = this.builder.getAttributes("http://x", null);
            fail("Should throw IOException");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("Source: \"http://x"));
        }
    }

}
