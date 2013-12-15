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
package io.fabric8.cxf;

import org.junit.Assert;
import org.junit.Test;

public class ServerAddressResolverTest extends Assert {

    @Test
    public void testGetFullAddress() throws Exception {
        // Setup FabricServerListener
        PrefixAddressResolver resolver = new PrefixAddressResolver();
        resolver.setPrefixAddress("http://localhost:8081/cxf/service");
        FabricServerListener listener = new FabricServerListener(null, resolver);
        String address = listener.getFullAddress("/test1");
        assertEquals("Get a wrong address.", "http://localhost:8081/cxf/service/test1", address);

        address = listener.getFullAddress("http://test1");
        assertEquals("Get a wrong address.", "http://test1", address);

    }

}
