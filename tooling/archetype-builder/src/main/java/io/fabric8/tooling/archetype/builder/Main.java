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
package io.fabric8.tooling.archetype.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = ".";
            }

            File karafProfilesDir = new File(basedir, "../../fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/").getCanonicalFile();

            File catalogFile = new File(basedir, "target/classes/archetype-catalog.xml").getCanonicalFile();
            File quickStartJavaSrcDir = new File(basedir, "../../quickstarts/java").getCanonicalFile();
            File quickStartKarafSrcDir = new File(basedir, "../../quickstarts/karaf").getCanonicalFile();
            File quickStartKarafBeginnerSrcDir = new File(basedir, "../../quickstarts/karaf/beginner").getCanonicalFile();
            File quickStartKarafCxfSrcDir = new File(basedir, "../../quickstarts/karaf/cxf").getCanonicalFile();
            File quickStartMuleSrcDir = new File(basedir, "../../quickstarts/mule").getCanonicalFile();
            File quickStartSpringBootSrcDir = new File(basedir, "../../quickstarts/spring-boot").getCanonicalFile();
            File quickStartWarSrcDir = new File(basedir, "../../quickstarts/war").getCanonicalFile();
            File outputDir = args.length > 0 ? new File(args[0]) : new File(basedir, "../archetypes");
            ArchetypeBuilder builder = new ArchetypeBuilder(catalogFile);

            builder.configure();

            List<String> dirs = new ArrayList<>();
            try {
                builder.generateArchetypes("java", quickStartJavaSrcDir, outputDir, false, dirs, karafProfilesDir);
                builder.generateArchetypes("karaf", quickStartKarafSrcDir, outputDir, false, dirs, karafProfilesDir);
                builder.generateArchetypes("karaf", quickStartKarafBeginnerSrcDir, outputDir, false, dirs, karafProfilesDir);
                builder.generateArchetypes("karaf", quickStartKarafCxfSrcDir, outputDir, false, dirs, karafProfilesDir);
                builder.generateArchetypes("mule", quickStartMuleSrcDir, outputDir, false, dirs, karafProfilesDir);
                builder.generateArchetypes("springboot", quickStartSpringBootSrcDir, outputDir, false, dirs, karafProfilesDir);
                builder.generateArchetypes("war", quickStartWarSrcDir, outputDir, false, dirs, karafProfilesDir);
            } finally {
                LOG.debug("Completed the generation. Closing!");
                builder.close();
            }

            StringBuffer sb = new StringBuffer();
            for (String dir : dirs) {
                sb.append("\n\t<module>" + dir + "</module>");
            }
            LOG.info("Done creating archetypes:\n{}\n\n", sb.toString());

        } catch (Exception e) {
            LOG.error("Caught: " + e.getMessage(), e);
        }
    }

}
