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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.fusesource.camel.component.sap.model.rfc.DataType;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.FieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.FunctionTemplate;
import org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RepositoryData;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * SAP Component Test cases.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class SAPComponentTest extends SAPTestSupport {

	@Test
	public void testFillFromRequest() throws Exception {
		// Given
		
		enhanceParameterListMetaData();
		Structure request = createAndPopulateRequest();
		
		// When

		RfcUtil.fillJCoParameterListsFromRequest(request, mockFunction);
		
		// Then
		
		assertThat("RfcUtil.getRequest(mockRepository, FUNCTION_MODULE_NAME) returned unexpected value", request, notNullValue());

		verify(mockParameterListCharField, times(1)).setValue((Object)CHAR_PARAM_IN_VAL);
		verify(mockCharField, times(2)).setValue((Object)CHAR_PARAM_IN_VAL);

		verify(mockParameterListNumField, times(1)).setValue((Object)NUM_PARAM_IN_VAL);
		verify(mockNumField, times(2)).setValue((Object)NUM_PARAM_IN_VAL);

		verify(mockParameterListIntField, times(1)).setValue((Object)INT_PARAM_IN_VAL);
		verify(mockIntField, times(2)).setValue((Object)INT_PARAM_IN_VAL);

		verify(mockParameterListFloatField, times(1)).setValue((Object)FLOAT_PARAM_IN_VAL);
		verify(mockFloatField, times(2)).setValue((Object)FLOAT_PARAM_IN_VAL);

		verify(mockParameterListBCDField, times(1)).setValue((Object)BCD_PARAM_IN_VAL);
		verify(mockBCDField, times(2)).setValue((Object)BCD_PARAM_IN_VAL);

		verify(mockParameterListBinaryField, times(1)).setValue((Object)BINARY_PARAM_IN_VAL);
		verify(mockBinaryField, times(2)).setValue((Object)BINARY_PARAM_IN_VAL);

		verify(mockParameterListBinaryArrayField, times(1)).setValue((Object)BINARY_ARRAY_PARAM_IN_VAL);
		verify(mockBinaryArrayField, times(2)).setValue((Object)BINARY_ARRAY_PARAM_IN_VAL);

		verify(mockParameterListDateField, times(1)).setValue((Object)DATE_PARAM_IN_VAL);
		verify(mockDateField, times(2)).setValue((Object)DATE_PARAM_IN_VAL);

		verify(mockParameterListTimeField, times(1)).setValue((Object)TIME_PARAM_IN_VAL);
		verify(mockTimeField, times(2)).setValue((Object)TIME_PARAM_IN_VAL);

		verify(mockParameterListStringField, times(1)).setValue((Object)STRING_PARAM_IN_VAL);
		verify(mockStringField, times(2)).setValue((Object)STRING_PARAM_IN_VAL);
		
		verify(mockParameterListStructureField, times(1)).isStructure();
		verify(mockParameterListStructureField, times(1)).getStructure();

		verify(mockParameterListTableField, times(1)).isTable();
		verify(mockParameterListTableField, times(1)).getTable();

	}

	@Test
	public void testExtractIntoResponse() throws Exception {
		// Given
		enhanceParameterListMetaData();
		
		// When
		Structure response = RfcUtil.getResponse(mockRepository, FUNCTION_MODULE_NAME);
		
		RfcUtil.extractJCoParameterListsIntoResponse(mockFunction, response);
		
		// Then
		assertThat("recordFactory.createMappedRecord(OUTPUT_RECORD_NAME) returned unexpected null value", response, notNullValue());
		
		assertThat("response.get(PARAM_LIST_CHAR_PARAM) returned '" +  response.get(PARAM_LIST_CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) response.get(PARAM_LIST_CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_NUM_PARAM) returned '" +  response.get(PARAM_LIST_NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) response.get(PARAM_LIST_NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_INT_PARAM) returned '" +  response.get(PARAM_LIST_INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) response.get(PARAM_LIST_INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_FLOAT_PARAM) returned '" +  response.get(PARAM_LIST_FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) response.get(PARAM_LIST_FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_BCD_PARAM) returned '" +  response.get(PARAM_LIST_BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) response.get(PARAM_LIST_BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_BINARY_PARAM) returned '" +  response.get(PARAM_LIST_BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) response.get(PARAM_LIST_BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_BINARY_ARRAY_PARAM) returned '" +  response.get(PARAM_LIST_BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) response.get(PARAM_LIST_BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("outputRecord.getP(ARAM_LIST_DATE_PARAM) returned '" +  response.get(PARAM_LIST_DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) response.get(PARAM_LIST_DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_TIME_PARAM) returned '" +  response.get(PARAM_LIST_TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) response.get(PARAM_LIST_TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("response.get(PARAM_LIST_STRING_PARAM) returned '" +  response.get(PARAM_LIST_STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) response.get(PARAM_LIST_STRING_PARAM), is(STRING_PARAM_OUT_VAL));
		
		Structure structure = (Structure) response.get(PARAM_LIST_STRUCTURE_PARAM);
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
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSynchronousRfc() throws Exception {
		// Given
		
		enhanceParameterListMetaData();
		Structure request = createAndPopulateRequest();
		Structure structure;
		Table<? extends Structure> table;
		
		// When

		Structure response = RfcUtil.executeFunction(mockDestination, FUNCTION_MODULE_NAME, request);

		// Then

		assertThat("RfcUtil.getRequest(mockRepository, FUNCTION_MODULE_NAME) returned unexpected value", request, notNullValue());

		verify(mockParameterListCharField, times(1)).setValue((Object)CHAR_PARAM_IN_VAL);
		verify(mockCharField, times(2)).setValue((Object)CHAR_PARAM_IN_VAL);

		verify(mockParameterListNumField, times(1)).setValue((Object)NUM_PARAM_IN_VAL);
		verify(mockNumField, times(2)).setValue((Object)NUM_PARAM_IN_VAL);

		verify(mockParameterListIntField, times(1)).setValue((Object)INT_PARAM_IN_VAL);
		verify(mockIntField, times(2)).setValue((Object)INT_PARAM_IN_VAL);

		verify(mockParameterListFloatField, times(1)).setValue((Object)FLOAT_PARAM_IN_VAL);
		verify(mockFloatField, times(2)).setValue((Object)FLOAT_PARAM_IN_VAL);

		verify(mockParameterListBCDField, times(1)).setValue((Object)BCD_PARAM_IN_VAL);
		verify(mockBCDField, times(2)).setValue((Object)BCD_PARAM_IN_VAL);

		verify(mockParameterListBinaryField, times(1)).setValue((Object)BINARY_PARAM_IN_VAL);
		verify(mockBinaryField, times(2)).setValue((Object)BINARY_PARAM_IN_VAL);

		verify(mockParameterListBinaryArrayField, times(1)).setValue((Object)BINARY_ARRAY_PARAM_IN_VAL);
		verify(mockBinaryArrayField, times(2)).setValue((Object)BINARY_ARRAY_PARAM_IN_VAL);

		verify(mockParameterListDateField, times(1)).setValue((Object)DATE_PARAM_IN_VAL);
		verify(mockDateField, times(2)).setValue((Object)DATE_PARAM_IN_VAL);

		verify(mockParameterListTimeField, times(1)).setValue((Object)TIME_PARAM_IN_VAL);
		verify(mockTimeField, times(2)).setValue((Object)TIME_PARAM_IN_VAL);

		verify(mockParameterListStringField, times(1)).setValue((Object)STRING_PARAM_IN_VAL);
		verify(mockStringField, times(2)).setValue((Object)STRING_PARAM_IN_VAL);
		
		verify(mockParameterListStructureField, times(2)).isStructure();
		verify(mockParameterListStructureField, times(2)).getStructure();

		verify(mockParameterListTableField, times(2)).isTable();
		verify(mockParameterListTableField, times(2)).getTable();

		assertThat("Structure response = RfcUtil.executeFunction(mockDestination, FUNCTION_MODULE_NAME, request returned unexpected null value", response, notNullValue());
		
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
		
		structure = response.get(PARAM_LIST_STRUCTURE_PARAM,Structure.class);
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
		
		table = response.get(PARAM_LIST_TABLE_PARAM, Table.class);
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
	
	@Test
	public void testComponentConfiguration() {
		SAPComponent component = (SAPComponent) context.getComponent("sap");
		
		//
		// Validate Destination Data
		//
		
		DestinationData nplDestinationData = component.getDestinationDataStore().get("nplDest");
		assertNotNull("Destination Data 'nplDest' not loaded into Destination Data Store", nplDestinationData);
		assertEquals("Destination Data Property 'ashost' has incorrect value set", "nplhost", nplDestinationData.getAshost());
		assertEquals("Destination Data Property 'sysnr' has incorrect value set", "42", nplDestinationData.getSysnr());
		assertEquals("Destination Data Property 'client' has incorrect value set", "001", nplDestinationData.getClient());
		assertEquals("Destination Data Property 'user' has incorrect value set", "developer", nplDestinationData.getUser());
		assertEquals("Destination Data Property 'passwd' has incorrect value set", "ch4ngeme", nplDestinationData.getPasswd());
		assertEquals("Destination Data Property 'lang' has incorrect value set", "en", nplDestinationData.getLang());
		
		// 
		// Validate Server Data
		//
		
		ServerData nplServerData = component.getServerDataStore().get("nplServer");
		assertNotNull("Server Data 'nplServer' not loaded into Server Data Store", nplServerData);
		assertEquals("Server Data Property 'gwhost' has incorrect value set", "nplhost", nplServerData.getGwhost());
		assertEquals("Server Data Property 'gwserv' has incorrect value set", "3342", nplServerData.getGwserv());
		assertEquals("Server Data Property 'progid' has incorrect value set", "JCO_SERVER", nplServerData.getProgid());
		assertEquals("Server Data Property 'repositoryDestination' has incorrect value set", "nplDest", nplServerData.getRepositoryDestination());
		assertEquals("Server Data Property 'connectionCount' has incorrect value set", "2", nplServerData.getConnectionCount());
		
		//
		// Validate Repository Data
		//
		
		// Changing Parameter List
		//
		RepositoryData nplRepositoryData = component.getRepositoryDataStore().get("nplServer");
		assertNotNull("Respository Data 'nplServer' not loaded into Repository Data Store", nplRepositoryData);
		FunctionTemplate paramTestFunctionTemplate = nplRepositoryData.getFunctionTemplates().get("PARAM_TEST");
		assertNotNull("Function Template 'PARAM_TEST' not loaded into Repository Data Store", paramTestFunctionTemplate);
		// PARAM_LIST_CHAR_PARAM
		ListFieldMetaData paramListCharParam = paramTestFunctionTemplate.getChangingParameterList().get(0);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_CHAR_PARAM' not loaded into Repository Data Store", paramListCharParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_CHAR_PARAM", paramListCharParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.CHAR, paramListCharParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 10, paramListCharParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 20, paramListCharParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListCharParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListCharParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListCharParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListCharParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListCharParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListCharParam.isOptional());
		// PARAM_LIST_NUM_PARAM
		ListFieldMetaData paramListNumParam = paramTestFunctionTemplate.getChangingParameterList().get(1);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_NUM_PARAM' not loaded into Repository Data Store", paramListNumParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_NUM_PARAM", paramListNumParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.NUM, paramListNumParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 10, paramListNumParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 10, paramListNumParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListNumParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListNumParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListNumParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListNumParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListNumParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListNumParam.isOptional());
		// PARAM_LIST_INT_PARAM
		ListFieldMetaData paramListIntParam = paramTestFunctionTemplate.getChangingParameterList().get(2);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_INT_PARAM' not loaded into Repository Data Store", paramListIntParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_INT_PARAM", paramListIntParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.INT, paramListIntParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 4, paramListIntParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 4, paramListIntParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListIntParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListIntParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListIntParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListIntParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListIntParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListIntParam.isOptional());
		// PARAM_LIST_FLOAT_PARAM
		ListFieldMetaData paramListFloatParam = paramTestFunctionTemplate.getChangingParameterList().get(3);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_FLOAT_PARAM' not loaded into Repository Data Store", paramListFloatParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_FLOAT_PARAM", paramListFloatParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.FLOAT, paramListFloatParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 8, paramListFloatParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 8, paramListFloatParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListFloatParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListFloatParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListFloatParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListFloatParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListFloatParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListFloatParam.isOptional());
		// PARAM_LIST_BCD_PARAM
		ListFieldMetaData paramListBCDParam = paramTestFunctionTemplate.getChangingParameterList().get(4);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_BCD_PARAM' not loaded into Repository Data Store", paramListBCDParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_BCD_PARAM", paramListBCDParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.BCD, paramListBCDParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 9, paramListBCDParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 9, paramListBCDParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 14, paramListBCDParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBCDParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBCDParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListBCDParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBCDParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBCDParam.isOptional());
		// PARAM_LIST_BINARY_PARAM
		ListFieldMetaData paramListBinaryParam = paramTestFunctionTemplate.getChangingParameterList().get(5);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_BINARY_PARAM' not loaded into Repository Data Store", paramListBinaryParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_BINARY_PARAM", paramListBinaryParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.BYTE, paramListBinaryParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 1, paramListBinaryParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 1, paramListBinaryParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListBinaryParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListBinaryParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryParam.isOptional());
		// PARAM_LIST_BINARY_ARRAY_PARAM
		ListFieldMetaData paramListBinaryArrayParam = paramTestFunctionTemplate.getChangingParameterList().get(6);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_BINARY_ARRAY_PARAM' not loaded into Repository Data Store", paramListBinaryArrayParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_BINARY_ARRAY_PARAM", paramListBinaryArrayParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.BYTE, paramListBinaryArrayParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 10, paramListBinaryArrayParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 10, paramListBinaryArrayParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListBinaryArrayParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryArrayParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryArrayParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListBinaryArrayParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryArrayParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListBinaryArrayParam.isOptional());
		// PARAM_LIST_DATE_PARAM
		ListFieldMetaData paramListDateParam = paramTestFunctionTemplate.getChangingParameterList().get(7);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_DATE_PARAM' not loaded into Repository Data Store", paramListDateParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_DATE_PARAM", paramListDateParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.DATE, paramListDateParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 8, paramListDateParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 16, paramListDateParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListDateParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListDateParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListDateParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListDateParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListDateParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListDateParam.isOptional());
		// PARAM_LIST_TIME_PARAM
		ListFieldMetaData paramListTimeParam = paramTestFunctionTemplate.getChangingParameterList().get(8);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_TIME_PARAM' not loaded into Repository Data Store", paramListTimeParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_TIME_PARAM", paramListTimeParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.TIME, paramListTimeParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 6, paramListTimeParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 12, paramListTimeParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListTimeParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTimeParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTimeParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListTimeParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTimeParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTimeParam.isOptional());
		// PARAM_LIST_STRING_PARAM
		ListFieldMetaData paramListStringParam = paramTestFunctionTemplate.getChangingParameterList().get(9);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_STRING_PARAM' not loaded into Repository Data Store", paramListStringParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_STRING_PARAM", paramListStringParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.STRING, paramListStringParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", 8, paramListStringParam.getByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 8, paramListStringParam.getUnicodeByteLength());
		assertEquals("ListFieldMetaData property has incorrect value set", 0, paramListStringParam.getDecimals());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStringParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStringParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListStringParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStringParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStringParam.isOptional());
		// PARAM_LIST_STRUCTURE_PARAM
		ListFieldMetaData paramListStructureParam = paramTestFunctionTemplate.getChangingParameterList().get(10);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_STRUCTURE_PARAM' not loaded into Repository Data Store", paramListStructureParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_STRUCTURE_PARAM", paramListStructureParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.STRUCTURE, paramListStructureParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStructureParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStructureParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListStructureParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStructureParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListStructureParam.isOptional());
		assertNotNull("RecordMetaData for 'PARAM_LIST_STRUCTURE_PARAM' not loaded into Repository Data Store", paramListStructureParam.getRecordMetaData());
		List<FieldMetaData> zjbossStructure = paramListStructureParam.getRecordMetaData().getRecordFieldMetaData();
		assertNotNull("ComplexFieldMetaData for 'PARAM_LIST_STRUCTURE_PARAM' not loaded into Repository Data Store", zjbossStructure);
		// PARAM_LIST_TABLE_PARAM
		ListFieldMetaData paramListTableParam = paramTestFunctionTemplate.getChangingParameterList().get(11);
		assertNotNull("ListFieldMetaData for 'PARAM_LIST_TABLE_PARAM' not loaded into Repository Data Store", paramListTableParam);
		assertEquals("ListFieldMetaData property has incorrect value set", "PARAM_LIST_TABLE_PARAM", paramListTableParam.getName());
		assertEquals("ListFieldMetaData property has incorrect value set", DataType.TABLE, paramListTableParam.getType());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTableParam.isImport());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTableParam.isExport());
		assertEquals("ListFieldMetaData property has incorrect value set", true, paramListTableParam.isChanging());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTableParam.isException());
		assertEquals("ListFieldMetaData property has incorrect value set", false, paramListTableParam.isOptional());
		assertNotNull("RecordMetaData for 'PARAM_LIST_TABLE_PARAM' not loaded into Repository Data Store", paramListTableParam.getRecordMetaData());
		List<FieldMetaData> zjbossLineType = paramListTableParam.getRecordMetaData().getRecordFieldMetaData();
		assertNotNull("ComplexFieldMetaData for 'PARAM_LIST_TABLE_PARAM' not loaded into Repository Data Store", zjbossLineType);
		
		// ZJBOSS_STRUCTURE
		//
		// CHAR_PARAM
		FieldMetaData charParam = zjbossStructure.get(0);
		assertNotNull("FieldMetaData for 'CHAR_PARAM' not loaded into Repository Data Store", charParam);
		assertEquals("FieldMetaData property has incorrect value set", "CHAR_PARAM", charParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.CHAR, charParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 10, charParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 0, charParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 20, charParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 0, charParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, charParam.getDecimals());
		// NUM_PARAM
		FieldMetaData numParam = zjbossStructure.get(1);
		assertNotNull("FieldMetaData for 'NUM_PARAM' not loaded into Repository Data Store", numParam);
		assertEquals("FieldMetaData property has incorrect value set", "NUM_PARAM", numParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.NUM, numParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 10, numParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 10, numParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 20, numParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 20, numParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, numParam.getDecimals());
		// INT_PARAM
		FieldMetaData intParam = zjbossStructure.get(2);
		assertNotNull("FieldMetaData for 'INT_PARAM' not loaded into Repository Data Store", intParam);
		assertEquals("FieldMetaData property has incorrect value set", "INT_PARAM", intParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.INT, intParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 4, intParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 20, intParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 4, intParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 40, intParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, intParam.getDecimals());
		// FLOAT_PARAM
		FieldMetaData floatParam = zjbossStructure.get(3);
		assertNotNull("FieldMetaData for 'FLOAT_PARAM' not loaded into Repository Data Store", floatParam);
		assertEquals("FieldMetaData property has incorrect value set", "FLOAT_PARAM", floatParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.FLOAT, floatParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 8, floatParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 24, floatParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 8, floatParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 48, floatParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 16, floatParam.getDecimals());
		// BCD_PARAM
		FieldMetaData bcdParam = zjbossStructure.get(4);
		assertNotNull("FieldMetaData for 'BCD_PARAM' not loaded into Repository Data Store", bcdParam);
		assertEquals("FieldMetaData property has incorrect value set", "BCD_PARAM", bcdParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.BCD, bcdParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 9, bcdParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 32, bcdParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 9, bcdParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 56, bcdParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 14, bcdParam.getDecimals());
		// BINARY_PARAM
		FieldMetaData binaryParam = zjbossStructure.get(5);
		assertNotNull("FieldMetaData for 'BINARY_PARAM' not loaded into Repository Data Store", binaryParam);
		assertEquals("FieldMetaData property has incorrect value set", "BINARY_PARAM", binaryParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.BYTE, binaryParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 1, binaryParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 41, binaryParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 1, binaryParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 65, binaryParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, binaryParam.getDecimals());
		// BINARY_ARRAY_PARAM
		FieldMetaData binaryArrayParam = zjbossStructure.get(6);
		assertNotNull("FieldMetaData for 'BINARY_ARRAY_PARAM' not loaded into Repository Data Store", binaryArrayParam);
		assertEquals("FieldMetaData property has incorrect value set", "BINARY_ARRAY_PARAM", binaryArrayParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.BYTE, binaryArrayParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 10, binaryArrayParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 42, binaryArrayParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 10, binaryArrayParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 66, binaryArrayParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, binaryArrayParam.getDecimals());
		// DATE_PARAM
		FieldMetaData dateParam = zjbossStructure.get(7);
		assertNotNull("FieldMetaData for 'DATE_PARAM' not loaded into Repository Data Store", dateParam);
		assertEquals("FieldMetaData property has incorrect value set", "DATE_PARAM", dateParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.DATE, dateParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 8, dateParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 52, dateParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 16, dateParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 76, dateParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, dateParam.getDecimals());
		// TIME_PARAM
		FieldMetaData timeParam = zjbossStructure.get(8);
		assertNotNull("FieldMetaData for 'TIME_PARAM' not loaded into Repository Data Store", timeParam);
		assertEquals("FieldMetaData property has incorrect value set", "TIME_PARAM", timeParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.TIME, timeParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 6, timeParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 60, timeParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 12, timeParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 92, timeParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, timeParam.getDecimals());
		// STRING_PARAM
		FieldMetaData stringParam = zjbossStructure.get(9);
		assertNotNull("FieldMetaData for 'STRING_PARAM' not loaded into Repository Data Store", stringParam);
		assertEquals("FieldMetaData property has incorrect value set", "STRING_PARAM", stringParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.STRING, stringParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 8, stringParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 68, stringParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 8, stringParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 104, stringParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, stringParam.getDecimals());
		
		// ZJBOSS_LINE_TYPE
		//
		// CHAR_PARAM
		charParam = zjbossLineType.get(0);
		assertNotNull("FieldMetaData for 'CHAR_PARAM' not loaded into Repository Data Store", charParam);
		assertEquals("FieldMetaData property has incorrect value set", "CHAR_PARAM", charParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.CHAR, charParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 10, charParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 0, charParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 20, charParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 0, charParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, charParam.getDecimals());
		// NUM_PARAM
		numParam = zjbossLineType.get(1);
		assertNotNull("FieldMetaData for 'NUM_PARAM' not loaded into Repository Data Store", numParam);
		assertEquals("FieldMetaData property has incorrect value set", "NUM_PARAM", numParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.NUM, numParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 10, numParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 10, numParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 20, numParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 20, numParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, numParam.getDecimals());
		// INT_PARAM
		intParam = zjbossLineType.get(2);
		assertNotNull("FieldMetaData for 'INT_PARAM' not loaded into Repository Data Store", intParam);
		assertEquals("FieldMetaData property has incorrect value set", "INT_PARAM", intParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.INT, intParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 4, intParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 20, intParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 4, intParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 40, intParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, intParam.getDecimals());
		// FLOAT_PARAM
		floatParam = zjbossLineType.get(3);
		assertNotNull("FieldMetaData for 'FLOAT_PARAM' not loaded into Repository Data Store", floatParam);
		assertEquals("FieldMetaData property has incorrect value set", "FLOAT_PARAM", floatParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.FLOAT, floatParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 8, floatParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 24, floatParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 8, floatParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 48, floatParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 16, floatParam.getDecimals());
		// BCD_PARAM
		bcdParam = zjbossLineType.get(4);
		assertNotNull("FieldMetaData for 'BCD_PARAM' not loaded into Repository Data Store", bcdParam);
		assertEquals("FieldMetaData property has incorrect value set", "BCD_PARAM", bcdParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.BCD, bcdParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 9, bcdParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 32, bcdParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 9, bcdParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 56, bcdParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 14, bcdParam.getDecimals());
		// BINARY_PARAM
		binaryParam = zjbossLineType.get(5);
		assertNotNull("FieldMetaData for 'BINARY_PARAM' not loaded into Repository Data Store", binaryParam);
		assertEquals("FieldMetaData property has incorrect value set", "BINARY_PARAM", binaryParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.BYTE, binaryParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 1, binaryParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 41, binaryParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 1, binaryParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 65, binaryParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, binaryParam.getDecimals());
		// BINARY_ARRAY_PARAM
		binaryArrayParam = zjbossLineType.get(6);
		assertNotNull("FieldMetaData for 'BINARY_ARRAY_PARAM' not loaded into Repository Data Store", binaryArrayParam);
		assertEquals("FieldMetaData property has incorrect value set", "BINARY_ARRAY_PARAM", binaryArrayParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.BYTE, binaryArrayParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 10, binaryArrayParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 42, binaryArrayParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 10, binaryArrayParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 66, binaryArrayParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, binaryArrayParam.getDecimals());
		// DATE_PARAM
		dateParam = zjbossLineType.get(7);
		assertNotNull("FieldMetaData for 'DATE_PARAM' not loaded into Repository Data Store", dateParam);
		assertEquals("FieldMetaData property has incorrect value set", "DATE_PARAM", dateParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.DATE, dateParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 8, dateParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 52, dateParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 16, dateParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 76, dateParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, dateParam.getDecimals());
		// TIME_PARAM
		timeParam = zjbossStructure.get(8);
		assertNotNull("FieldMetaData for 'TIME_PARAM' not loaded into Repository Data Store", timeParam);
		assertEquals("FieldMetaData property has incorrect value set", "TIME_PARAM", timeParam.getName());
		assertEquals("FieldMetaData property has incorrect value set", DataType.TIME, timeParam.getType());
		assertEquals("FieldMetaData property has incorrect value set", 6, timeParam.getByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 60, timeParam.getByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 12, timeParam.getUnicodeByteLength());
		assertEquals("FieldMetaData property has incorrect value set", 92, timeParam.getUnicodeByteOffset());
		assertEquals("FieldMetaData property has incorrect value set", 0, timeParam.getDecimals());
		
	}
	
	@Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start")
                  .to("sap:destination:nplDest:BAPI_FLCUST_GETLIST")
                  .to("mock:result");
            }
        };
    }

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("org/fusesource/camel/component/sap/SAPComponentTestConfig.xml");
	}
	
}
