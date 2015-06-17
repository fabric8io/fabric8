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
package io.fabric8.tooling.archetype.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try {
            String basedir = System.getProperty("basedir");
            if (Strings.isNullOrBlank(basedir)) {
                basedir = ".";
            }

            String sourcedir = System.getProperty("sourcedir");
            if (Strings.isNullOrBlank(sourcedir)) {
                throw new IllegalArgumentException("No sourcedir system property");
            }

            File bomFile = new File(basedir, System.getProperty("rootPomFile", "../pom.xml"));
            File catalogFile = new File(basedir, "target/classes/archetype-catalog.xml").getCanonicalFile();
            catalogFile.getParentFile().mkdirs();


            String outputPath = System.getProperty("outputdir");
            File outputDir = Strings.isNotBlank(outputPath) ? new File(outputPath) : new File(basedir);

            ArchetypeBuilder builder = new ArchetypeBuilder(catalogFile);
            builder.setBomFile(bomFile);
            builder.configure();

            List<String> dirs = new ArrayList<>();
            try {
                File sourceDirectory = new File(sourcedir);
                if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
                    throw new IllegalArgumentException("Source directory: " + sourcedir + " is not a valid directory");
                }
                builder.generateArchetypes("", sourceDirectory, outputDir, false, dirs);
            } finally {
                LOG.debug("Completed the generation. Closing!");
                builder.close();
            }

            StringBuffer sb = new StringBuffer();
            for (String dir : dirs) {
                sb.append("\n\t<module>" + dir + "</module>");
            }
            System.out.println("Done creating archetypes:\n" + sb + "\n");

        } catch (Exception e) {
            System.out.println("Caught: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
