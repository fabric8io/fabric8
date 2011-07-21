/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import java.io.*;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.*;

/**
 * <p>The ModuleRegistry keeps track of all Fab modules that
 * are installed or available to be installed, their descriptions
 * and extension relationships.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ModuleRegistry {

    private  HashMap<DependencyId, Module> modules = new HashMap<DependencyId, Module>();
    private  HashMap<VersionedDependencyId, VersionedModule> moduleVersions = new HashMap<VersionedDependencyId, VersionedModule>();
    private  HashMap<VersionedDependencyId, TreeMap<String, VersionedModule>> extensions = new HashMap<VersionedDependencyId, TreeMap<String, VersionedModule>>();

    public VersionedModule add(ModuleDescriptor descriptor) {
        return add(descriptor, null);
    }

    public void clear() {
        modules.clear();
        moduleVersions.clear();
    }

    protected VersionedModule add(ModuleDescriptor descriptor, File file) {
        VersionedDependencyId id = descriptor.getId();
        remove(id);
        VersionedModule rc = new VersionedModule(descriptor, file);
        moduleVersions.put(id, rc);

        DependencyId dependencyId = id.toDependencyId();
        Module module = modules.get(dependencyId);
        if( module==null ) {
            module = new Module(dependencyId);
            modules.put(dependencyId, module);
        }
        module.versions.put(id.getVersion(), rc);

        for (VersionedDependencyId key: descriptor.getExtendsModules()){
            TreeMap<String, VersionedModule> map = extensions.get(key);
            if( map == null ) {
                map = new TreeMap<String, VersionedModule>();
                extensions.put(key, map);
            }
            map.put(rc.getName(), rc);
        }
        return rc;
    }

    public VersionedModule remove(VersionedDependencyId id) {
        VersionedModule versionedModule = moduleVersions.remove(id);
        if( versionedModule!=null ) {
            ModuleDescriptor descriptor = versionedModule.descriptor;

            for (VersionedDependencyId key: descriptor.getExtendsModules()){
                TreeMap<String, VersionedModule> map = extensions.get(key);
                if( map != null ) {
                    map.remove(versionedModule.getName());
                    if( map.isEmpty() ) {
                        extensions.remove(key);
                    }
                }
            }

            DependencyId dependencyId = id.toDependencyId();
            Module module = modules.get(dependencyId);
            if( module!=null ) {
                module.versions.remove(id.getVersion());
                if( module.versions.isEmpty() ) {
                    modules.remove(dependencyId);
                }
            }
        }
        return versionedModule;
    }

    private  HashMap<VersionedDependencyId, List<String>> enabledExtensions = new HashMap<VersionedDependencyId, List<String>>();
    protected List<String> getEnabledExtensions(VersionedDependencyId id) {
        return enabledExtensions.get(id);
    }
    protected void setEnabledExtensions(VersionedDependencyId id, List<String> values) {
        if( values==null ) {
            enabledExtensions.remove(id);
        } else {
            enabledExtensions.put(id, values);
        }
    }

    public class VersionedModule {

        private final ModuleDescriptor descriptor;
        private final File file;

        VersionedModule(ModuleDescriptor descriptor, File file) {
            this.descriptor = descriptor;
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public Map<String, VersionedModule> getAvailableExtensions() {
            TreeMap<String, VersionedModule> map = extensions.get(descriptor.getId());
            if( map == null ) {
                return Collections.emptyMap();
            } else {
                return Collections.unmodifiableMap(map);
            }
        }

        public List<String> getEnabledExtensions() {
            List<String> rc = ModuleRegistry.this.getEnabledExtensions(descriptor.getId());
            if( rc == null ) {
                return Collections.unmodifiableList(getDefaultExtensions());
            } else {
                return Collections.unmodifiableList(rc);
            }
        }

        public List<String> getDefaultExtensions() {
            return Collections.unmodifiableList(descriptor.getDefaultExtensions());
        }

        public VersionedDependencyId getId() {
            return descriptor.getId();
        }

        public String getLongDescription() {
            return descriptor.getLongDescription();
        }

        public String getName() {
            return descriptor.getName();
        }

        public String getDescription() {
            return descriptor.getDescription();
        }

        public boolean isExtensionModule() {
            return descriptor.isExtensionModule();
        }

    }

    public class Module {
        final DependencyId id;
        final TreeMap<String, VersionedModule> versions;

        Module(DependencyId id) {
            this.id = id;
            versions = new TreeMap<String, VersionedModule>(new Comparator<String>() {
                @Override
                public int compare(String d1, String d2) {
                    return d1.compareTo( d2);
                }
            });
        }

        public VersionedModule latest() {
            return versions.lastEntry().getValue();
        }

        public String getName() {
            if( latest().getName()!=null ) {
                return latest().getName();
            } else {
                return latest().getId().getArtifactId();
            }
        }

        public List<VersionedModule> getVersions() {
            return new ArrayList<VersionedModule>(versions.values());
        }

        public boolean isInstalled() {
            // TODO: inspect the installed OSGi bundles
            // to see if this is true.
            return true;
        }
    }

    public VersionedModule getVersionedModule(VersionedDependencyId id) {
        return moduleVersions.get(id);
    }

    public List<Module> getModules() {
        ArrayList<Module> rc = new ArrayList<Module>(modules.values());
        sort(rc, new Comparator<Module>() {
            @Override
            public int compare(Module d1, Module d2) {
                return d1.getName().compareTo(d2.getName());
            }
        });
        return rc;
    }

    public List<Module> getApplicationModules() {
        List<Module> rc = getModules();
        // Remove all purely extension modules.
        Iterator<Module> iterator = rc.iterator();
        while (iterator.hasNext()) {
            Module next =  iterator.next();
            if(next.latest().isExtensionModule()) {
                iterator.remove();
            }
        }
        return rc;
    }

    /**
     * Scan a jar file for ".fmd" files to add as
     * fabric module descriptors to the repository.
     *
     * @param file
     * @throws IOException
     */
    public void loadJar(File file) throws IOException {
        JarInputStream jar = new JarInputStream(new FileInputStream(file));
        try {
            ZipEntry zipEntry;
            while( (zipEntry = jar.getNextEntry()) != null ) {
                if( zipEntry.getName().endsWith(".fmd") && !zipEntry.isDirectory()) {
                    load(null, jar);
                }
                jar.closeEntry();
            }
        } finally {
            jar.close();
        }
    }

    /**
     * recursively scan a directory of ".fmd" files to add
     * a fabric module descriptors to the repository.
     *
     * @param directory
     */
    public void loadDirectory(File directory, PrintStream err) {
        for(File f : directory.listFiles() ) {
            if( f.isDirectory() ) {
                loadDirectory(f, err);
            } else {
                // load the fab module descriptors
                if( f.getName().endsWith(".fmd") ) {
                    try {
                        FileInputStream is = new FileInputStream(f);
                        try {
                            load(f, is);
                        } finally {
                            is.close();
                        }
                    } catch (IOException e) {
                        err.println("Error loading fab module descriptor '"+f+"': "+e);
                    }
                }
            }
        }
    }

    private void load(File f, InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        ModuleDescriptor descriptor = ModuleDescriptor.fromProperties(properties);

        // only the first load wins..
        if( getVersionedModule(descriptor.getId()) == null ) {
            add(descriptor, f);
        }
    }

}
