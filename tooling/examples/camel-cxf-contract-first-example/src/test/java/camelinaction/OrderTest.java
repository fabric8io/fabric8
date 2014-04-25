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
package camelinaction;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

public class OrderTest extends CamelBlueprintTestSupport {

    private boolean canTest = true;

    @Override
    public void setUp() throws Exception {
        try {
            super.setUp();
        } catch (Exception e) {
            // ignore if we fail during setup due OSGi issue
            canTest = false;
        }
    }

    @BeforeClass
    public static void setupPort() {
        int port = AvailablePortFinder.getNextAvailable(10000);
        System.setProperty("port", "" + port);
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "camel-route-test.xml";
    }

    @Test
    public void testOrderOk() throws Exception {
        if (!canTest) {
            return;
        }

        List<Object> params = new ArrayList<Object>();
        params.add("motor");
        params.add(1);
        params.add("honda");
        
        String reply = template.requestBody("cxf:bean:orderEndpoint", params, String.class);
        assertEquals("OK", reply);
    }
}
