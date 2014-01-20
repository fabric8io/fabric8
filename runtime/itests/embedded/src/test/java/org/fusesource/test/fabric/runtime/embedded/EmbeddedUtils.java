/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.test.fabric.runtime.embedded;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.embedded.internal.EmbeddedRuntime;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.spi.RuntimeFactory;
import org.jboss.gravia.runtime.util.ClassLoaderEntriesProvider;
import org.jboss.gravia.runtime.util.DefaultPropertiesProvider;
import org.jboss.gravia.runtime.util.ManifestHeadersProvider;

/**
 * Utility for embedded runtime tests
 *
 * @author thomas.diesler@jbos.com
 * @since 18-Oct-2013
 */
public class EmbeddedUtils {

    static Runtime getEmbeddedRuntime() {
        Runtime runtime = RuntimeLocator.getRuntime();
        if (runtime == null) {
            RuntimeFactory factory = new RuntimeFactory() {
                @Override
                public Runtime createRuntime(PropertiesProvider propertiesProvider) {
                    return new EmbeddedRuntime(propertiesProvider, null) {
                        @Override
                        protected ModuleEntriesProvider getDefaultEntriesProvider(Module module, Attachable context) {
                            return new ClassLoaderEntriesProvider(module);
                        }
                    };
                }
            };
            runtime = RuntimeLocator.createRuntime(factory, new DefaultPropertiesProvider());
            runtime.init();
        }
        return runtime;
    }

    static ModuleContext getSystemContext() {
        Module sysmodule = getEmbeddedRuntime().getModule(0);
        return sysmodule.getModuleContext();
    }

    static <T> T getSystemService(Class<T> type) {
        ModuleContext context = getSystemContext();
        return context.getService(context.getServiceReference(type));
    }

    static Module installAndStartModule(ClassLoader classLoader, String symbolicName) throws ModuleException, IOException {
        File modfile = getModuleFile(symbolicName);
        if (modfile.isFile()) {
            return installAndStartModule(classLoader, modfile);
        } else {
            return installAndStartModule(classLoader, symbolicName, null);
        }
    }

    static Module installAndStartModule(ClassLoader classLoader, File location) throws ModuleException, IOException {
        return installAndStartModule(classLoader, location.toURI().toURL());
    }

    static Module installAndStartModule(ClassLoader classLoader, URL location) throws ModuleException, IOException {
        JarInputStream input = new JarInputStream(location.openStream());
        try {
            Manifest manifest = input.getManifest();
            Dictionary<String, String> headers = new ManifestHeadersProvider(manifest).getHeaders();
            return installAndStartModule(classLoader, null, headers);
        } finally {
            input.close();
        }
    }

    static Module installAndStartModule(ClassLoader classLoader, String symbolicName, String version) throws ModuleException {
        ResourceIdentity.create(symbolicName, version);
        Resource resource = new DefaultResourceBuilder().addIdentityCapability(symbolicName, version).getResource();
        return installAndStartModule(classLoader, resource);
    }

    static Module installAndStartModule(ClassLoader classLoader, ResourceIdentity identity) throws ModuleException {
        Resource resource = new DefaultResourceBuilder().addIdentityCapability(identity).getResource();
        return installAndStartModule(classLoader, resource);
    }

    static Module installAndStartModule(ClassLoader classLoader, Resource resource) throws ModuleException {
        return installAndStartModule(classLoader, resource, null);
    }

    static Module installAndStartModule(ClassLoader classLoader, Resource resource, Dictionary<String, String> headers) throws ModuleException {
        Module module = getEmbeddedRuntime().installModule(classLoader, resource, headers);
        module.start();
        return module;
    }

    static File getModuleFile(String modname) {
        return new File("system/modules/" + modname + ".jar").getAbsoluteFile();
    }
}
