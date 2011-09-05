/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.ModuleDescriptor;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.fusesource.fabric.fab.util.Filter;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import static org.fusesource.fabric.fab.util.Strings.notEmpty;

/**
 */
public class BundleFabFacade extends FabFacadeSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(BundleFabFacade.class);

    private final Bundle bundle;
    private Configuration configuration;
    private VersionedDependencyId dependencyId;

    public BundleFabFacade(Bundle bundle) {
        this.bundle = bundle;
        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(System.getProperties());
        this.configuration = new Configuration(propertyResolver);
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

    public DependencyTree collectDependencyTree(boolean offline, Filter<DependencyTree> excludeDependencyFilter) throws RepositoryException, IOException, XmlPullParserException {
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
    public VersionedDependencyId getVersionedDependencyId() throws IOException, XmlPullParserException {
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
}
