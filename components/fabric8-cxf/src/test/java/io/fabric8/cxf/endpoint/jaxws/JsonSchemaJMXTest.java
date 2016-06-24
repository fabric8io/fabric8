/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.cxf.endpoint.jaxws;

import io.fabric8.cxf.endpoint.ManagedApi;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
    
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.testutil.common.TestUtil;


import org.junit.Assert;
import org.junit.Test;

public class JsonSchemaJMXTest extends Assert {
    
    static final String PORT = TestUtil.getPortNumber(JsonSchemaJMXTest.class);
    private static MBeanServerConnection mbsc;
    private static final String DEFAULT_JMXSERVICE_URL = 
        "service:jmx:rmi:///jndi/rmi://localhost:9914/jmxrmi";
    private static final Logger LOG = LogUtils.getL7dLogger(JsonSchemaJMXTest.class);
    
    
    private String jmxServerURL;
   
    @Test
    public void testJMXGetJsonSchema() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf
                .createBus("/io/fabric8/cxf/endpoint/jaxws/jmx-enable.xml");
        BusFactory.setDefaultBus(bus);
        String json = "";
        try {
            connectToMBserver();
            //test getJSONSchema 
            json = invokeEndpoint("getJSONSchema", null);
            parseJson(json);
            //test getJSONSchemaForClass
            json = invokeEndpoint("getJSONSchemaForClass", "SayHi");
            parseJson(json);
            //test getJSONSchemaForOperation
            json = invokeEndpoint("getJSONSchemaForOperation", "greetMe");
            parseJson(json);
        } catch (Throwable e) {
            e.printStackTrace();
            fail("invalid json for " + json);
        } finally {
            //ep.stop();
        }
    }
    
    private void connectToMBserver() throws IOException {
        jmxServerURL = jmxServerURL == null ? DEFAULT_JMXSERVICE_URL : jmxServerURL; 
        JMXServiceURL url = new JMXServiceURL(jmxServerURL);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        mbsc = jmxc.getMBeanServerConnection();
    }
    
    private ObjectName getEndpointObjectName() 
        throws MalformedObjectNameException, NullPointerException {
        StringBuilder buffer = new StringBuilder();
        String serviceName = "{http://apache.org/hello_world/services}SOAPService";
        String portName = "SoapPort";
        buffer.append(ManagedApi.DOMAIN_NAME + ":type=Bus.Service.Endpoint,");
        buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=\"" + serviceName + "\",");
        buffer.append(ManagementConstants.PORT_NAME_PROP + "=\"" + portName + "\",*");        
        return new ObjectName(buffer.toString());
    }
    
    private String invokeEndpoint(String operation, String operationPara) 
        throws Exception {
        ObjectName endpointName = null;
        ObjectName queryEndpointName;
        String ret = "";
        Object[] jmxPara = null;
        String[] jmxSig = null;
        if (operationPara != null) {
            jmxPara = new Object[]{operationPara};
            jmxSig = new String[] {String.class.getName()};
        } else {
            jmxPara = new Object[0];
            jmxSig = new String[0];
        }
        queryEndpointName = getEndpointObjectName();
        Set<ObjectName> endpointNames = CastUtils.cast(mbsc.queryNames(queryEndpointName, null));
        // now get the ObjectName with the busId
        Iterator<ObjectName> it = endpointNames.iterator();
    
        if (it.hasNext()) {
            // only deal with the first endpoint object which return from the list.
            endpointName = it.next();
            ret = (String)mbsc.invoke(endpointName, operation, jmxPara, jmxSig);
            LOG.info("invoke endpoint " + endpointName 
                               + " operation " + operation + " succeed!");
        }
        return ret;
    }
    
    private void parseJson(String json) throws Exception {
        JsonParser parser = new JsonFactory().createParser(json);
        while (parser.nextToken() != null) {
            //if it's an invalidate json will throw exception 
            //which could be caught by the test
        }
    }

}
