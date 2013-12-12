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

package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.ModuleDescriptor;
import io.fabric8.fab.VersionedDependencyId;
import io.fabric8.fab.osgi.ServiceConstants;
import org.fusesource.common.util.Filter;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;

import static org.fusesource.common.util.Strings.notEmpty;

/**
 */
public class BundleFabFacade extends FabFacadeSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(BundleFabFacade.class);

    private final Bundle bundle;
    private Configuration configuration;
    private VersionedDependencyId dependencyId;

    public BundleFabFacade(Bundle bundle) {
        this.bundle = bundle;
        this.configuration = ConfigurationImpl.newInstance();
        setResolver(configuration.getResolver());
        Dictionary headers = bundle.getHeaders();
        Object fabId = headers.get(ServiceConstants.INSTR_FAB_MODULE_ID);
        String fabIdString = null;
        if (fabId instanceof String) {
            fabIdString = (String) fabId;
        } else {
            // lets try find the pom.properties file inside the bundle as its more reliable than bundle symbolic name
            Enumeration iter = bundle.findEntries("META-INF", "pom.properties", true);
            while (iter.hasMoreElements()) {
                Object value = iter.nextElement();
                if (value instanceof URL) {
                    URL url = (URL) value;
                    Properties properties = new Properties();
                    try {
                        properties.load(url.openStream());
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Failed to load Properties for " + url + ". " + e.getMessage(), e);
                    }
                    String groupId = properties.getProperty("groupId");
                    String artifactId = properties.getProperty("artifactId");
                    String version = properties.getProperty("version");

                    if (notEmpty(groupId) && notEmpty(artifactId) && notEmpty(version)) {
                        fabIdString = groupId + ":" + artifactId + ":" + version + ":jar";
                    }
                }
            }

            if (fabIdString == null) {
                // lets try make one using the bundle name and implementation version
                Object bundleName = headers.get(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME);
                Object versionValue = headers.get(ServiceConstants.INSTR_IMPLEMENTATION_VERSION);
                if (bundleName instanceof String && versionValue instanceof String) {
                    String name = bundleName.toString();
                    String version = versionValue.toString();
                    int idx = name.lastIndexOf('.');
                    if (idx > 0) {
                        fabIdString = name.substring(0, idx) + ":" + name.substring(idx + 1, name.length()) + ":" + version + ":jar";
                    }
                }
            }
        }
        if (fabIdString != null) {
            dependencyId = VersionedDependencyId.fromString(fabIdString);
        }
        if (dependencyId == null) {
            throw new IllegalArgumentException("Bundle is not a FAB as there is no manifest header: " + ServiceConstants.INSTR_FAB_MODULE_ID);
        }
    }

    public DependencyTree collectDependencyTree(boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
        return getResolver().collectDependencies(dependencyId, offline, excludeDependencyFilter).getTree();
    }

    @Override
    public File getJarFile() throws IOException {
        String location = bundle.getLocation();
        return new File(location);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public String getProjectDescription() {
        Object value = bundle.getHeaders().get(ModuleDescriptor.FAB_MODULE_DESCRIPTION);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    @Override
    public VersionedDependencyId getVersionedDependencyId() throws IOException {
        return dependencyId;
    }

    @Override
    public String toVersionRange(String version) {
        int digits = ServiceConstants.DEFAULT_VERSION_DIGITS;
        Object o = bundle.getHeaders().get(ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS);
        if (o instanceof String) {
            String value = (String) o;
            if (notEmpty(value)) {
                try {
                    digits = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    LOG.warn("Failed to parse manifest header " + ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS + " as a number. Got: '" + value + "' so ignoring it");
                }
                if (digits < 0 || digits > 4) {
                    LOG.warn("Invalid value of manifest header " + ServiceConstants.INSTR_FAB_VERSION_RANGE_DIGITS + " as value " + digits + " is out of range so ignoring it");
                    digits = ServiceConstants.DEFAULT_VERSION_DIGITS;
                }
            }
        }
        return Versions.toVersionRange(version, digits);

    }

    @Override
    public boolean isInstalled(DependencyTree tree) {
        return isInstalled(bundle.getBundleContext(), tree);
    }
}
