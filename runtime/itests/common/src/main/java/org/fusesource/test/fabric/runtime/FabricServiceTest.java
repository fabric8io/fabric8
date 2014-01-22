/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
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
package org.fusesource.test.fabric.runtime;

import io.fabric8.api.FabricService;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test basic {@link FabricService} functionality
 *
 * @author thomas.diesler@jbos.com
 * @since 04-Oct-2013
 */
@RunWith(Arquillian.class)
public class FabricServiceTest  {

    private Runtime runtime;
    private ModuleContext syscontext;
    private FabricService fabricService;

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("fabric-service");
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(archive.getName());
                    builder.addBundleVersion("1.0.0");
                    builder.addManifestHeader(Constants.GRAVIA_ENABLED, Boolean.TRUE.toString());
                    builder.addImportPackages(RuntimeLocator.class, FabricService.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(archive.getName(), "1.0.0");
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia,io.fabric8.api");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Before
    public void before() throws Exception {
        runtime = RuntimeLocator.getRuntime();
        syscontext = runtime.getModule(0).getModuleContext();
        fabricService = syscontext.getService(syscontext.getServiceReference(FabricService.class));
    }

    @Test
    public void testFabricServiceAvailable() throws Exception {
        Assert.assertNotNull("FabricService not null", fabricService);
    }

}
