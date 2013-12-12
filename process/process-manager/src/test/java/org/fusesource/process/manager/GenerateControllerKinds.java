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

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class GenerateControllerKinds {
    public static void main(String[] args) {
        try {
            new GenerateControllerKinds().run();
        } catch (Exception e) {
            System.out.println("Failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() throws Exception {
        File baseDir = new File(System.getProperty("basedir", "."));
        File srcDir = new File(baseDir, "src/main/resources");
        assertTrue("source dir doesn't exist! " + srcDir, srcDir.exists());
        assertTrue("source folder is not a directory " + srcDir, srcDir.isDirectory());

        File classesDir = new File(baseDir, "target/classes");
        File outFile = new File(classesDir, "org/fusesource/process/manager/controllerKinds");
        outFile.getParentFile().mkdirs();

        System.out.println("Generating controller kinds file: " + outFile);
        FileWriter writer = null;
        try {
            writer = new FileWriter(outFile);

            File[] list = srcDir.listFiles();
            String postfix = ".json";
            List<String> kinds = Lists.newArrayList();
            assertNotNull("No JSON files found in source dir: " + srcDir, list);
            for (File file : list) {
                if (!file.isFile()) continue;
                String name = file.getName();
                if (name.endsWith(postfix)) {
                    String kind = name.substring(0, name.length() - postfix.length());
                    kinds.add(kind);
                    writer.write(kind + "\n");
                }
            }

            System.out.println("Found controller kinds: " + kinds);
        } finally {
            Closeables.closeQuietly(writer);
        }

        // lets try find the process tarball
        String groupId = System.getProperty("groupId", "org.fusesource.process");
        String artifactId = "process-launcher";
        String version = System.getProperty("version", "99-master-SNAPSHOT");
        String classifier = "bin";
        String extension = "tar.gz";

        String name = groupId + "/" + artifactId + "/" + version;
        System.out.println("Loading bundle: " + name);

        File file = new File(baseDir + "/../process-launcher/target/" + artifactId + "-" + version + "-bin.tar.gz");

        assertNotNull("Cannot find the file for " + name + " using " + file.getPath(), file);
        File newFile = new File(classesDir, "process-launcher.tar.gz");
        Files.move(file, newFile);
        System.out.println("Moved process launch tarball to " + newFile);
    }
}
