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
import org.jboss.gravia.utils.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class ManifestTesty {
    String basedir = System.getProperty("basedir", ".");
    File target = new File(basedir, "target");

    //@Ignore
    @Test
    public void testWarManifest() throws Exception {
        File[] files = target.listFiles();
        assertNotNull("No files found in " + target, files);
        assertTrue("Should have more than one file inside dir: " + target, files.length > 0);

        File war = null;
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".war")) {
                war = file;
            }
        }
        assertNotNull("should have found a war!", war);
        System.out.println("Loading manifest of " + war.getCanonicalPath());

        ZipFile zip = new ZipFile(war);
        ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
        assertNotNull("No zip entry for manifest", entry);
        InputStream inputStream = zip.getInputStream(entry);
        assertNotNull("No zip stream for manifest", inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOUtils.copyStream(inputStream, buffer);
        String text = buffer.toString();
        System.out.println("Loaded manifest: " + text);
        Manifest manifest = new Manifest(new ByteArrayInputStream(buffer.toByteArray()));
/*
        URLConnection urlConnection = war.toURI().toURL().openConnection();
        URLClassLoader classLoader = new URLClassLoader(new URL[] {war.toURI().toURL()});
        InputStream resourceAsStream = classLoader.getResourceAsStream("META-INF/MANIFEST.MF");
        assertNotNull("No manifest in the WAR", resourceAsStream);
        Manifest manifest = new Manifest(resourceAsStream);
*/

/*
        assertTrue("should be a JarURLConnection", urlConnection instanceof JarURLConnection);
        JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
        Manifest manifest = jarURLConnection.getManifest();
*/
/*
        JarFile jarFile = new JarFile(war);
        Manifest manifest = jarFile.getManifest();
*/
        assertNotNull("should have a manifest in " + war, manifest);
        validateManifest(manifest);
    }

    @Test
    public void testGeneratedManifest() throws Exception {
        File file = new File(target, "generated-manifest/MANIFEST.MF");
        Manifest manifest = new Manifest(new FileInputStream(file));
/*
        URLConnection urlConnection = war.toURI().toURL().openConnection();
        URLClassLoader classLoader = new URLClassLoader(new URL[] {war.toURI().toURL()});
        InputStream resourceAsStream = classLoader.getResourceAsStream("META-INF/MANIFEST.MF");
        assertNotNull("No manifest in the WAR", resourceAsStream);
        Manifest manifest = new Manifest(resourceAsStream);
*/

/*
        assertTrue("should be a JarURLConnection", urlConnection instanceof JarURLConnection);
        JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
        Manifest manifest = jarURLConnection.getManifest();
*/
/*
        JarFile jarFile = new JarFile(war);
        Manifest manifest = jarFile.getManifest();
*/
        assertNotNull("should have a manifest in " + file, manifest);
        validateManifest(manifest);
    }

    protected void validateManifest(Manifest manifest) {
        Attributes mainAttributes = manifest.getMainAttributes();
/*
        printAttributes("  Main; ", mainAttributes);
        Map<String, Attributes> attrEntries = manifest.getEntries();
        for (Map.Entry<String, Attributes> attrEntry : attrEntries.entrySet()) {
            printAttributes("  entry " + attrEntry.getKey(), attrEntry.getValue());
        }
*/
        System.out.println("Gravia-Identity: " + mainAttributes.getValue("Gravia-Identity"));
        System.out.println("Service-Component: " + mainAttributes.getValue("Service-Component"));
    }

    protected static void printAttributes(String prefix, Attributes attributes) {
        Set<Map.Entry<Object, Object>> entries = attributes.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            System.out.println(prefix + " " + entry.getKey() + ": " + entry.getValue());
        }
    }

}
