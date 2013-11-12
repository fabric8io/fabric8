/*
 * #%L
 * Gravia :: Integration Tests :: OSGi
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
package org.fusesource.test.fabric.runtime.karaf;

import java.io.InputStream;

import org.fusesource.test.fabric.runtime.ModuleLifecycleTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.resource.Constants;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

/**
 * @see ModuleLifecycleTest
 *
 * @author thomas.diesler@jboss.com
 * @since 01-Oct-2013
 */
@RunWith(Arquillian.class)
public class KarafModuleLifecycleTestCase extends ModuleLifecycleTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, identity.getSymbolicName() + ".jar");
        archive.addClasses(ModuleLifecycleTest.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(identity.getSymbolicName());
                builder.addBundleVersion(identity.getVersion().toString());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(RuntimeLocator.class, Resource.class);
                builder.addManifestHeader(Constants.GRAVIA_ENABLED, "true");
                return builder.openStream();
            }
        });
        return archive;
    }
}
