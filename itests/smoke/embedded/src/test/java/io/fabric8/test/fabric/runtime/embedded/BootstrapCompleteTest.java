/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.test.fabric.runtime.embedded;

import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.git.GitService;
import io.fabric8.test.fabric.runtime.embedded.support.AbstractEmbeddedTest;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test fabric-core servies
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
