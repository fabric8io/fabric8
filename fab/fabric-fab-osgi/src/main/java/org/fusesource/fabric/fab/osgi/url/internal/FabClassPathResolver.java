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
import org.fusesource.fabric.fab.util.Manifests;
import org.fusesource.fabric.fab.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Resolves the classpath using the FAB resolving mechanism
 */
public class FabClassPathResolver {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabClassPathResolver.class);

    private final FabConnection connection;
    private final Properties instructions;
    private final Map<String, Object> embeddedResources;
    private final List<String> bundleClassPath = new ArrayList<String>();
    private final List<String> requireBundles = new ArrayList<String>();
    private final List<String> importPackages = new ArrayList<String>();
    private boolean offline = false;
    private Filter<DependencyTree> sharedFilter;
    private Filter<DependencyTree> requireBundleFilter;
    private Filter<DependencyTree> excludePackageFilter;
    private List<DependencyTree> nonSharedDependencies = new ArrayList<DependencyTree>();
    private List<DependencyTree> sharedDependencies = new ArrayList<DependencyTree>();
    private DependencyTree rootTree;
    // don't think there's any need to even look at Import-Packages as BND takes care of it...
    boolean processImportPackages = false;

    public FabClassPathResolver(FabConnection connection, Properties instructions, Map<String, Object> embeddedResources) {
        this.connection = connection;
        this.instructions = instructions;
        this.embeddedResources = embeddedResources;
    }

    public void resolve() throws RepositoryException, IOException, XmlPullParserException {
        MavenResolver resolver = new MavenResolver();
        String[] repositories = connection.getConfiguration().getMavenRepositories();
        if (repositories != null) {
            resolver.setRepositories(repositories);
        }
        File fileJar = connection.getJarFile();
        DependencyTreeResult result = resolver.collectDependenciesForJar(fileJar, offline);
        this.rootTree = result.getTree();

        String sharedFilterText = getManfiestProperty(ServiceConstants.INSTR_FAB_PROVIDED_DEPENDENCY);
        String requireBundleFilterText = getManfiestProperty(ServiceConstants.INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE);
        String excludeFilterText = getManfiestProperty(ServiceConstants.INSTR_FAB_EXCLUDE_DEPENDENCY);
        String optionalDependencyText = getManfiestProperty(ServiceConstants.INSTR_FAB_OPTIONAL_DEPENDENCY);

        sharedFilter = DependencyTreeFilters.parseShareFilter(sharedFilterText);
        requireBundleFilter = DependencyTreeFilters.parseImportPackageFilter(requireBundleFilterText);
        excludePackageFilter = DependencyTreeFilters.parseExcludeFilter(excludeFilterText, optionalDependencyText);

        bundleClassPath.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH), ","));
        requireBundles.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE), ","));
        if (processImportPackages) {
            importPackages.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_IMPORT_PACKAGE), ","));
        }


        String name = getManfiestProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME);
        if (name.length() <= 0) {
            name = rootTree.getBundleId();
            instructions.setProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME, name);
        }
        addDependencies(rootTree);

        for (DependencyTree dependencyTree : sharedDependencies) {
            if (requireBundleFilter.matches(dependencyTree)) {
                // lets figure out the bundle ID etc...
                String bundleId = dependencyTree.getBundleId();
                // TODO add a version range...
                requireBundles.add(bundleId);
            } else {
                // TODO don't think we need to do anything now since already the BND stuff figures out the import packages for any missing stuff?
                if (processImportPackages) {
                    // lets add all the import packages...
                    String text = dependencyTree.getManfiestEntry(ServiceConstants.INSTR_EXPORT_PACKAGE);
                    if (text != null && text.length() > 0) {
                        List<String> list = new ArrayList<String>();
                        list.addAll(Strings.splitAsList(text, ","));
                        // TODO filter out duplicates
                        importPackages.addAll(list);
                    }
                }
            }
        }

        for (DependencyTree dependencyTree : nonSharedDependencies) {
            if (dependencyTree.isValidLibrary()) {
                String url = dependencyTree.getUrl();
                if (url != null) {
                    String path = dependencyTree.getGroupId() + "." + dependencyTree.getArtifactId() + ".jar";

                    if (!bundleClassPath.contains(path)) {
                        // try use a file if it exists
                        File file = new File(url);
                        if (file.exists()) {
                            embeddedResources.put(path, file);
                        } else {
                            embeddedResources.put(path, new URL(url));
                        }
                        if (bundleClassPath.isEmpty()) {
                            bundleClassPath.add(".");
                        }
                        bundleClassPath.add(path);
                    }
                }
            }
        }

        LOG.debug("resolved: bundleClassPath: " + Strings.join(bundleClassPath, "\t\n"));
        LOG.debug("resolved: requireBundles: " + Strings.join(requireBundles, "\t\n"));
        if (processImportPackages) {
            LOG.debug("resolved: importPackages: " + Strings.join(importPackages, "\t\n"));
        }

        instructions.setProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH, Strings.join(bundleClassPath, ","));
        instructions.setProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE, Strings.join(requireBundles, ","));
        if (processImportPackages) {
            instructions.setProperty(ServiceConstants.INSTR_IMPORT_PACKAGE, Strings.join(importPackages, ","));
        }
    }

    public boolean isProcessImportPackages() {
        return processImportPackages;
    }

    protected String getManfiestProperty(String name) {
        String answer = null;
        try {
            if (true) {
                // TODO do some caching!!!
                 answer = Manifests.getManfiestEntry(connection.getJarFile(), name);
            } else {
                answer = instructions.getProperty(name, "");
            }
        } catch (IOException e) {
            // TODO warn
        }
        if (answer == null) {
            answer = "";
        }
        return answer;
    }

    protected void addDependencies(DependencyTree tree) throws MalformedURLException {
        List<DependencyTree> children = tree.getChildren();
        for (DependencyTree child : children) {
            if (excludePackageFilter.matches(child)) {
                // ignore
                LOG.debug("Excluded dependency: " + child);
                continue;
            } else if (sharedFilter.matches(child) || requireBundleFilter.matches(child)) {
                // lets add all the transitive dependencies as shared
                sharedDependencies.add(child);
                List<DependencyTree> list = child.getDescendants();
                for (DependencyTree grandChild : list) {
                    if (excludePackageFilter != null && excludePackageFilter.matches(tree)) {
                        LOG.debug("Excluded transitive dependency: " + child);
                        continue;
                    } else {
                        sharedDependencies.add(grandChild);
                    }
                }
            } else {
                nonSharedDependencies.add(child);
                // we now need to recursively flatten all transitive dependencies (whether shared or not)
                addDependencies(child);
            }
        }
    }
}
