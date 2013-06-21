package org.fusesource.camel.component.sap;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SAPComponentTest extends CamelSpringTestSupport {

    @Test
    public void testSAP() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);       
        
        assertMockEndpointsSatisfied();
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
                from("sap://foo")
                  .to("sap://bar")
                  .to("mock:result");
            }
        };
    }

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("org/fusesource/camel/component/sap/SAPComponentTestConfig.xml");
	}
}
