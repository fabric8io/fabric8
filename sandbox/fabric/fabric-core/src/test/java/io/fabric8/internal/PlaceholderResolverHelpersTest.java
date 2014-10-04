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
package io.fabric8.internal;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Set;

public class PlaceholderResolverHelpersTest {

    @Test
    public void testExtractSchemes() {
        //Simple key.
        String key = "${zk:container/ip}";
        Set<String> schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 1);
        Assert.assertTrue(schemes.contains("zk"));

        //Nested key
        key = "${zk:container/${zk:container/resolver}}";
        schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 1);
        Assert.assertTrue(schemes.contains("zk"));

        //Nested key with multiple schemes
        key = "${profile:${zk:container/foo}";
        schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 2);
        Assert.assertTrue(schemes.contains("zk"));
        Assert.assertTrue(schemes.contains("profile"));

        key = "file:${runtime.home}/${karaf.default.repository}@snapshots@id=karaf-default,file:${runtime.home}/local-repo@snapshots@id=karaf-local";
        schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 0);
    }
}
