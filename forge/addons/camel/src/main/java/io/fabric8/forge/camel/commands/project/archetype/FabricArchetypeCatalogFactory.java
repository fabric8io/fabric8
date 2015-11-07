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
package io.fabric8.forge.camel.commands.project.archetype;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.VersionHelper;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactory;
import org.jboss.forge.furnace.services.Imported;

/**
 * The Fabric8 archetypes
 */
public class FabricArchetypeCatalogFactory implements ArchetypeCatalogFactory {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private static final String NAME = "fabric8";

    @Inject
    Imported<DependencyResolver> resolver;

    private ArchetypeCatalog cachedArchetypes;

    public String toString() {
        return NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
        if (cachedArchetypes == null) {
            String version = VersionHelper.fabric8ArchetypesVersion();

            Coordinate coordinate = CoordinateBuilder.create()
                    .setGroupId("io.fabric8.archetypes")
                    .setArtifactId("archetypes-catalog")
                    .setVersion(version)
                    .setPackaging("jar");

            // load the archetype-catalog.xml from inside the JAR
            Dependency dependency = resolver.get().resolveArtifact(DependencyQueryBuilder.create(coordinate));
            if (dependency != null) {
                try {
                    String name = dependency.getArtifact().getFullyQualifiedName();
                    URL url = new URL("file", null, name);
                    URLClassLoader loader = new URLClassLoader(new URL[]{url});
                    InputStream is = loader.getResourceAsStream("archetype-catalog.xml");
                    if (is != null) {
                        cachedArchetypes = new ArchetypeCatalogXpp3Reader().read(is);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error while retrieving archetypes", e);
                }
            }
        }
        return cachedArchetypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FabricArchetypeCatalogFactory that = (FabricArchetypeCatalogFactory) o;

        if (!NAME.equals(that.NAME)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return NAME.hashCode();
    }
}
