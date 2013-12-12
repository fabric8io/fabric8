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
import java.util.List;

import org.apache.karaf.features.FeaturesService;
import io.fabric8.fab.DependencyTree;
import org.fusesource.common.util.Filter;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link FabResolverFactoryImpl}
 */
public class FabResolverFactoryImplTest {

    @Test
    public void testFeaturesServiceAvailable() throws MalformedURLException {
        FabResolverFactoryImpl factory = new FabResolverFactoryImpl();
        factory.setFeaturesService(createNiceMock(FeaturesService.class));
        factory.setConfiguration(ConfigurationImpl.newInstance());

        FabResolverFactoryImpl.FabResolverImpl fabResolver =
                (FabResolverFactoryImpl.FabResolverImpl) factory.getResolver(new URL("http://dummy/location"));

        FabClassPathResolver classPathResolver = fabResolver.getClasspathResolver(null ,null);
        assertContainsInstanceOf(classPathResolver.pruningFilters, FabResolverFactoryImpl.FeaturesMatchingFilter.class);

    }

    @Test
    public void testNoFeaturesServiceAvailable() throws MalformedURLException {
        FabResolverFactoryImpl factory = new FabResolverFactoryImpl();
        factory.setConfiguration(ConfigurationImpl.newInstance());

        FabResolverFactoryImpl.FabResolverImpl fabResolver =
                (FabResolverFactoryImpl.FabResolverImpl) factory.getResolver(new URL("http://dummy/location"));

        FabClassPathResolver classPathResolver = fabResolver.getClasspathResolver(null ,null);
        assertNotContainsInstanceOf(classPathResolver.pruningFilters, FabResolverFactoryImpl.FeaturesMatchingFilter.class);
    }

    private void assertContainsInstanceOf(List<Filter<DependencyTree>> items, Class type) {
        assertTrue("Should have found an instance of " + type + " in collection " + items, containsInstanceOf(items, type));
    }

    private void assertNotContainsInstanceOf(List<?> items, Class type) {
        assertFalse("Should not find an instance of " + type + " in collection " + items, containsInstanceOf(items, type));
    }

    private boolean containsInstanceOf(List<?> items, Class type) {
        for (Object item : items) {
            if (type.isAssignableFrom(item.getClass())) {
                return true;
            }
        }
        return false;
    }
}
