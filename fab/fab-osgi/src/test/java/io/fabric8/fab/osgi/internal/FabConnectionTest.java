/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.fab.osgi.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import io.fabric8.fab.osgi.ServiceConstants;
import org.junit.Test;
import org.ops4j.util.property.DictionaryPropertyResolver;

/**
 * Tests for {@link FabConnection}
 */
public class FabConnectionTest {

    @Test
    public void testResolverConfiguration() throws MalformedURLException {
        Properties properties = new Properties();
        properties.setProperty(ServiceConstants.PROPERTY_MAVEN_REPOSITORIES, "http://repo1,http://repo2");
        properties.setProperty(ServiceConstants.PROPERTY_LOCAL_MAVEN_REPOSITORY, "/home/test/.m2/repository");

        URL url = new URL("file:test");
        ConfigurationImpl configuration = new ConfigurationImpl(new DictionaryPropertyResolver(properties));

        // TODO: reimplement this test
//        FabConnection connection = new FabConnection(url, configuration, null);
//        assertArrayEquals(new String[]{"http://repo1", "http://repo2"}, connection.getResolver().getRepositories());
//        assertEquals("/home/test/.m2/repository", connection.getResolver().getLocalRepo());
                
    }
}
