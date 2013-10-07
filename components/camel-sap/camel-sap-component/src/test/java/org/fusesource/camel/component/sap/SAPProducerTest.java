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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.ext.Environment;

/**
 * SAP Producer test cases.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
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
		
		assertThat("response.get(PARAM_LIST_CHAR_PARAM) returned '" +  response.get(PARAM_LIST_CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) response.get(PARAM_LIST_CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_NUM_PARAM) returned '" +  response.get(PARAM_LIST_NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) response.get(PARAM_LIST_NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_INT_PARAM) returned '" +  response.get(PARAM_LIST_INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) response.get(PARAM_LIST_INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_FLOAT_PARAM) returned '" +  response.get(PARAM_LIST_FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) response.get(PARAM_LIST_FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_BCD_PARAM) returned '" +  response.get(PARAM_LIST_BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) response.get(PARAM_LIST_BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_BINARY_PARAM) returned '" +  response.get(PARAM_LIST_BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) response.get(PARAM_LIST_BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_BINARY_ARRAY_PARAM) returned '" +  response.get(PARAM_LIST_BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) response.get(PARAM_LIST_BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_DATE_PARAM) returned '" +  response.get(PARAM_LIST_DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) response.get(PARAM_LIST_DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_TIME_PARAM) returned '" +  response.get(PARAM_LIST_TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) response.get(PARAM_LIST_TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_STRING_PARAM) returned '" +  response.get(PARAM_LIST_STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) response.get(PARAM_LIST_STRING_PARAM), is(STRING_PARAM_OUT_VAL));
		
		Structure structure = response.get(PARAM_LIST_STRUCTURE_PARAM, Structure.class);
		assertThat("structure.get(PARAM_LIST_STRUCTURE_PARAM) returned unexpected null value", structure, notNullValue());
		assertThat("structure.get(CHAR_PARAM) returned '" +  structure.get(CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) structure.get(CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("structure.get(NUM_PARAM) returned '" +  structure.get(NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) structure.get(NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("structure.get(INT_PARAM) returned '" +  structure.get(INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) structure.get(INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("structure.get(FLOAT_PARAM) returned '" +  structure.get(FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) structure.get(FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("structure.get(BCD_PARAM) returned '" +  structure.get(BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) structure.get(BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("structure.get(BINARY_PARAM) returned '" +  structure.get(BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) structure.get(BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("structure.get(BINARY_ARRAY_PARAM) returned '" +  structure.get(BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) structure.get(BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("structure.get(DATE_PARAM) returned '" +  structure.get(DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) structure.get(DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("structure.get(TIME_PARAM) returned '" +  structure.get(TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) structure.get(TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("structure.get(STRING_PARAM) returned '" +  structure.get(STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) structure.get(STRING_PARAM), is(STRING_PARAM_OUT_VAL));
		
		@SuppressWarnings("unchecked")
		Table<? extends Structure> table = response.get(PARAM_LIST_TABLE_PARAM, Table.class);
		assertThat("response.get(PARAM_LIST_TABLE_PARAM) returned unexpected null value", table, notNullValue());
		List<? extends Structure> rows = table.getRows();
		assertThat("rows.size() returned '" + rows.size() + "' instead of expected value of '1'", rows.size(), is(1));
		Structure tableRow = rows.get(0);
		assertThat("tableRow.get(CHAR_PARAM) returned '" +  tableRow.get(CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) tableRow.get(CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("tableRow.get(NUM_PARAM) returned '" +  tableRow.get(NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) tableRow.get(NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("tableRow.get(INT_PARAM) returned '" +  tableRow.get(INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) tableRow.get(INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("tableRow.get(FLOAT_PARAM) returned '" +  tableRow.get(FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) tableRow.get(FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("tableRow.get(BCD_PARAM) returned '" +  tableRow.get(BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) tableRow.get(BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("tableRow.get(BINARY_PARAM) returned '" +  tableRow.get(BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) tableRow.get(BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("tableRow.get(BINARY_ARRAY_PARAM) returned '" +  tableRow.get(BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) tableRow.get(BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("tableRow.get(DATE_PARAM) returned '" +  tableRow.get(DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) tableRow.get(DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("tableRow.get(TIME_PARAM) returned '" +  tableRow.get(TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) tableRow.get(TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("tableRow.get(STRING_PARAM) returned '" +  tableRow.get(STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) tableRow.get(STRING_PARAM), is(STRING_PARAM_OUT_VAL));
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
