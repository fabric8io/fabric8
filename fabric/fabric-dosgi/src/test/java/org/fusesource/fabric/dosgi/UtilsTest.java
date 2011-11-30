/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.fabric.dosgi.impl.EndpointDescription;
import org.fusesource.fabric.dosgi.impl.Manager;
import org.fusesource.fabric.dosgi.util.Utils;
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
