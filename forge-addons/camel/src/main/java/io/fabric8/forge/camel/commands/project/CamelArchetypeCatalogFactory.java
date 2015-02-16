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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactory;

/**
 * The Apache Camel archetypes
 */
public class CamelArchetypeCatalogFactory implements ArchetypeCatalogFactory {

    private DependencyResolver resolver;

    public CamelArchetypeCatalogFactory(DependencyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String getName() {
        return "camel";
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
        ArchetypeCatalog catalog = new ArchetypeCatalog();

        try {
            Set<Dependency> deps = resolver.resolveDependencies(DependencyQueryBuilder.create("org.apache.camel.archetypes::[2.4.1]"));
            for (Dependency dep : deps) {
                Archetype arc = new Archetype();
                arc.setGroupId(dep.getCoordinate().getGroupId());
                arc.setArtifactId(dep.getCoordinate().getArtifactId());
                arc.setVersion(dep.getCoordinate().getVersion());
                // maven central
                arc.setRepository("http://repo2.maven.org/maven2/");
                catalog.addArchetype(arc);
            }
        } catch (Throwable e) {
            e.printStackTrace();

            // thanks cant see the fucking exceptions
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String s = sw.toString();
            System.out.println(s);
        }

        return catalog;
    }
}
