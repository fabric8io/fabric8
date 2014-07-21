/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.testkit;

import io.fabric8.api.FabricRequirements;
import io.fabric8.testkit.support.CommandLineFabricController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 */
public class FabricControllerTest {

    private FabricController factory;

    @Before
    public void init() throws Exception {
        factory = createFabricFactory();

        File baseDir = getBaseDir();
        String canonicalName = getClass().getCanonicalName();
        File workDir = new File(baseDir, "target/fabricInstall/" + canonicalName);
        factory.setWorkDirectory(workDir);
    }

    @After
    public void destroy() throws Exception {
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    public void createProvisionedFabric() throws Exception {
        FabricRequirements requirements = new FabricRequirements();
        requirements.profile("mq-default").minimumInstances(1);

        FabricAssertions.assertFabricCreate(factory, requirements);

    }

    public static File getBaseDir() {
        return new File(System.getProperty("basedir", "."));
    }


    protected FabricController createFabricFactory() {
        return new CommandLineFabricController();
    }

}
