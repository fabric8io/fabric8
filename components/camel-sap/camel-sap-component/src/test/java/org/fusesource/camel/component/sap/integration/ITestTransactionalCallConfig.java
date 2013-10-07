/**
 * Copyright 2013 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.camel.component.sap.integration;


import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;

/**
 * Integration test cases for transactional destination RFC calls.
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ITestTransactionalCallConfig extends CamelSpringTestSupport {

	@Test
	public void testCall() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);    
        
        
        JCoDestination destination = JCoDestinationManager.getDestination("nplDest");
        
        Structure request = RfcUtil.getRequest(destination.getRepository(), "BAPI_FLCUST_CREATEFROMDATA");
        request.put("TEST_RUN", "");
        Structure customerData = (Structure) request.get("CUSTOMER_DATA");
        customerData.put("CUSTNAME", "Barney Rubble");
        customerData.put("FORM", "Mr.");
        customerData.put("STREET", "456 Cobblestone Ave");
        customerData.put("POBOX", "987");
        customerData.put("POSTCODE", "99999");
        customerData.put("CITY", "Bedrock");
        customerData.put("CONTR", "US");
        customerData.put("REGION", "PA");
        customerData.put("PHONE", "18005551212");
        customerData.put("CUSTTYPE", "P");
        customerData.put("DISCOUNT", "005");
        customerData.put("LANG_ISO", "en");
        
        template.sendBody("direct:createFlcustList", request);
        
        assertMockEndpointsSatisfied();
        
        Structure response = mock.getExchanges().get(0).getIn().getBody(Structure.class);
        Resource res = new XMLResourceImpl();
        res.getContents().add(response);
        res.save(System.out, null);
	}

	@Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:createFlcustList")
                  .to("sap:destination:nplDest:BAPI_FLCUST_CREATEFROMDATA?transacted=true")
                  .to("mock:result");
            }
        };
    }

	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(
				"org/fusesource/camel/component/sap/integration/ITestCallConfig.xml");
	}

}
