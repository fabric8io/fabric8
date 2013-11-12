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

import org.fusesource.test.fabric.runtime.ModuleLifecycleTest;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * @see ModuleLifecycleTest
 *
 * @author thomas.diesler@jbos.com
 * @since 27-Jan-2012
 */
@RunWith(Arquillian.class)
public class EmbeddedModuleLifecycleTestCase extends ModuleLifecycleTest {

    Module module;

    @Before
    public void setUp() throws Exception {
        module = EmbeddedUtils.installAndStartModule(getClass().getClassLoader(), identity);
    }

    @After
    public void tearDown() throws Exception {
        module.uninstall();
        RuntimeLocator.releaseRuntime();
    }
}
