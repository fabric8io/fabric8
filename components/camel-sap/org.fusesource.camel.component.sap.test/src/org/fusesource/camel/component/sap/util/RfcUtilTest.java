package org.fusesource.camel.component.sap.util;

import junit.framework.Assert;

import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;

public class RfcUtilTest {

	protected DestinationDataStore destinationDataStore;

	@Before
	public void setUp() throws Exception {
		
		DestinationData destinationData = RfcFactory.eINSTANCE.createDestinationData();
		destinationData.setAshost("nplhost");
		destinationData.setSysnr("42");
		destinationData.setClient("001");
		destinationData.setUser("developer");
		destinationData.setPasswd("ch4ngeme");
		destinationData.setLang("en");
		
		destinationDataStore = RfcFactory.eINSTANCE.createDestinationDataStore();
		destinationDataStore.getEntries().put("TestDestination", destinationData);
		
		ComponentDestinationDataProvider.INSTANCE.addDestinationDataStore(destinationDataStore);
	}

	@After
	public void tearDown() throws Exception {
		//ComponentDestinationDataProvider.INSTANCE.removeDestinationDataStore(destinationDataStore);
	}

	@Test
	public void test() throws JCoException {
		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination"); 
		
		Structure request = RfcUtil.getRequest(jcoDestination.getRepository(), "STFC_CONNECTION");
		RfcUtil.setValue(request, "REQUTEXT", "Hello, SAP!");
		
		Structure response = RfcUtil.executeFunction(jcoDestination, "STFC_CONNECTION", request);
		
		String echoText = (String) RfcUtil.getValue(response, "ECHOTEXT");
		String respText = (String) RfcUtil.getValue(response, "RESPTEXT");
		
		Assert.assertEquals("ECHOTEXT of response different from REQUTEXT of request", "Hello, SAP!", echoText);
		System.out.println("RESPTEXT: " + respText);
	}

}
