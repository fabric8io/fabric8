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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
        assert srcDir.exists() : "source dir doesn't exist! " + srcDir;
        assert srcDir.isDirectory() : "source folder is not a directory " + srcDir;

        File outFile = new File(baseDir, "target/classes/org/fusesource/process/manager/controllerKinds");
        outFile.getParentFile().mkdirs();

        System.out.println("Generating controller kinds file: " + outFile);
        FileWriter writer = null;
        try {
            writer = new FileWriter(outFile);

            File[] list = srcDir.listFiles();
            String postfix = ".json";
            List<String> kinds = Lists.newArrayList();
            assert list != null : "No JSON files found in source dir: " + srcDir;
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
    }
}
