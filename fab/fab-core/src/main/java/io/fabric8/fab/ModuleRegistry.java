/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.fab;

import java.io.*;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import static java.util.Collections.sort;

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
        public void setEnabledExtensions(List<String> extensions) {
            ModuleRegistry.this.setEnabledExtensions(descriptor.getId(), extensions);
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

        public Map<String, VersionedModule> getVersions() {
            return Collections.unmodifiableMap(versions);
        }

        public List<VersionedDependencyId> getVersionIds() {
            ArrayList<VersionedDependencyId> rc = new ArrayList<VersionedDependencyId>(versions.size());
            for (VersionedModule module : versions.values()) {
                rc.add(module.getId());
            }
            return rc;
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

    public void loadDirectory(File directory, PrintStream err) {
        loadDirectory(directory, err, false);
    }

    /**
     * recursively scan a directory of ".fmd" files to add
     * a fabric module descriptors to the repository.
     *
     * @param directory
     */
    public void loadDirectory(File directory, PrintStream err, boolean local) {
        for(File f : directory.listFiles() ) {
            if( f.isDirectory() ) {
                loadDirectory(f, err, local);
            } else {
                // load the fab module descriptors
                if( f.getName().endsWith(".fmd") ) {
                    try {
                        FileInputStream is = new FileInputStream(f);
                        try {
                            load(local ? f : null, is);
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
