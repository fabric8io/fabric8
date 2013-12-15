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
package io.fabric8.dosgi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.dosgi.impl.EndpointDescription;
import io.fabric8.dosgi.impl.Manager;
import io.fabric8.dosgi.util.Utils;
import org.junit.Test;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UtilsTest {

    @Test
    public void testXml() throws Exception {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.OBJECTCLASS, new String[] { BundleActivator.class.getName() });
        props.put("protocols", new String[]{"foo", "bar"});
        props.put("ints", new int[]{1, 2, 3});
        props.put("endpoint.id", "identifier");
        props.put("service.imported.configs", Collections.<Object>singletonList(Manager.CONFIG));;
        EndpointDescription endpoint1 = new EndpointDescription(props);

        String xml = Utils.getEndpointDescriptionXML(endpoint1);

        EndpointDescription endpoint2 = Utils.getEndpointDescription(xml);

        assertNotNull(endpoint2);
        assertNotNull(endpoint2.getInterfaces());
        assertEquals(1, endpoint2.getInterfaces().size());
        assertEquals(BundleActivator.class.getName(), endpoint2.getInterfaces().get(0));
        assertEquals("identifier", endpoint2.getId());
        assertNotNull(endpoint2.getProperties().get("protocols"));
        assertEquals(2, ((String[]) endpoint2.getProperties().get("protocols")).length);
        assertEquals("foo", ((String[]) endpoint2.getProperties().get("protocols"))[0]);
        assertEquals("bar", ((String[]) endpoint2.getProperties().get("protocols"))[1]);
        assertNotNull(endpoint2.getProperties().get("ints"));
        assertEquals(3, ((int[]) endpoint2.getProperties().get("ints")).length);
        assertEquals(1, ((int[]) endpoint2.getProperties().get("ints"))[0]);
        assertEquals(2, ((int[]) endpoint2.getProperties().get("ints"))[1]);
        assertEquals(3, ((int[]) endpoint2.getProperties().get("ints"))[2]);
    }

}
