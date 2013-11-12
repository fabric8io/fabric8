/*
 * #%L
 * Gravia :: Integration Tests :: Tomcat
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
package org.fusesource.test.fabric.runtime.tomcat;

import java.io.InputStream;

import org.fusesource.test.fabric.runtime.ConfigurationAdminTest;
import org.fusesource.test.fabric.runtime.sub.d.ServiceD;
import org.fusesource.test.fabric.runtime.sub.d1.ServiceD1;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.tomcat.ApplicationActivator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * @see ConfigurationAdminTest
 *
 * @author thomas.diesler@jboss.com
 * @since 01-Oct-2013
 */
@RunWith(Arquillian.class)
public class TomcatConfigurationAdminTestCase extends ConfigurationAdminTest {

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, identity.getSymbolicName() + ".war");
        archive.addClasses(ApplicationActivator.class, ConfigurationAdminTest.class);
        archive.addClasses(ServiceD.class, ServiceD1.class);
        archive.addAsResource("OSGI-INF/org.fusesource.test.fabric.runtime.sub.d.ServiceD.xml");
        archive.addAsResource("OSGI-INF/org.fusesource.test.fabric.runtime.sub.d1.ServiceD1.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability(identity);
                builder.addManifestHeader("Service-Component", "OSGI-INF/org.fusesource.test.fabric.runtime.sub.d.ServiceD.xml,OSGI-INF/org.fusesource.test.fabric.runtime.sub.d1.ServiceD1.xml");
                return builder.openStream();
            }
        });
        return archive;
    }
}
