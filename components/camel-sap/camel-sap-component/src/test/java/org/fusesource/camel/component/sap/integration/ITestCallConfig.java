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
import org.fusesource.camel.component.sap.SAPComponent;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;

/**
 * Integration test cases for destination RFC calls.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ITestCallConfig extends CamelSpringTestSupport {

	@Test
	public void testCall() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);    
        
        
        JCoDestination destination = JCoDestinationManager.getDestination("nplDest");
        
        Structure request = RfcUtil.getRequest(destination.getRepository(), "BAPI_FLCUST_GETLIST");
        request.put("MAX_ROWS", 1);
        
        template.sendBody("direct:getFlcustList", request);
        
        assertMockEndpointsSatisfied();
        
        Structure response = mock.getExchanges().get(0).getIn().getBody(Structure.class);
        Resource res = new XMLResourceImpl();
        res.getContents().add(response);
        res.save(System.out, null);
	}

	@Test
	public void testComponentConfiguration() {
		SAPComponent component = (SAPComponent) context.getComponent("sap");
		
		// Validated Destination Data
		DestinationData nplDestinationData = component.getDestinationDataStore().get("nplDest");
		assertNotNull("Destination Data 'nplDest' not loaded into Destination Data Store", nplDestinationData);
		assertEquals("Destination Data Property 'ashost' has incorrect value set", "nplhost", nplDestinationData.getAshost());
		assertEquals("Destination Data Property 'sysnr' has incorrect value set", "42", nplDestinationData.getSysnr());
		assertEquals("Destination Data Property 'client' has incorrect value set", "001", nplDestinationData.getClient());
		assertEquals("Destination Data Property 'user' has incorrect value set", "developer", nplDestinationData.getUser());
		assertEquals("Destination Data Property 'passwd' has incorrect value set", "ch4ngeme", nplDestinationData.getPasswd());
		assertEquals("Destination Data Property 'lang' has incorrect value set", "en", nplDestinationData.getLang());

		// Validated Server Data
		ServerData nplServerData = component.getServerDataStore().get("nplServer");
		assertNotNull("Server Data 'nplServer' not loaded into Server Data Store", nplServerData);
		assertEquals("Server Data Property 'gwhost' has incorrect value set", "nplhost", nplServerData.getGwhost());
		assertEquals("Server Data Property 'gwserv' has incorrect value set", "3342", nplServerData.getGwserv());
		assertEquals("Server Data Property 'progid' has incorrect value set", "JCO_SERVER", nplServerData.getProgid());
		assertEquals("Server Data Property 'repositoryDestination' has incorrect value set", "nplDest", nplServerData.getRepositoryDestination());
		assertEquals("Server Data Property 'connectionCount' has incorrect value set", "2", nplServerData.getConnectionCount());
	}

	@Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:getFlcustList")
                  .to("sap:destination:nplDest:BAPI_FLCUST_GETLIST")
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
