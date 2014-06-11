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
            File catalogFile = new File(basedir, "target/archetype-catalog.xml").getCanonicalFile();
            File quickStartSrcDir = new File(basedir, "../../quickstarts").getCanonicalFile();
            File quickStartBeginnerSrcDir = new File(basedir, "../../quickstarts/beginner").getCanonicalFile();
            File quickStartSpringBootSrcDir = new File(basedir, "../../quickstarts/spring-boot").getCanonicalFile();
            File outputDir = args.length > 0 ? new File(args[0]) : new File(basedir, "../archetypes");
            ArchetypeBuilder builder = new ArchetypeBuilder(catalogFile);

            builder.configure();
            try {
                builder.generateArchetypes(quickStartSrcDir, outputDir, false);
                builder.generateArchetypes(quickStartBeginnerSrcDir, outputDir, false);
                builder.generateArchetypes(quickStartSpringBootSrcDir, outputDir, false);
            } finally {
                LOG.info("Completed the generation. Closing!");
                builder.close();
            }
        } catch (Exception e) {
            LOG.error("Caught: " + e.getMessage(), e);
        }
    }

}
