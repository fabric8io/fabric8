/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.internal;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.DependencyTreeFilters;
import org.fusesource.fabric.fab.DependencyTreeResult;
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Filter;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Resolves the classpath using the FAB resolving mechanism
 */
public class FabClassPathResolver {
    private final FabConnection connection;
    private final Properties instructions;
    private final List<String> bundleClassPath;
    private final List<String> requireBundles;
    private boolean offline = false;
    private Filter<DependencyTree> sharedFilter;
    private List<DependencyTree> nonSharedDependencies = new ArrayList<DependencyTree>();
    private List<DependencyTree> sharedDependencies = new ArrayList<DependencyTree>();

    public FabClassPathResolver(FabConnection connection, Properties instructions, List<String> bundleClassPath, List<String> requireBundles) {
        this.connection = connection;
        this.instructions = instructions;
        this.bundleClassPath = bundleClassPath;
        this.requireBundles = requireBundles;

        String sharedFilterText = instructions.getProperty(ServiceConstants.INSTR_FAB_DEPENDENCY_SHARED, "");
        sharedFilter = DependencyTreeFilters.parse(sharedFilterText);
    }

    public void resolve() throws RepositoryException, IOException, XmlPullParserException {
        MavenResolver resolver = new MavenResolver();
        File fileJar = connection.getJarFile();
        DependencyTreeResult result = resolver.collectDependenciesForJar(fileJar, offline);

        addDependencies(result.getTree());

        // TODO now lets check that all the shared dependencies are available as OSGi bundles, installing them on the fly if not
        if (sharedDependencies.size() > 0) {
            System.out.println("ERROR: shared dependencies are currently not supported!: " + sharedDependencies);
        }

        for (DependencyTree dependencyTree : nonSharedDependencies) {
            if (dependencyTree.isValidLibrary()) {
                String url = dependencyTree.getUrl();
                if (url != null) {
                    bundleClassPath.add(url);
                }
            }
        }

        System.out.println("FAB resolved: bundleClassPath: " + bundleClassPath);
        System.out.println("FAB resolved requireBundles: " + requireBundles);
    }

    protected void addDependencies(DependencyTree tree) throws MalformedURLException {
        List<DependencyTree> children = tree.getChildren();
        for (DependencyTree child : children) {
            if (sharedFilter != null && sharedFilter.matches(child)) {
                // lets add all the transitive dependencies as shared
                sharedDependencies.addAll(child.getDescendants());
            } else {
                nonSharedDependencies.add(child);
                // we now need to recursively flatten all transitive dependencies (whether shared or not)
                addDependencies(child);
            }
        }
    }

}
