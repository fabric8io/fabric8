/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.felix.utils.version.VersionCleaner;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.DependencyTreeFilters;
import org.fusesource.fabric.fab.DependencyTreeResult;
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.ModuleDescriptor;
import org.fusesource.fabric.fab.ModuleRegistry;
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Filter;
import org.fusesource.fabric.fab.util.IOHelpers;
import org.fusesource.fabric.fab.util.Manifests;
import org.fusesource.fabric.fab.util.Strings;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;

import static org.fusesource.fabric.fab.ModuleDescriptor.FAB_MODULE_DESCRIPTION;
import static org.fusesource.fabric.fab.ModuleDescriptor.FAB_MODULE_ID;
import static org.fusesource.fabric.fab.ModuleDescriptor.FAB_MODULE_NAME;
import static org.fusesource.fabric.fab.ModuleDescriptor.FAB_MODULE_PROPERTIES;
import static org.fusesource.fabric.fab.util.Strings.emptyIfNull;

/**
 * Resolves the classpath using the FAB resolving mechanism
 */
public class FabClassPathResolver {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabClassPathResolver.class);

    private FabConnection connection;
    private Properties instructions;
    private Map<String, Object> embeddedResources;
    private HashMap<String, DependencyTree> dependenciesByPackage = new HashMap<String, DependencyTree>();
    private ModuleRegistry moduleRegistry;
    private List<String> bundleClassPath = new ArrayList<String>();
    private List<String> requireBundles = new ArrayList<String>();
    private List<String> importPackages = new ArrayList<String>();
    private boolean offline = false;
    private Filter<DependencyTree> sharedFilter;
    private Filter<DependencyTree> requireBundleFilter;
    private Filter<DependencyTree> excludePackageFilter;
    private Filter<DependencyTree> excludeOptionalFilter;

    private List<DependencyTree> nonSharedDependencies = new ArrayList<DependencyTree>();
    private List<DependencyTree> sharedDependencies = new ArrayList<DependencyTree>();

    private List<DependencyTree> installDependencies = new ArrayList<DependencyTree>();
    private List<DependencyTree> optionalDependencies = new ArrayList<DependencyTree>();
    private DependencyTree rootTree;
    // don't think there's any need to even look at Import-Packages as BND takes care of it...
    private boolean processImportPackages = false;
    private MavenResolver resolver;
    private VersionedDependencyId moduleId;

    public FabClassPathResolver(FabConnection connection, Properties instructions, Map<String, Object> embeddedResources) {
        this.connection = connection;
        this.instructions = instructions;
        this.embeddedResources = embeddedResources;
        this.moduleRegistry = Activator.registry;
        this.resolver = connection.getResolver();
    }

    public List<DependencyTree> getInstallDependencies() {
        return installDependencies;
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
        excludePackageFilter = DependencyTreeFilters.parseExcludeFilter(excludeFilterText);
        excludeOptionalFilter = DependencyTreeFilters.parseExcludeOptionalFilter(optionalDependencyText);

        bundleClassPath.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH), ","));
        requireBundles.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE), ","));
        importPackages.addAll(Strings.splitAsList(getManfiestProperty(ServiceConstants.INSTR_IMPORT_PACKAGE), ","));


        String name = getManfiestProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME);
        if (name.length() <= 0) {
            name = rootTree.getBundleSymbolicName();
            instructions.setProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME, name);
        }
        addDependencies(rootTree);


        // Build a ModuleDescriptor using the Jar Manifests headers..
        Model model = pomDetails.getModel();
        moduleId = new VersionedDependencyId(model);
        ModuleRegistry.VersionedModule module = moduleRegistry.getVersionedModule(moduleId);
        if( module==null || module.getFile()!=null ) {
            registerModule(model);
        }
        resolveExtensions(model, rootTree);

        for (DependencyTree dependencyTree : sharedDependencies) {
            if (requireBundleFilter.matches(dependencyTree)) {
                // lets figure out the bundle ID etc...
                String bundleId = dependencyTree.getBundleSymbolicName();
                Version version = new Version(VersionCleaner.clean(dependencyTree.getVersion()));
                requireBundles.add(bundleId + ";bundle-version=" + version + "");
            } else {
                // TODO don't think we need to do anything now since already the BND stuff figures out the import packages for any missing stuff?
                if (processImportPackages) {
                    // lets add all the import packages...
                    importAllExportedPackages(dependencyTree);
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
                        addBundleClassPath(path);
                    }
                }
            }
        }

        LOG.debug("resolved: bundleClassPath: " + Strings.join(bundleClassPath, "\t\n"));
        LOG.debug("resolved: requireBundles: " + Strings.join(requireBundles, "\t\n"));
        LOG.debug("resolved: importPackages: " + Strings.join(importPackages, "\t\n"));

        instructions.setProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH, Strings.join(bundleClassPath, ","));
        instructions.setProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE, Strings.join(requireBundles, ","));
        instructions.setProperty(ServiceConstants.INSTR_FAB_MODULE_ID, moduleId.toString());

        // adding import package statements causes the Bnd analyzer to not run so lets not do that :)
        instructions.setProperty(ServiceConstants.INSTR_IMPORT_PACKAGE, Strings.join(importPackages, ","));
    }

    private void registerModule(Model model) throws IOException, XmlPullParserException {
        try {
            Properties moduleProperties = new Properties();
            for( String key: FAB_MODULE_PROPERTIES) {
                String value = getManfiestProperty("Fabric-"+key);
                if( Strings.notEmpty(value) ) {
                    moduleProperties.setProperty(key, value);
                }
            }
            // Enhance with maven pom information
            if( !moduleProperties.containsKey(FAB_MODULE_ID) ) {
                moduleProperties.setProperty(FAB_MODULE_ID, moduleId.toString());
            }
            if( !moduleProperties.containsKey(FAB_MODULE_NAME) ) {
                moduleProperties.setProperty(FAB_MODULE_NAME, model.getArtifactId());
            }
            if( !moduleProperties.containsKey(FAB_MODULE_DESCRIPTION) ) {
                moduleProperties.setProperty(FAB_MODULE_DESCRIPTION, emptyIfNull(model.getDescription()));
            }

            ModuleDescriptor descriptor = ModuleDescriptor.fromProperties(moduleProperties);
            moduleRegistry.add(descriptor);

        } catch (Exception e) {
            System.err.println("Failed to register the fabric module for: "+moduleId);
            e.printStackTrace();
        }
    }

    protected void resolveExtensions(Model model, DependencyTree root) throws IOException, RepositoryException, XmlPullParserException {
        ModuleRegistry.VersionedModule module = moduleRegistry.getVersionedModule(moduleId);
        if( module!=null ) {
            Map<String, ModuleRegistry.VersionedModule> availableExtensions = module.getAvailableExtensions();
            String extensionsString="";
            for (String enabledExtension : module.getEnabledExtensions()) {
                ModuleRegistry.VersionedModule extensionModule = availableExtensions.get(enabledExtension);
                if( extensionModule!=null ) {
                    VersionedDependencyId id = extensionModule.getId();

                    // lets resolve the dependency
                    DependencyTreeResult result = resolver.collectDependencies(id.getGroupId(), id.getArtifactId(), id.getVersion(), id.getExtension(), id.getClassifier());
                    if (result != null) {
                        DependencyTree tree = result.getTree();
                        LOG.debug("Adding extensions: " + tree);
                        if( extensionsString.length()!=0 ) {
                            extensionsString += " ";
                        }
                        extensionsString += id;
                        addExtensionDependencies(tree);
                    } else {
                        LOG.debug("Could not resolve extension: " + id);
                    }
                }
            }
            if( extensionsString.length()!= 0 ) {
                instructions.put(ServiceConstants.INSTR_FAB_MODULE_ENABLED_EXTENSIONS, extensionsString);
            }
        }
    }

    protected void importAllExportedPackages(DependencyTree dependencyTree) {
        String text = dependencyTree.getManfiestEntry(ServiceConstants.INSTR_EXPORT_PACKAGE);
        if (text != null && text.length() > 0) {
            List<String> list = new ArrayList<String>();
            list.addAll(Strings.splitAsList(text, ","));
            // TODO filter out duplicates
            importPackages.addAll(list);
        }
    }


    public boolean isProcessImportPackages() {
        return processImportPackages;
    }

    private Manifest manfiest;

    Manifest getManifest() {
        if( manfiest == null ) {
            try {
                manfiest = Manifests.getManfiest(connection.getJarFile());
            } catch (IOException e) {
                // TODO: warn
                manfiest = new Manifest();
            }
        }
        return manfiest;
    }

    protected String getManfiestProperty(String name) {
        String answer = null;
        if (true) {
            // TODO do some caching!!!
            answer = getManifest().getMainAttributes().getValue(name);
        } else {
            answer = instructions.getProperty(name, "");
        }
        if (answer == null) {
            answer = "";
        }
        return answer;
    }

    protected void addPackages(DependencyTree tree) {
        try {
            for(String p: tree.getPackages() ) {
                if( !dependenciesByPackage.containsKey(p) ) {
                    dependenciesByPackage.put(p, tree);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addDependencies(DependencyTree tree) throws IOException {
        addPackages(tree);
        List<DependencyTree> children = tree.getChildren();
        for (DependencyTree child : children) {
            if (excludePackageFilter.matches(child)) {
                // ignore
                LOG.debug("Excluded dependency: " + child);
                continue;
            } else if (excludeOptionalFilter.matches(child)) {
                addOptionalDependency(child);
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

    private void addOptionalDependency(DependencyTree tree) {
        addPackages(tree);
        optionalDependencies.add(tree);
        List<DependencyTree> list = tree.getDescendants();
        for (DependencyTree child : list) {
            if (excludePackageFilter.matches(child)) {
                LOG.debug("Excluded transitive dependency: " + child);
                continue;
            } else {
                addOptionalDependency(child);
            }
        }
    }

    protected void addExtensionDependencies(DependencyTree child) throws IOException {
        if (excludePackageFilter.matches(child)) {
            // ignore
            LOG.debug("Excluded dependency: " + child);
        } else if (excludeOptionalFilter.matches(child)) {
            LOG.debug("Excluded optional dependency: " + child);
        } else if (sharedFilter.matches(child) || requireBundleFilter.matches(child)) {
            // lets add all the transitive dependencies as shared
            addSharedDependency(child);
        } else {
            nonSharedDependencies.add(child);
            // we now need to recursively flatten all transitive dependencies (whether shared or not)
            addDependencies(child);
        }
    }

    protected void addSharedDependency(DependencyTree tree) throws IOException {
        addPackages(tree);
        sharedDependencies.add(tree);
        if (connection.isIncludeSharedResources()) {
            includeSharedResources(tree);
        }

        List<DependencyTree> list = tree.getDescendants();
        for (DependencyTree child : list) {
            if (excludePackageFilter.matches(child)) {
                LOG.debug("Excluded transitive dependency: " + child);
                continue;
            } else if (excludeOptionalFilter.matches(child)) {
                LOG.debug("Excluded optional transitive dependency: " + child);
                continue;
            } else {
                sharedDependencies.add(child);
            }
        }
        addInstallDependencies(tree);
    }

    /**
     * If there are any matching shared resources using the current filters
     * then extract them from the jar and create a new jar to be added to the Bundle-ClassPath
     * containing the shared resources; which are then added to the flat class path to avoid
     * breaking the META-INF/services contracts
     */
    protected void includeSharedResources(DependencyTree tree) throws IOException {
        if (tree.isValidLibrary()) {
            File file = tree.getJarFile();
            if (file.exists()) {
                File sharedResourceFile = null;
                JarOutputStream out = null;
                JarFile jarFile = new JarFile(file);
                String[] sharedPaths = connection.getConfiguration().getSharedResourcePaths();
                String pathPrefix = tree.getGroupId() + "." + tree.getArtifactId() + "-resources";
                String path = pathPrefix + ".jar";
                if (sharedPaths != null) {
                    for (String sharedPath : sharedPaths) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry jarEntry = entries.nextElement();
                            String name = jarEntry.getName();
                            if (name.startsWith(sharedPath)) {
                                if (sharedResourceFile == null) {
                                    sharedResourceFile = File.createTempFile(pathPrefix + "-", ".jar");
                                    out = new JarOutputStream(new FileOutputStream(sharedResourceFile));
                                }
                                out.putNextEntry(new ZipEntry(jarEntry.getName()));
                                if (!jarEntry.isDirectory()) {
                                    IOHelpers.writeTo(out, jarFile.getInputStream(jarEntry), false);
                                }
                                out.closeEntry();
                            }
                        }
                    }
                }
                if (sharedResourceFile != null) {
                    out.finish();
                    out.close();
                    if (!bundleClassPath.contains(path)) {
                        embeddedResources.put(path, sharedResourceFile);
                        addBundleClassPath(path);
                        LOG.info("Adding shared resources jar: " + path);

                        // lets add the imports from this bundle's exports...
                        // as we are probably using META-INF/services type stuff
                        importAllExportedPackages(tree);
                    }
                }
            }
        }
    }

    protected void addBundleClassPath(String path) {
        if (bundleClassPath.isEmpty()) {
            bundleClassPath.add(".");
        }
        bundleClassPath.add(path);
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
                } else if (excludeOptionalFilter.matches(child)) {
                    continue;
                } else {
                    addInstallDependencies(child);
                }
            }
        }
        // lets add the child dependencies first so we add things in the right order
        installDependencies.add(node);
    }

    public String getExtraImportPackages() {
        return Strings.join(importPackages, ",");
    }

    public Map<String, DependencyTree> getDependenciesByPackage() {
        return dependenciesByPackage;
    }

    public List<DependencyTree> getOptionalDependencies() {
        return optionalDependencies;
    }
}
