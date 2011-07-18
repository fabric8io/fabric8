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
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Filter;
import org.fusesource.fabric.fab.util.Manifests;
import org.fusesource.fabric.fab.util.Strings;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
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
import java.util.StringTokenizer;

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
    private List<DependencyTree> installDependencies = new ArrayList<DependencyTree>();
    private DependencyTree rootTree;
    // don't think there's any need to even look at Import-Packages as BND takes care of it...
    private boolean processImportPackages = false;
    private MavenResolver resolver;

    public FabClassPathResolver(FabConnection connection, Properties instructions, Map<String, Object> embeddedResources) {
        this.connection = connection;
        this.instructions = instructions;
        this.embeddedResources = embeddedResources;
        this.resolver = connection.getResolver();
    }


    public void resolve() throws RepositoryException, IOException, XmlPullParserException, BundleException {
        PomDetails pomDetails = connection.resolvePomDetails();
        if (!pomDetails.isValid()) {
            LOG.warn("Cannot resolve pom.xml for " + connection.getJarFile());
            return;
        }
        DependencyTreeResult result = resolver.collectDependencies(pomDetails, offline);
        this.rootTree = result.getTree();

        String sharedFilterText = getManfiestProperty(ServiceConstants.INSTR_FAB_PROVIDED_DEPENDENCY);
        String requireBundleFilterText = getManfiestProperty(ServiceConstants.INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE);
        String excludeFilterText = getManfiestProperty(ServiceConstants.INSTR_FAB_EXCLUDE_DEPENDENCY);
        String optionalDependencyText = getManfiestProperty(ServiceConstants.INSTR_FAB_OPTIONAL_DEPENDENCY);

        sharedFilter = DependencyTreeFilters.parseShareFilter(sharedFilterText);
        requireBundleFilter = DependencyTreeFilters.parseRequireBundleFilter(requireBundleFilterText);
        excludePackageFilter = DependencyTreeFilters.parseExcludeFilter(excludeFilterText, optionalDependencyText);

        bundleClassPath.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH), ","));
        requireBundles.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE), ","));
        if (processImportPackages) {
            importPackages.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_IMPORT_PACKAGE), ","));
        }


        String name = getManfiestProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME);
        if (name.length() <= 0) {
            name = rootTree.getBundleSymbolicName();
            instructions.setProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME, name);
        }
        addDependencies(rootTree);

        // lets process any extension modules
        String extensionPropertyName = getManfiestProperty(ServiceConstants.INSTR_FAB_EXTENSION_VARIABLE);
        if (Strings.notEmpty(extensionPropertyName)) {
            resolveExtensions(extensionPropertyName, rootTree);
        }


        for (DependencyTree dependencyTree : sharedDependencies) {
            if (requireBundleFilter.matches(dependencyTree)) {
                // lets figure out the bundle ID etc...
                String bundleId = dependencyTree.getBundleSymbolicName();
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

        if (connection.getConfiguration().isInstallMissingDependencies()) {
            installMissingDependencies();
        } else {
            LOG.info("Not installing dependencies as not enabled");
        }
    }

    protected void installMissingDependencies() throws IOException, BundleException {
        BundleContext bundleContext = connection.getBundleContext();
        if (bundleContext == null) {
            LOG.warn("No BundleContext available so cannot install provided dependencies");
        } else {
            List<Bundle> toStart = new ArrayList<Bundle>();
            for (DependencyTree dependency : installDependencies) {
                String name = dependency.getBundleSymbolicName();
                if (Bundles.isInstalled(bundleContext, name)) {
                    LOG.info("Bundle already installed: " + name);
                } else {
                    URL url = dependency.getJarURL();
                    String installUri = url.toExternalForm();
                    try {
                        Bundle bundle = null;
                        if (!dependency.isBundle()) {
                            FabConnection childConnection = connection.createChild(url);
                            PomDetails pomDetails = childConnection.resolvePomDetails();
                            if (pomDetails != null && pomDetails.isValid()) {
                                // lets make sure we use a FAB to deploy it
                                LOG.info("Installing fabric bundle: " + name + " from: " + installUri);
                                bundle = bundleContext.installBundle(installUri, childConnection.getInputStream());
                            } else {
                                LOG.warn("Could not deduce the pom.xml for the jar " + installUri + " so cannot treat as FAB");
                            }
                        } else {
                            LOG.info("Installing bundle: " + name + " from: " + installUri);
                            bundle = bundleContext.installBundle(installUri);
                        }
                        if (bundle != null && connection.isStartInstalledDependentBundles()) {
                            toStart.add(bundle);
                        }
                    } catch (BundleException e) {
                        LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                        throw e;
                    } catch (IOException e) {
                        LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                        throw e;
                    } catch (RuntimeException e) {
                        LOG.error("Failed to deploy " + installUri + " due to error: " + e, e);
                        throw e;
                    }
                }
            }
            // now lets start the installed bundles
            for (Bundle bundle : toStart) {
                try {
                    bundle.start();
                } catch (BundleException e) {
                    LOG.warn("Failed to start " + bundle.getSymbolicName() + ". Reason: " + e, e);
                }
            }
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


    /**
     * Lets use the given property name to find any extension dependencies to add to this flat class loader
     */
    protected void resolveExtensions(String extensionPropertyName, DependencyTree root) throws IOException, RepositoryException, XmlPullParserException {
        String value = resolvePropertyName(extensionPropertyName);
        // TODO dirty hack - how should this really work???
        if (value == null) {
            value = System.getProperty(extensionPropertyName);
        }
        LOG.info("Fabric resolved extension variable '" + extensionPropertyName + "' to extensions: " + value);

        if (Strings.notEmpty(value)) {
            StringTokenizer iter = new StringTokenizer(value, ",");
            while (iter.hasMoreElements()) {
                String text = iter.nextToken().trim();
                if (Strings.notEmpty(text)) {
                    String[] values = text.split(":");
                    String archetypeId;
                    String groupId = root.getGroupId();
                    String version = root.getVersion();
                    String extension = "jar";
                    String classifier = "";
                    if (values.length == 1) {
                        archetypeId = values[0];
                    } else if (values.length == 2) {
                        groupId = values[0];
                        archetypeId = values[1];
                    } else if (values.length > 2) {
                        groupId = values[0];
                        archetypeId = values[1];
                        version = values[2];
                    } else {
                        continue;
                    }

                    // lets resolve the dependency
                    DependencyTreeResult result = resolver.collectDependencies(groupId, archetypeId, version, extension, classifier);
                    if (result != null) {
                        DependencyTree tree = result.getTree();
                        LOG.debug("Adding extensions: " + tree);
                        addExtensionDependencies(tree);
                    }

                }

            }
        }
    }

    protected String resolvePropertyName(String extensionPropertyName) {
        // TODO whats the right way to do this???
        return connection.getConfiguration().get(extensionPropertyName);
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
                addSharedDependency(child);
            } else {
                nonSharedDependencies.add(child);
                // we now need to recursively flatten all transitive dependencies (whether shared or not)
                addDependencies(child);
            }
        }
    }

    protected void addExtensionDependencies(DependencyTree child) throws MalformedURLException {
        if (excludePackageFilter.matches(child)) {
            // ignore
            LOG.debug("Excluded dependency: " + child);
        } else if (sharedFilter.matches(child) || requireBundleFilter.matches(child)) {
            // lets add all the transitive dependencies as shared
            addSharedDependency(child);
        } else {
            nonSharedDependencies.add(child);
            // we now need to recursively flatten all transitive dependencies (whether shared or not)
            addDependencies(child);
        }
    }

    protected void addSharedDependency(DependencyTree child) {
        sharedDependencies.add(child);
        List<DependencyTree> list = child.getDescendants();
        for (DependencyTree grandChild : list) {
            if (excludePackageFilter.matches(grandChild)) {
                LOG.debug("Excluded transitive dependency: " + grandChild);
                continue;
            } else {
                sharedDependencies.add(grandChild);
            }
        }
        addInstallDependencies(child);
    }

    protected void addInstallDependencies(DependencyTree node) {
        // we only transitively walk bundle dependencies as any non-bundle
        // dependencies will be installed using FAB which will deal with
        // non-shared or shared transitive dependencies
        if (node.isBundle()) {
            List<DependencyTree> list = node.getChildren();
            for (DependencyTree child : list) {
                if (excludePackageFilter.matches(child)) {
                    continue;
                } else {
                    addInstallDependencies(child);
                }
            }
        }
        // lets add the child dependencies first so we add things in the right order
        installDependencies.add(node);
    }

}
