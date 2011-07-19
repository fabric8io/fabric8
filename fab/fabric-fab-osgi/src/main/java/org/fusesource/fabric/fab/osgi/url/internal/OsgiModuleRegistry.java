/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.fusesource.fabric.fab.ModuleDescriptor;
import org.fusesource.fabric.fab.ModuleRegistry;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.url.internal.Activator;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import static org.fusesource.fabric.fab.util.Strings.*;
/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class OsgiModuleRegistry extends ModuleRegistry {

    File directory;
    ConfigurationAdmin configurationAdmin;
    String pid;

    public OsgiModuleRegistry() {
        Activator.registry = this;
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    /**
     * Store extension configuration in the config admin.
     *
     * @param id
     * @return
     */
    @Override
    protected List<String> getEnabledExtensions(VersionedDependencyId id) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(pid);
            Dictionary props = configuration.getProperties();
            if( props == null ) {
                return null;
            }
            String value = (String) props.get(id.toString());
            if( value==null ) {
                return null;
            }
            return splitAndTrimAsList(value, " ");
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Store extension configuration in the config admin.
     *
     * @param id
     * @return
     */
    @Override
    protected void setEnabledExtensions(VersionedDependencyId id, List<String> values) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(pid);
            Dictionary props = configuration.getProperties();
            if( props==null ) {
                props = new Hashtable();
            }
            props.put(id.toString(), join(values, " "));
            configuration.update(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        directory.mkdirs();
        load(directory);
    }

    private void load(File directory) {
        for(File f : directory.listFiles() ) {
            if( f.isDirectory() ) {
                load(f);
            } else {
                // load the fab module descriptors
                if( f.getName().endsWith(".fmd") ) {
                    try {
                        FileInputStream is = new FileInputStream(f);
                        try {
                            Properties properties = new Properties();
                            properties.load(is);
                            add(ModuleDescriptor.fromProperties(properties), f);

                        } finally {
                            is.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Error loading fab module descriptor '"+f+"': "+e);
                    }
                }
            }
        }
    }

    @Override
    public VersionedModule add(ModuleDescriptor descriptor) {
        try {
            // Store the module descriptor in a file..
            String path = descriptor.getId().getRepositoryPath()+".fmd";
            File file = new File(directory, path);
            file.getParentFile().mkdirs();
            Properties props = descriptor.toProperties();
            FileOutputStream os = new FileOutputStream(file);
            try {
                props.store(os, null);
            } finally {
                os.close();
            }

            return super.add(descriptor, file);
        } catch (IOException e) {
            e.printStackTrace();;
            return null;
        }
    }

    @Override
    public VersionedModule remove(VersionedDependencyId id) {
        VersionedModule rc = super.remove(id);
        if( rc!=null ) {
            rc.getFile().delete();
        }
        return rc;
    }
}
