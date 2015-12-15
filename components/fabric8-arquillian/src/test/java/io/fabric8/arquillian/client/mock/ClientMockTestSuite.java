/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.arquillian.client.mock;

import io.fabric8.arquillian.PodInjection;
import io.fabric8.arquillian.ResourceInjection;
import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.kubernetes.client.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;


@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                ResourceInjection.class,
                PodInjection.class
        }
)
public class ClientMockTestSuite {

    @BeforeClass
    public static void setUpClass() throws IOException {
        System.setProperty(Constants.CLIENT_CREATOR_CLASS_NAME, MockClientCreator.class.getName());
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "arquillian");
        System.setProperty(Constants.NAMESPACE_TO_USE, "arquillian");
        System.setProperty(Constants.NAMESPACE_LAZY_CREATE_ENABLED, "arquillian");
        System.setProperty(Constants.ENVIRONMENT_CONFIG_URL, ClientMockTestSuite.class.getResource("/test-kubernetes-1.json").toString());
    }

    @AfterClass
    public static void tearDownClass() throws IOException {

    }
}
