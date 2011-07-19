/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package org.fusesource.fabric.fab.osgi;

import org.fusesource.fabric.fab.ModuleDescriptor;
import org.fusesource.fabric.fab.ModuleRegistry;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

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
            String value = (String) props.get(id.toString());
            if( value==null ) {
                return null;
            }
            return ModuleDescriptor.split(value, " ");
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
            props.put(id.toString(), ModuleDescriptor.mkString(values, " "));
            configuration.update(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() throws IOException {
        directory.mkdirs();

        // TODO: store in a maven repo style layout.

        // Load all previously stored module descriptors.
        for(File f : directory.listFiles() ) {
            if( f.getName().startsWith("module-") ) {
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
                    System.err.println("Invalid module file: "+f+", error loading: "+e);
                }
            }
        }
    }

    @Override
    public VersionedModule add(ModuleDescriptor descriptor) {
        try {

            // Store the module info in a file..
            File file = File.createTempFile("module-", "", directory);
            Properties props = descriptor.toProperties();
            FileOutputStream os = new FileOutputStream(file);
            try {
                props.store(os, "");
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
