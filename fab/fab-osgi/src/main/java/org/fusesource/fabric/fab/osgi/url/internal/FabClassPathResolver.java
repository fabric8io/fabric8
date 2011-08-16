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

import aQute.lib.osgi.Analyzer;
import org.apache.felix.utils.version.VersionCleaner;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.*;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Filter;
import org.fusesource.fabric.fab.util.IOHelpers;
import org.fusesource.fabric.fab.util.Manifests;
import org.fusesource.fabric.fab.util.Strings;
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
import static org.fusesource.fabric.fab.util.Strings.join;

/**
 * Resolves the classpath using the FAB resolving mechanism
 */
public class FabClassPathResolver {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabClassPathResolver.class);

    private FabFacade connection;
    private Properties instructions;
    private Map<String, Object> embeddedResources;
    private HashMap<String, DependencyTree> dependenciesByPackage = new HashMap<String, DependencyTree>();
    private ModuleRegistry moduleRegistry;
    private List<String> bundleClassPath = new ArrayList<String>();
    private List<String> requireBundles = new ArrayList<String>();
    private List<String> importPackages = new ArrayList<String>();
    private boolean offline = false;

    HashSet<String> sharedFilterPatterns = new HashSet<String>();
    HashSet<String> requireBundleFilterPatterns = new HashSet<String>();
    HashSet<String> excludeFilterPatterns = new HashSet<String>();
    HashSet<String> optionalDependencyPatterns = new HashSet<String>();

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

    private Manifest manfiest;

    public FabClassPathResolver(FabFacade connection, Properties instructions, Map<String, Object> embeddedResources) {
        this.connection = connection;
        this.instructions = instructions;
        this.embeddedResources = embeddedResources;
        this.moduleRegistry = Activator.registry;
        this.resolver = connection.getResolver();
    }


    public void resolve() throws RepositoryException, IOException, XmlPullParserException, BundleException {
        moduleId = connection.getVersionedDependencyId();
        if (moduleId == null) {
            return;
        }
        DependencyTreeResult result = connection.collectDependencies(offline);
        this.rootTree = result.getTree();

        sharedFilterPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(getManifestProperty(ServiceConstants.INSTR_FAB_PROVIDED_DEPENDENCY)), "\\s+"));
        requireBundleFilterPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(getManifestProperty(ServiceConstants.INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE)), "\\s+"));
        excludeFilterPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(getManifestProperty(ServiceConstants.INSTR_FAB_EXCLUDE_DEPENDENCY)), "\\s+"));
        optionalDependencyPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(getManifestProperty(ServiceConstants.INSTR_FAB_OPTIONAL_DEPENDENCY)), "\\s+"));

        sharedFilter = DependencyTreeFilters.parseShareFilter(join(sharedFilterPatterns, " "));
        requireBundleFilter = DependencyTreeFilters.parseRequireBundleFilter(join(requireBundleFilterPatterns, " "));
        excludePackageFilter = DependencyTreeFilters.parseExcludeFilter(join(excludeFilterPatterns, " "));
        excludeOptionalFilter = DependencyTreeFilters.parseExcludeOptionalFilter(join(optionalDependencyPatterns, " "));

        bundleClassPath.addAll(Strings.splitAsList(getManifestProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH), ","));
        requireBundles.addAll(Strings.splitAsList(getManifestProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE), ","));
        importPackages.addAll(Strings.splitAsList(getManifestProperty(ServiceConstants.INSTR_IMPORT_PACKAGE), ","));


        String name = getManifestProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME);
        if (name.length() <= 0) {
            name = rootTree.getBundleSymbolicName();
            instructions.setProperty(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME, name);
        }
        String bundleVersion = getManifestProperty(Analyzer.BUNDLE_VERSION);
        if (bundleVersion.length() <= 0) {
            bundleVersion = VersionCleaner.clean(rootTree.getVersion());
            instructions.setProperty(Analyzer.BUNDLE_VERSION, bundleVersion);
        }

        LOG.debug("Resolving Dependencies for: "+rootTree.getDependencyId());
        addDependencies(rootTree);

        // Build a ModuleDescriptor using the Jar Manifests headers..
        ModuleRegistry.VersionedModule module = moduleRegistry.getVersionedModule(moduleId);
        if (module == null || module.getFile() != null) {
            registerModule();
        }

        resolveExtensions(rootTree);

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

        // Remove dup dependencies..
        nonSharedDependencies = filterOutDuplicates(nonSharedDependencies);
        sharedDependencies = filterOutDuplicates(sharedDependencies);
        installDependencies = filterOutDuplicates(installDependencies);
        optionalDependencies = filterOutDuplicates(optionalDependencies);

        LOG.debug("nonSharedDependencies:");
        for( DependencyTree d : nonSharedDependencies) {
            LOG.debug("  "+d.getDependencyId());
        }
        LOG.debug("sharedDependencies:");
        for( DependencyTree d : sharedDependencies) {
            LOG.debug("  "+d.getDependencyId());
        }
        LOG.debug("installDependencies:");
        for( DependencyTree d : installDependencies) {
            LOG.debug("  "+d.getDependencyId());
        }

        LOG.debug("resolved: bundleClassPath: " + Strings.join(bundleClassPath, "\t\n"));
        LOG.debug("resolved: requireBundles: " + Strings.join(requireBundles, "\t\n"));
        LOG.debug("resolved: importPackages: " + Strings.join(importPackages, "\t\n"));

        instructions.setProperty(ServiceConstants.INSTR_BUNDLE_CLASSPATH, Strings.join(bundleClassPath, ","));
        instructions.setProperty(ServiceConstants.INSTR_REQUIRE_BUNDLE, Strings.join(requireBundles, ","));
        instructions.setProperty(ServiceConstants.INSTR_FAB_MODULE_ID, moduleId.toString());

        // Update the headers fab headers.. they may have been updated by the extensions.
        if( !sharedFilterPatterns.isEmpty() ) {
            instructions.setProperty(ServiceConstants.INSTR_FAB_PROVIDED_DEPENDENCY, join(sharedFilterPatterns, " "));
        }
        if( !requireBundleFilterPatterns.isEmpty() ) {
            instructions.setProperty(ServiceConstants.INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE, join(requireBundleFilterPatterns, " "));
        }
        if( !excludeFilterPatterns.isEmpty() ) {
            instructions.setProperty(ServiceConstants.INSTR_FAB_EXCLUDE_DEPENDENCY, join(excludeFilterPatterns, " "));
        }
        if( !optionalDependencyPatterns.isEmpty() ) {
            instructions.setProperty(ServiceConstants.INSTR_FAB_OPTIONAL_DEPENDENCY, join(optionalDependencyPatterns, " "));
        }

    }

    public List<DependencyTree> getInstallDependencies() {
        return installDependencies;
    }

    public List<DependencyTree> getSharedDependencies() {
        return sharedDependencies;
    }

    public List<DependencyTree> getNonSharedDependencies() {
        return nonSharedDependencies;
    }

    public boolean isOffline() {
        return offline;
    }

    public DependencyTree getRootTree() {
        return rootTree;
    }

    private List<DependencyTree> filterOutDuplicates(List<DependencyTree> list) {
        LinkedHashMap<DependencyId, DependencyTree> map = new LinkedHashMap<DependencyId, DependencyTree>();
        for (DependencyTree tree : list) {
            if( !map.containsKey(tree.getDependencyId()) ) {
                map.put(tree.getDependencyId(), tree);
            }
        }
        return new ArrayList<DependencyTree>(map.values());
    }

    private void registerModule() throws IOException, XmlPullParserException {
        try {
            Properties moduleProperties = new Properties();
            for( String key: FAB_MODULE_PROPERTIES) {
                String value = getManifestProperty("Fabric-" + key);
                if( Strings.notEmpty(value) ) {
                    moduleProperties.setProperty(key, value);
                }
            }
            // Enhance with maven pom information
            if( !moduleProperties.containsKey(FAB_MODULE_ID) ) {
                moduleProperties.setProperty(FAB_MODULE_ID, moduleId.toString());
            }
            if( !moduleProperties.containsKey(FAB_MODULE_NAME) ) {
                moduleProperties.setProperty(FAB_MODULE_NAME, moduleId.getArtifactId());
            }
            if( !moduleProperties.containsKey(FAB_MODULE_DESCRIPTION) ) {
                moduleProperties.setProperty(FAB_MODULE_DESCRIPTION, emptyIfNull(connection.getProjectDescription()));
            }

            ModuleDescriptor descriptor = ModuleDescriptor.fromProperties(moduleProperties);
            moduleRegistry.add(descriptor);

        } catch (Exception e) {
            System.err.println("Failed to register the fabric module for: "+moduleId);
            e.printStackTrace();
        }
    }

    protected void resolveExtensions(DependencyTree root) throws IOException, RepositoryException, XmlPullParserException {
        ModuleRegistry.VersionedModule module = moduleRegistry.getVersionedModule(moduleId);
        if( module!=null ) {
            Map<String, ModuleRegistry.VersionedModule> availableExtensions = module.getAvailableExtensions();
            String extensionsString="";
            for (String enabledExtension : module.getEnabledExtensions()) {
                ModuleRegistry.VersionedModule extensionModule = availableExtensions.get(enabledExtension);
                if( extensionModule!=null ) {
                    VersionedDependencyId id = extensionModule.getId();

                    // lets resolve the dependency
                    DependencyTreeResult result = resolver.collectDependencies(id, offline);

                    if (result != null) {
                        DependencyTree tree = result.getTree();

                        sharedFilterPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(tree.getManfiestEntry(ServiceConstants.INSTR_FAB_PROVIDED_DEPENDENCY)), "\\s+"));
                        requireBundleFilterPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(tree.getManfiestEntry(ServiceConstants.INSTR_FAB_DEPENDENCY_REQUIRE_BUNDLE)), "\\s+"));
                        excludeFilterPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(tree.getManfiestEntry(ServiceConstants.INSTR_FAB_EXCLUDE_DEPENDENCY)), "\\s+"));
                        optionalDependencyPatterns.addAll(Strings.splitAndTrimAsList(emptyIfNull(tree.getManfiestEntry(ServiceConstants.INSTR_FAB_OPTIONAL_DEPENDENCY)), "\\s+"));

                        sharedFilter = DependencyTreeFilters.parseShareFilter(join(sharedFilterPatterns, " "));
                        requireBundleFilter = DependencyTreeFilters.parseRequireBundleFilter(join(requireBundleFilterPatterns, " "));
                        excludePackageFilter = DependencyTreeFilters.parseExcludeFilter(join(excludeFilterPatterns, " "));
                        excludeOptionalFilter = DependencyTreeFilters.parseExcludeOptionalFilter(join(optionalDependencyPatterns, " "));

                        LOG.debug("Adding extension: " + tree.getDependencyId());
                        if( extensionsString.length()!=0 ) {
                            extensionsString += " ";
                        }
                        extensionsString += id;
                        addChildDependency(tree);

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

    public Manifest getManifest() {
        if( manfiest == null ) {
            try {
                File jarFile = connection.getJarFile();
                if (jarFile != null && jarFile.exists()) {
                    manfiest = Manifests.getManfiest(jarFile);
                }
            } catch (IOException e) {
                // TODO: warn
                manfiest = new Manifest();
            }
        }
        return manfiest;
    }

    public String getManifestProperty(String name) {
        String answer = null;
        Manifest manifest = getManifest();
        if (manifest != null) {
            // TODO do some caching!!!
            answer = manifest.getMainAttributes().getValue(name);
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
            addChildDependency(child);
        }
    }

    private void addChildDependency(DependencyTree child) throws IOException {
        String dependencyId = child.getDependencyId().toString();
        if (excludePackageFilter.matches(child)) {
            // ignore
            LOG.debug("Excluded dependency: " + dependencyId);
            return;
        } else if (excludeOptionalFilter.matches(child)) {
            addOptionalDependency(child);
            return;
        } else if (sharedFilter.matches(child) || requireBundleFilter.matches(child)) {
            // lets add all the transitive dependencies as shared
            addSharedDependency(child);
        } else {
            LOG.debug("Added non-shared dependency: " + dependencyId);
            nonSharedDependencies.add(child);
            // we now need to recursively flatten all transitive dependencies (whether shared or not)
            addDependencies(child);
        }
    }

    private void addOptionalDependency(DependencyTree tree) {
        LOG.debug("Added optional dependency: " + tree.getDependencyId());
        addPackages(tree);
        optionalDependencies.add(tree);
        List<DependencyTree> list = tree.getDescendants();
        for (DependencyTree child : list) {
            if (excludePackageFilter.matches(child)) {
                LOG.debug("Excluded transitive dependency: " + child.getDependencyId());
                continue;
            } else {
                addOptionalDependency(child);
            }
        }
    }

    protected void addSharedDependency(DependencyTree tree) throws IOException {
        LOG.debug("Added shared dependency: " + tree.getDependencyId());
        addPackages(tree);
        sharedDependencies.add(tree);
        if (connection.isIncludeSharedResources()) {
            includeSharedResources(tree);
        }

        List<DependencyTree> list = tree.getDescendants();
        for (DependencyTree child : list) {
            if (excludePackageFilter.matches(child)) {
                LOG.debug("Excluded transitive dependency: " + child.getDependencyId());
                continue;
            } else if (excludeOptionalFilter.matches(child)) {
                LOG.debug("Excluded optional transitive dependency: " + child.getDependencyId());
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
