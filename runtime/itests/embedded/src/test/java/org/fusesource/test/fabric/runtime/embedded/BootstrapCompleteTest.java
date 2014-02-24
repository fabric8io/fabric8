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
package org.fusesource.test.fabric.runtime.embedded;

import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.git.GitService;
import io.fabric8.runtime.itests.support.ServiceLocator;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;

import org.fusesource.test.fabric.runtime.embedded.support.AbstractEmbeddedTest;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test fabric-core servies
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Oct-2013
 */
@RunWith(Arquillian.class)
public class BootstrapCompleteTest extends AbstractEmbeddedTest {

    @Test
    public void testBootstrapConfiguration() {
        BootstrapConfiguration service = ServiceLocator.getRequiredService(BootstrapConfiguration.class);
        CreateEnsembleOptions options = service.getBootstrapOptions();
        Assert.assertFalse("Ensemble start", options.isEnsembleStart());
    }

    @Test
    public void testGitService() {
        ServiceLocator.getRequiredService(GitService.class);
    }
}
