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
package io.fabric8.forge.camel;

import io.fabric8.forge.camel.commands.project.helper.CamelCatalogService;
import org.apache.camel.catalog.CamelCatalog;
import org.junit.Assert;
import org.junit.Test;

public class CachedCamelCatalogTest {

    @Test
    public void testCachedCamelCatalog() {
        CamelCatalog camelCatalog = new CamelCatalogService().createCamelCatalog();
        String json = camelCatalog.componentJSonSchema("timer");
        String json2 = camelCatalog.componentJSonSchema("timer");

        Assert.assertSame(json, json2);
    }
}
