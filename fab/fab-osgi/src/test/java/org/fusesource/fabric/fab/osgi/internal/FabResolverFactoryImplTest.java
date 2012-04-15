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
package org.fusesource.fabric.fab.osgi.internal;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 * Test cases for {@link FabResolverFactoryImpl}
 */
public class FabResolverFactoryImplTest {

    @Test
    public void testNoFeaturesServiceAvailable() throws MalformedURLException {
        FabResolverFactoryImpl factory = new FabResolverFactoryImpl();

        FabResolverFactoryImpl.FabResolverImpl fabResolver =
                (FabResolverFactoryImpl.FabResolverImpl) factory.getResolver(new URL("http://dummy/location"));

        FabClassPathResolver classPathResolver = fabResolver.getClasspathResolver(null ,null);
        assertNotContainsInstanceOf(classPathResolver.pruningFilters, FabResolverFactoryImpl.CamelFeaturesFilter.class);

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
