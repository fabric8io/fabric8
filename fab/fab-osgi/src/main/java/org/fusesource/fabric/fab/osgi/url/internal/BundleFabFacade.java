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
import org.fusesource.fabric.fab.DependencyTreeResult;
import org.fusesource.fabric.fab.ModuleDescriptor;
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.osgi.framework.Bundle;
import org.sonatype.aether.RepositoryException;

import java.io.File;
import java.io.IOException;

/**
 */
public class BundleFabFacade extends FabFacadeSupport {
    private final Bundle bundle;
    private Configuration configuration;
    private VersionedDependencyId dependencyId;

    public BundleFabFacade(Bundle bundle) {
        this.bundle = bundle;
        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(System.getProperties());
        this.configuration = new Configuration(propertyResolver);
        Object fabId = bundle.getHeaders().get(ServiceConstants.INSTR_FAB_MODULE_ID);
        if (fabId instanceof String) {
            String fabIdString = (String) fabId;
            dependencyId = VersionedDependencyId.fromString(fabIdString);
        }
        if (dependencyId == null) {
            throw new IllegalArgumentException("Bundle is not a FAB as there is no manifest header: " + ServiceConstants.INSTR_FAB_MODULE_ID);
        }
    }

    @Override
    public DependencyTreeResult collectDependencies(boolean offline) throws RepositoryException, IOException, XmlPullParserException {
        return getResolver().collectDependencies(dependencyId.getGroupId(), dependencyId.getArtifactId(), dependencyId.getVersion(), dependencyId.getExtension(), dependencyId.getClassifier());
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
}
