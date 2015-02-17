/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactory;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.furnace.container.cdi.events.Local;
import org.jboss.forge.furnace.event.PostStartup;
import org.jboss.forge.furnace.util.Strings;

/**
 * The Apache Camel archetypes
 */
public class CamelArchetypeCatalogFactory implements ArchetypeCatalogFactory {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private ArchetypeCatalog cachedArchetypes;

    // TODO: Remove when when using Forge 2.14.1+
    void startup(@Observes @Local PostStartup startup, ArchetypeCatalogFactoryRegistry registry) {
        registry.addArchetypeCatalogFactory(this);
    }

    @Override
    public String getName() {
        return "camel";
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
        if (cachedArchetypes == null) {
            URL url = null;
            try {
                url = new URL("http://repo2.maven.org/maven2/archetype-catalog.xml");
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "URL should be valid", e);
                // ignore
            }

            String defaultRepository = "http://repo2.maven.org/maven2";

            // TODO: ideally Apache Camel has its own catalog we can use, and load from the offline camel-catalog JAR
            // https://issues.apache.org/jira/browse/CAMEL-8365
            if (url != null) {
                try (InputStream urlStream = url.openStream()) {
                    cachedArchetypes = new ArchetypeCatalog();

                    ArchetypeCatalog catalog = new ArchetypeCatalogXpp3Reader().read(urlStream);
                    for (Archetype archetype : catalog.getArchetypes()) {
                        // only include camel
                        if ("org.apache.camel.archetypes".equals(archetype.getArtifactId())) {
                            if (Strings.isNullOrEmpty(archetype.getRepository())) {
                                archetype.setRepository(defaultRepository);
                            }
                            cachedArchetypes.addArchetype(archetype);
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error while retrieving archetypes", e);
                }
            }
        }
        return cachedArchetypes;
    }

}
