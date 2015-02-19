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

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;

import org.apache.camel.catalog.DefaultCamelComponentCatalog;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactory;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.furnace.container.cdi.events.Local;
import org.jboss.forge.furnace.event.PostStartup;

/**
 * The Apache Camel archetypes
 */
public class CamelArchetypeCatalogFactory implements ArchetypeCatalogFactory {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private ArchetypeCatalog cachedArchetypes;

    void startup(@Observes @Local PostStartup startup, ArchetypeCatalogFactoryRegistry registry) {
        // must use this to trigger startup event so we can add ourselves
        if (registry.getArchetypeCatalogFactory("camel") == null) {
            registry.addArchetypeCatalogFactory(this);
        }
    }

    @Override
    public String getName() {
        return "camel";
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
        if (cachedArchetypes == null) {
            // use the camel catalog to load the archetypes
            String xml = new DefaultCamelComponentCatalog().archetypeCatalogAsXml();
            if (xml != null) {
                try {
                    cachedArchetypes = new ArchetypeCatalogXpp3Reader().read(new StringReader(xml));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error while retrieving archetypes", e);
                }
            }
        }
        return cachedArchetypes;
    }

}
