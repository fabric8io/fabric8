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
package org.fusesource.camel.component.sap;


import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.ext.Environment;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JCoDestinationManager.class, Environment.class })
public class SAPProducerTest extends SAPTestSupport {
	
	@BeforeClass
	public static void setupBeforeClass() {
		PowerMockito.mockStatic(Environment.class);
	}

	@Test
	public void testProducer() throws Exception{ 
		
		//
		// Given
		//
		
		PowerMockito.mockStatic(JCoDestinationManager.class);
		Mockito.when(JCoDestinationManager.getDestination("TEST_DEST")).thenReturn(mockDestination);
		
		enhanceParameterListMetaData();
		Structure request = createAndPopulateRequest();
		
		getMockEndpoint("mock:result").expectedMessageCount(1);
		
		//
		// When
		//
		
		template.sendBody("direct:start", request);
		
		//
		// Then
		//
		
		assertMockEndpointsSatisfied();
		
		// check response
		Exchange exchange = getMockEndpoint("mock:result").getExchanges().get(0);
		Structure response = exchange.getIn().getBody(Structure.class);
		assertThat("The response returned by route is an unexpected null value", response, notNullValue());
		
		assertThat("RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_INT_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) RfcUtil.getValue(response, PARAM_LIST_INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_TIME_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(response, PARAM_LIST_TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_STRING_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_STRING_PARAM), is(STRING_PARAM_OUT_VAL));
		
		Structure structure = (Structure) RfcUtil.getValue(response, PARAM_LIST_STRUCTURE_PARAM);
		assertThat("RfcUtil.getValue(structure, PARAM_LIST_STRUCTURE_PARAM) returned unexpected null value", structure, notNullValue());
		assertThat("RfcUtil.getValue(structure, CHAR_PARAM) returned '" +  RfcUtil.getValue(structure, CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(structure, CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, NUM_PARAM) returned '" +  RfcUtil.getValue(structure, NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(structure, NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, INT_PARAM) returned '" +  RfcUtil.getValue(structure, INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) RfcUtil.getValue(structure, INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, FLOAT_PARAM) returned '" +  RfcUtil.getValue(structure, FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) RfcUtil.getValue(structure, FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, BCD_PARAM) returned '" +  RfcUtil.getValue(structure, BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) RfcUtil.getValue(structure, BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, BINARY_PARAM) returned '" +  RfcUtil.getValue(structure, BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(structure, BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, BINARY_ARRAY_PARAM) returned '" +  RfcUtil.getValue(structure, BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(structure, BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, DATE_PARAM) returned '" +  RfcUtil.getValue(structure, DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(structure, DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, TIME_PARAM) returned '" +  RfcUtil.getValue(structure, TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(structure, TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(structure, STRING_PARAM) returned '" +  RfcUtil.getValue(structure, STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(structure, STRING_PARAM), is(STRING_PARAM_OUT_VAL));
		
		@SuppressWarnings("unchecked")
		Table<? extends Structure> table = (Table<? extends Structure>) RfcUtil.getValue(response, PARAM_LIST_TABLE_PARAM);
		assertThat("RfcUtil.getValue(response, PARAM_LIST_TABLE_PARAM) returned unexpected null value", table, notNullValue());
		@SuppressWarnings("unchecked")
		List<? extends Structure> rows = (List<? extends Structure>) RfcUtil.getValue(table, "row");
		assertThat("rows.size() returned '" + rows.size() + "' instead of expected value of '1'", rows.size(), is(1));
		Structure tableRow = rows.get(0);
		assertThat("RfcUtil.getValue(tableRow, CHAR_PARAM) returned '" +  RfcUtil.getValue(tableRow, CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(tableRow, CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, NUM_PARAM) returned '" +  RfcUtil.getValue(tableRow, NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(tableRow, NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, INT_PARAM) returned '" +  RfcUtil.getValue(tableRow, INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) RfcUtil.getValue(tableRow, INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, FLOAT_PARAM) returned '" +  RfcUtil.getValue(tableRow, FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) RfcUtil.getValue(tableRow, FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, BCD_PARAM) returned '" +  RfcUtil.getValue(tableRow, BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) RfcUtil.getValue(tableRow, BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, BINARY_PARAM) returned '" +  RfcUtil.getValue(tableRow, BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(tableRow, BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, BINARY_ARRAY_PARAM) returned '" +  RfcUtil.getValue(tableRow, BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(tableRow, BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, DATE_PARAM) returned '" +  RfcUtil.getValue(tableRow, DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(tableRow, DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, TIME_PARAM) returned '" +  RfcUtil.getValue(tableRow, TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(tableRow, TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(tableRow, STRING_PARAM) returned '" +  RfcUtil.getValue(tableRow, STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(tableRow, STRING_PARAM), is(STRING_PARAM_OUT_VAL));
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from("direct:start").to("sap:destination:TEST_DEST:TEST_FUNCTION_MODULE").to("mock:result");
			}
		};
	}

}
