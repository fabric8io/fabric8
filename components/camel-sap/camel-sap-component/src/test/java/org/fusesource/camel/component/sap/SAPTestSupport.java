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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFieldIterator;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoRequest;
import com.sap.conn.jco.JCoResponse;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;

/**
 * Support base class for SAP test cases. 
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public abstract class SAPTestSupport extends CamelSpringTestSupport {
	/*********************************************************************
	 * Test Destination, Repository, and Function Module
	 *********************************************************************/

	public static final String DESTINATION_NAME = "TEST_DEST";
	public static final String REPOSITORY_NAME = "TEST_REPOSITORY";
	public static final String FUNCTION_MODULE_NAME = "TEST_FUNCTION_MODULE";
	
	/*********************************************************************
	 * Test Structure Names
	 *********************************************************************/

	public static final String STRUCTURE_TYPE_NAME = "ZSTRUCTURE";
	
	/*********************************************************************
	 * Test Parameter List Names
	 *********************************************************************/
	public static final String PARAM_LIST_CHAR_PARAM = "PARAM_LIST_CHAR_PARAM";
	public static final String PARAM_LIST_NUM_PARAM = "PARAM_LIST_NUM_PARAM";
	public static final String PARAM_LIST_INT_PARAM = "PARAM_LIST_INT_PARAM";
	public static final String PARAM_LIST_FLOAT_PARAM = "PARAM_LIST_FLOAT_PARAM";
	public static final String PARAM_LIST_BCD_PARAM = "PARAM_LIST_BCD_PARAM";
	public static final String PARAM_LIST_BINARY_PARAM = "PARAM_LIST_BINARY_PARAM";
	public static final String PARAM_LIST_BINARY_ARRAY_PARAM = "PARAM_LIST_BINARY_ARRAY_PARAM";
	public static final String PARAM_LIST_DATE_PARAM = "PARAM_LIST_DATE_PARAM";
	public static final String PARAM_LIST_TIME_PARAM = "PARAM_LIST_TIME_PARAM";
	public static final String PARAM_LIST_STRING_PARAM = "PARAM_LIST_STRING_PARAM";
	public static final String PARAM_LIST_STRUCTURE_PARAM = "PARAM_LIST_STRUCTURE_PARAM";
	public static final String PARAM_LIST_TABLE_PARAM = "PARAM_LIST_TABLE_PARAM";

	/*********************************************************************
	 * Test Parameter Names
	 *********************************************************************/
	public static final String CHAR_PARAM = "CHAR_PARAM";
	public static final String NUM_PARAM = "NUM_PARAM";
	public static final String INT_PARAM = "INT_PARAM";
	public static final String FLOAT_PARAM = "FLOAT_PARAM";
	public static final String BCD_PARAM = "BCD_PARAM";
	public static final String BINARY_PARAM = "BINARY_PARAM";
	public static final String BINARY_ARRAY_PARAM = "BINARY_ARRAY_PARAM";
	public static final String DATE_PARAM = "DATE_PARAM";
	public static final String TIME_PARAM = "TIME_PARAM";
	public static final String STRING_PARAM = "STRING_PARAM";

	/*********************************************************************
	 * Test Parameter Input Values
	 *********************************************************************/
	public static final String CHAR_PARAM_IN_VAL = "ABCDEFGHIJ";
	public static final String NUM_PARAM_IN_VAL = "0123456789";
	public static final int INT_PARAM_IN_VAL = 0x75555555;
	public static final double FLOAT_PARAM_IN_VAL = Math.pow(10, 38); // This seems to be the biggest double to not come
																	// back as infinity.
	public static final BigDecimal BCD_PARAM_IN_VAL = new BigDecimal("100.00000000000001");
	public static final byte[] BINARY_PARAM_IN_VAL = new byte[] { (byte) 0x55 };
	public static final byte[] BINARY_ARRAY_PARAM_IN_VAL = new byte[] { (byte) 0xFF, (byte) 0x0F, (byte) 0x1E,
			(byte) 0x2D, (byte) 0x3C, (byte) 0x4B, (byte) 0x5A, (byte) 0x60, (byte) 0x79, (byte) 0x88 };
	public static final Date DATE_PARAM_IN_VAL = new GregorianCalendar(1861, 03, 12).getTime();
	public static final Date TIME_PARAM_IN_VAL = new GregorianCalendar(1970, 0, 1, 12, 15, 30).getTime();
	public static final String STRING_PARAM_IN_VAL = "Four score and seven years ago ...";

	/*********************************************************************
	 * Test Parameter Output Values
	 *********************************************************************/

	public static final String CHAR_PARAM_OUT_VAL = "ZYXWVUTSRQ";
	public static final String NUM_PARAM_OUT_VAL = "9876543210";
	public static final int INT_PARAM_OUT_VAL = 0x7AAAAAAA;
	public static final double FLOAT_PARAM_OUT_VAL = 2 * Math.pow(10, 38);
	public static final BigDecimal BCD_PARAM_OUT_VAL = new BigDecimal("200.00000000000002");
	public static final byte[] BINARY_PARAM_OUT_VAL = new byte[] { (byte) 0xAA };
	public static final byte[] BINARY_ARRAY_PARAM_OUT_VAL = new byte[] { (byte) 0x88, (byte) 0x79, (byte) 0x60,
			(byte) 0x5A, (byte) 0x4B, (byte) 0x3C, (byte) 0x2D, (byte) 0x1E, (byte) 0x0F, (byte) 0xFF };
	public static final Date DATE_PARAM_OUT_VAL = new GregorianCalendar(1865, 03, 9).getTime();
	public static final Date TIME_PARAM_OUT_VAL = new GregorianCalendar(1970, 0, 1, 23, 45, 15).getTime();
	public static final String STRING_PARAM_OUT_VAL = "... shall not perish from this earth.";
	
	/****************************************************************************
	 * Mocks
	 ****************************************************************************/
	
	protected JCoRepository mockRepository;
	protected JCoFunction mockFunction;
	protected JCoFunctionTemplate mockFunctionTemplate;
	protected JCoListMetaData mockImportParameterListMetaData;
	protected JCoListMetaData mockChangingParameterListMetaData;
	protected JCoListMetaData mockTableParameterListMetaData;
	protected JCoListMetaData mockExportParameterListMetaData;
	protected JCoRecordMetaData mockStructureMetaData;
	protected JCoParameterList mockImportParameterList;
	protected JCoParameterList mockChangingParameterList;
	protected JCoParameterList mockExportParameterList;
	protected JCoParameterList mockTableParameterList;
	protected JCoFieldIterator mockParameterListFieldIterator;
	protected JCoFieldIterator mockTableParameterListFieldIterator;
	protected JCoField mockParameterListCharField;
	protected JCoField mockParameterListNumField;
	protected JCoField mockParameterListIntField;
	protected JCoField mockParameterListFloatField;
	protected JCoField mockParameterListBCDField;
	protected JCoField mockParameterListBinaryField;
	protected JCoField mockParameterListBinaryArrayField;
	protected JCoField mockParameterListDateField;
	protected JCoField mockParameterListTimeField;
	protected JCoField mockParameterListStringField;
	protected JCoField mockParameterListStructureField;
	protected JCoField mockParameterListTableField;
	protected JCoFieldIterator mockStructureFieldIterator;
	protected JCoField mockCharField;
	protected JCoField mockNumField;
	protected JCoField mockIntField;
	protected JCoField mockFloatField;
	protected JCoField mockBCDField;
	protected JCoField mockBinaryField;
	protected JCoField mockBinaryArrayField;
	protected JCoField mockDateField;
	protected JCoField mockTimeField;
	protected JCoField mockStringField;
	protected JCoStructure mockStructure;
	protected JCoFieldIterator mockTableFieldIterator;
	protected JCoRequest mockRequest;
	protected JCoResponse mockResponse;
	protected JCoDestination mockDestination;
	protected JCoFieldIterator mockEmptyParameterListFieldIterator;
	
	public void doPreSetup() throws Exception {	
		super.doPreSetup();
		
		/* Create mocks for repository and function template */
		mockDestination = mock(JCoDestination.class);
		mockRepository = mock(JCoRepository.class);
		mockFunction = mock(JCoFunction.class);
		mockFunctionTemplate = mock(JCoFunctionTemplate.class);
		mockRequest = mock(JCoRequest.class);
		mockResponse = mock(JCoResponse.class);
		
		/* Create mocks for parameter list */
		mockImportParameterList = mock(JCoParameterList.class, "ImportParameterList");
		mockChangingParameterList = mock(JCoParameterList.class, "ChangingParameterList");
		mockExportParameterList = mock(JCoParameterList.class, "ExportParameterList");
		mockTableParameterList = mock(JCoParameterList.class, "TableParameterList");
		
		/* Create mock field iterator for parameter list  */
		mockEmptyParameterListFieldIterator = mock(JCoFieldIterator.class, "EmptyParameterListFieldIterator");
		mockParameterListFieldIterator = mock(JCoFieldIterator.class, "ParameterListFieldIterator");
		mockTableParameterListFieldIterator = mock(JCoFieldIterator.class, "TableParameterListFieldIterator");
		
		/* Create field mocks for field iterator */
		mockParameterListCharField = mock(JCoField.class, "ParameterListCharField");
		mockParameterListNumField = mock(JCoField.class, "ParameterListNumField");
		mockParameterListIntField = mock(JCoField.class, "ParameterListIntField");
		mockParameterListFloatField = mock(JCoField.class, "ParameterListFloatField");
		mockParameterListBCDField = mock(JCoField.class, "ParameterListBCDField");
		mockParameterListBinaryField = mock(JCoField.class, "ParameterListBinaryField");
		mockParameterListBinaryArrayField = mock(JCoField.class, "ParameterListBinaryArrayField");
		mockParameterListDateField = mock(JCoField.class, "ParameterListDateField");
		mockParameterListTimeField = mock(JCoField.class, "ParameterListTimeField");
		mockParameterListStringField = mock(JCoField.class, "ParameterListStringField");
		mockParameterListStructureField = mock(JCoField.class, "ParameterListStructureField");
		mockParameterListTableField = mock(JCoField.class, "ParameterListTableField");
		
		/* Create mock for structure */
		mockStructure = mock(JCoStructure.class, "Structure");
		
		/* Create mock field iterators for structure and table */
		mockStructureFieldIterator = mock(JCoFieldIterator.class, "StructureFieldIterator");
		mockTableFieldIterator = mock(JCoFieldIterator.class, "TableFieldIterator");

		/* Create field mocks for field iterator */
		mockCharField = mock(JCoField.class, "CharField");
		mockNumField = mock(JCoField.class, "NumField");
		mockIntField = mock(JCoField.class, "IntField");
		mockFloatField = mock(JCoField.class, "FloatField");
		mockBCDField = mock(JCoField.class, "BCDField");
		mockBinaryField = mock(JCoField.class, "BinaryField");
		mockBinaryArrayField = mock(JCoField.class, "BinaryArrayField");
		mockDateField = mock(JCoField.class, "DateField");
		mockTimeField = mock(JCoField.class, "TimeField");
		mockStringField = mock(JCoField.class, "StringField");
		
		/* Create mock for table */
		JCoTable mockTable = mock(JCoTable.class, "Table");
		
		/* Create mocks for parameter list meta data */
		mockImportParameterListMetaData = mock(JCoListMetaData.class, "ImportParameterListMetaData");
		mockChangingParameterListMetaData = mock(JCoListMetaData.class, "ChangingParameterListMetaData");
		mockTableParameterListMetaData = mock(JCoListMetaData.class, "TableParameterListMetaData");
		mockExportParameterListMetaData = mock(JCoListMetaData.class, "ExportParameterListMetaData");
		
		/* Create mocks for structure meta data */
		mockStructureMetaData = mock(JCoRecordMetaData.class, "StructureMetaData");
		
		/* Enhance destination mock */
		when(mockDestination.getRepository()).thenReturn(mockRepository);

		/* Enhance repository mock */
		when(mockRepository.getFunction(FUNCTION_MODULE_NAME)).thenReturn(mockFunction);
		when(mockRepository.getFunctionTemplate(FUNCTION_MODULE_NAME)).thenReturn(mockFunctionTemplate);
		when(mockRepository.getName()).thenReturn(REPOSITORY_NAME);
		
		/* Enhance function mock */
		when(mockFunction.getImportParameterList()).thenReturn(mockImportParameterList);
		when(mockFunction.getChangingParameterList()).thenReturn(mockChangingParameterList);
		when(mockFunction.getExportParameterList()).thenReturn(mockExportParameterList);
		when(mockFunction.getTableParameterList()).thenReturn(mockTableParameterList);
		
		/* Enhance function template mock */
		when(mockFunctionTemplate.getImportParameterList()).thenReturn(mockImportParameterListMetaData);
		when(mockFunctionTemplate.getChangingParameterList()).thenReturn(mockChangingParameterListMetaData);
		when(mockFunctionTemplate.getExportParameterList()).thenReturn(mockExportParameterListMetaData);
		when(mockFunctionTemplate.getTableParameterList()).thenReturn(mockTableParameterListMetaData);

		/* Enhance request/response mock */
		when(mockRequest.getFieldIterator()).thenReturn(mockParameterListFieldIterator);
		when(mockResponse.getFieldIterator()).thenReturn(mockParameterListFieldIterator);
		
		/* Enhance parameter list mock */
		when(mockImportParameterList.getFieldIterator()).thenReturn(mockEmptyParameterListFieldIterator);
		when(mockChangingParameterList.getFieldIterator()).thenReturn(mockParameterListFieldIterator);
		when(mockExportParameterList.getFieldIterator()).thenReturn(mockEmptyParameterListFieldIterator);
		when(mockTableParameterList.getFieldIterator()).thenReturn(mockTableParameterListFieldIterator);
		
		/* Enhance field iterator mocks  */
		when(mockEmptyParameterListFieldIterator.hasNextField()).thenReturn(false );
		when(mockParameterListFieldIterator.hasNextField()).thenReturn(true, true, true, true, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, true, false );
		when(mockParameterListFieldIterator.nextField()).thenReturn(mockParameterListCharField, mockParameterListNumField, mockParameterListIntField, mockParameterListFloatField, mockParameterListBCDField, mockParameterListBinaryField, mockParameterListBinaryArrayField, mockParameterListDateField, mockParameterListTimeField, mockParameterListStringField, mockParameterListStructureField, mockParameterListCharField, mockParameterListNumField, mockParameterListIntField, mockParameterListFloatField, mockParameterListBCDField, mockParameterListBinaryField, mockParameterListBinaryArrayField, mockParameterListDateField, mockParameterListTimeField, mockParameterListStringField, mockParameterListStructureField).thenThrow(new NoSuchElementException());
		
		when(mockTableParameterListFieldIterator.hasNextField()).thenReturn(true, false, true, false );
		when(mockTableParameterListFieldIterator.nextField()).thenReturn(mockParameterListTableField, mockParameterListTableField).thenThrow(new NoSuchElementException());
		
		when(mockStructureFieldIterator.hasNextField()).thenReturn(true, true, true, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, false );
		when(mockStructureFieldIterator.nextField()).thenReturn(mockCharField, mockNumField, mockIntField, mockFloatField, mockBCDField, mockBinaryField, mockBinaryArrayField, mockDateField, mockTimeField, mockStringField, mockCharField, mockNumField, mockIntField, mockFloatField, mockBCDField, mockBinaryField, mockBinaryArrayField, mockDateField, mockTimeField, mockStringField).thenThrow(new NoSuchElementException());
		
		when(mockTableFieldIterator.hasNextField()).thenReturn(true, true, true, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, false );
		when(mockTableFieldIterator.nextField()).thenReturn(mockCharField, mockNumField, mockIntField, mockFloatField, mockBCDField, mockBinaryField, mockBinaryArrayField, mockDateField, mockTimeField, mockStringField, mockCharField, mockNumField, mockIntField, mockFloatField, mockBCDField, mockBinaryField, mockBinaryArrayField, mockDateField, mockTimeField, mockStringField).thenThrow(new NoSuchElementException());
		
		/* Enhance parameter list field mocks */
		when(mockParameterListCharField.getName()).thenReturn(PARAM_LIST_CHAR_PARAM);
		when(mockParameterListCharField.getValue()).thenReturn(CHAR_PARAM_OUT_VAL);

		when(mockParameterListNumField.getName()).thenReturn(PARAM_LIST_NUM_PARAM);
		when(mockParameterListNumField.getValue()).thenReturn(NUM_PARAM_OUT_VAL);

		when(mockParameterListIntField.getName()).thenReturn(PARAM_LIST_INT_PARAM);
		when(mockParameterListIntField.getValue()).thenReturn(INT_PARAM_OUT_VAL);

		when(mockParameterListFloatField.getName()).thenReturn(PARAM_LIST_FLOAT_PARAM);
		when(mockParameterListFloatField.getValue()).thenReturn(FLOAT_PARAM_OUT_VAL);

		when(mockParameterListBCDField.getName()).thenReturn(PARAM_LIST_BCD_PARAM);
		when(mockParameterListBCDField.getValue()).thenReturn(BCD_PARAM_OUT_VAL);

		when(mockParameterListBinaryField.getName()).thenReturn(PARAM_LIST_BINARY_PARAM);
		when(mockParameterListBinaryField.getValue()).thenReturn(BINARY_PARAM_OUT_VAL);

		when(mockParameterListBinaryArrayField.getName()).thenReturn(PARAM_LIST_BINARY_ARRAY_PARAM);
		when(mockParameterListBinaryArrayField.getValue()).thenReturn(BINARY_ARRAY_PARAM_OUT_VAL);

		when(mockParameterListDateField.getName()).thenReturn(PARAM_LIST_DATE_PARAM);
		when(mockParameterListDateField.getValue()).thenReturn(DATE_PARAM_OUT_VAL);

		when(mockParameterListTimeField.getName()).thenReturn(PARAM_LIST_TIME_PARAM);
		when(mockParameterListTimeField.getValue()).thenReturn(TIME_PARAM_OUT_VAL);

		when(mockParameterListStringField.getName()).thenReturn(PARAM_LIST_STRING_PARAM);
		when(mockParameterListStringField.getValue()).thenReturn(STRING_PARAM_OUT_VAL);

		when(mockParameterListStructureField.getName()).thenReturn(PARAM_LIST_STRUCTURE_PARAM);
		when(mockParameterListStructureField.isStructure()).thenReturn(true);
		when(mockParameterListStructureField.getStructure()).thenReturn(mockStructure);

		when(mockParameterListTableField.getName()).thenReturn(PARAM_LIST_TABLE_PARAM);
		when(mockParameterListTableField.isTable()).thenReturn(true);
		when(mockParameterListTableField.getTable()).thenReturn(mockTable);
		
		/* Enhance structure field mocks */
		when(mockCharField.getName()).thenReturn(CHAR_PARAM);
		when(mockCharField.getValue()).thenReturn(CHAR_PARAM_OUT_VAL);

		when(mockNumField.getName()).thenReturn(NUM_PARAM);
		when(mockNumField.getValue()).thenReturn(NUM_PARAM_OUT_VAL);

		when(mockIntField.getName()).thenReturn(INT_PARAM);
		when(mockIntField.getValue()).thenReturn(INT_PARAM_OUT_VAL);

		when(mockFloatField.getName()).thenReturn(FLOAT_PARAM);
		when(mockFloatField.getValue()).thenReturn(FLOAT_PARAM_OUT_VAL);

		when(mockBCDField.getName()).thenReturn(BCD_PARAM);
		when(mockBCDField.getValue()).thenReturn(BCD_PARAM_OUT_VAL);

		when(mockBinaryField.getName()).thenReturn(BINARY_PARAM);
		when(mockBinaryField.getValue()).thenReturn(BINARY_PARAM_OUT_VAL);

		when(mockBinaryArrayField.getName()).thenReturn(BINARY_ARRAY_PARAM);
		when(mockBinaryArrayField.getValue()).thenReturn(BINARY_ARRAY_PARAM_OUT_VAL);

		when(mockDateField.getName()).thenReturn(DATE_PARAM);
		when(mockDateField.getValue()).thenReturn(DATE_PARAM_OUT_VAL);

		when(mockTimeField.getName()).thenReturn(TIME_PARAM);
		when(mockTimeField.getValue()).thenReturn(TIME_PARAM_OUT_VAL);

		when(mockStringField.getName()).thenReturn(STRING_PARAM);
		when(mockStringField.getValue()).thenReturn(STRING_PARAM_OUT_VAL);
		
		/* Enhance structure mock */
		when(mockStructure.getFieldIterator()).thenReturn(mockStructureFieldIterator);
		
		/* Enhance table mock */
		when(mockTable.getNumRows()).thenReturn(1);
		when(mockTable.getFieldIterator()).thenReturn(mockTableFieldIterator);

		/* Enhance structure meta data mock */
		when(mockStructureMetaData.getName()).thenReturn(STRUCTURE_TYPE_NAME);
		
	}

	protected void enhanceParameterListMetaData() {
		
		/* Enhance Parameter List Meta Data mock */
		when(mockChangingParameterListMetaData.getName(0)).thenReturn(PARAM_LIST_CHAR_PARAM);
		when(mockChangingParameterListMetaData.getType(0)).thenReturn(JCoMetaData.TYPE_CHAR);
		when(mockChangingParameterListMetaData.getClassNameOfField(0)).thenReturn(String.class.getName());
		when(mockChangingParameterListMetaData.isOptional(0)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(0)).thenReturn("CHAR");
		when(mockChangingParameterListMetaData.getRecordTypeName(0)).thenReturn("ZCHAR");
		when(mockChangingParameterListMetaData.getLength(0)).thenReturn(10);
		when(mockChangingParameterListMetaData.getByteLength(0)).thenReturn(10);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(0)).thenReturn(20);
		when(mockChangingParameterListMetaData.getDecimals(0)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(0)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(0)).thenReturn(PARAM_LIST_CHAR_PARAM);
		
		when(mockChangingParameterListMetaData.getName(1)).thenReturn(PARAM_LIST_NUM_PARAM);
		when(mockChangingParameterListMetaData.getType(1)).thenReturn(JCoMetaData.TYPE_NUM);
		when(mockChangingParameterListMetaData.getClassNameOfField(1)).thenReturn(String.class.getName());
		when(mockChangingParameterListMetaData.isOptional(1)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(1)).thenReturn("NUM");
		when(mockChangingParameterListMetaData.getRecordTypeName(1)).thenReturn("ZNUM");
		when(mockChangingParameterListMetaData.getLength(1)).thenReturn(10);
		when(mockChangingParameterListMetaData.getByteLength(1)).thenReturn(10);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(1)).thenReturn(20);
		when(mockChangingParameterListMetaData.getDecimals(1)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(1)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(1)).thenReturn(PARAM_LIST_NUM_PARAM);
		
		when(mockChangingParameterListMetaData.getName(2)).thenReturn(PARAM_LIST_INT_PARAM);
		when(mockChangingParameterListMetaData.getType(2)).thenReturn(JCoMetaData.TYPE_INT);
		when(mockChangingParameterListMetaData.getClassNameOfField(2)).thenReturn(Integer.class.getName());
		when(mockChangingParameterListMetaData.isOptional(2)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(2)).thenReturn("INT");
		when(mockChangingParameterListMetaData.getRecordTypeName(2)).thenReturn("ZINT");
		when(mockChangingParameterListMetaData.getLength(2)).thenReturn(4);
		when(mockChangingParameterListMetaData.getByteLength(2)).thenReturn(4);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(2)).thenReturn(4);
		when(mockChangingParameterListMetaData.getDecimals(2)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(2)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(2)).thenReturn(PARAM_LIST_INT_PARAM);
		
		when(mockChangingParameterListMetaData.getName(3)).thenReturn(PARAM_LIST_FLOAT_PARAM);
		when(mockChangingParameterListMetaData.getType(3)).thenReturn(JCoMetaData.TYPE_FLOAT);
		when(mockChangingParameterListMetaData.getClassNameOfField(3)).thenReturn(Double.class.getName());
		when(mockChangingParameterListMetaData.isOptional(3)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(3)).thenReturn("FLOAT");
		when(mockChangingParameterListMetaData.getRecordTypeName(3)).thenReturn("F");
		when(mockChangingParameterListMetaData.getLength(3)).thenReturn(8);
		when(mockChangingParameterListMetaData.getByteLength(3)).thenReturn(8);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(3)).thenReturn(8);
		when(mockChangingParameterListMetaData.getDecimals(3)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(3)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(3)).thenReturn(PARAM_LIST_FLOAT_PARAM);
		
		when(mockChangingParameterListMetaData.getName(4)).thenReturn(PARAM_LIST_BCD_PARAM);
		when(mockChangingParameterListMetaData.getType(4)).thenReturn(JCoMetaData.TYPE_BCD);
		when(mockChangingParameterListMetaData.getClassNameOfField(4)).thenReturn(BigDecimal.class.getName());
		when(mockChangingParameterListMetaData.isOptional(4)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(4)).thenReturn("BCD");
		when(mockChangingParameterListMetaData.getRecordTypeName(4)).thenReturn("ZBCD");
		when(mockChangingParameterListMetaData.getLength(4)).thenReturn(9);
		when(mockChangingParameterListMetaData.getByteLength(4)).thenReturn(9);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(4)).thenReturn(9);
		when(mockChangingParameterListMetaData.getDecimals(4)).thenReturn(14);
		when(mockChangingParameterListMetaData.isChanging(4)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(4)).thenReturn(PARAM_LIST_BCD_PARAM);
		
		when(mockChangingParameterListMetaData.getName(5)).thenReturn(PARAM_LIST_BINARY_PARAM);
		when(mockChangingParameterListMetaData.getType(5)).thenReturn(JCoMetaData.TYPE_BYTE);
		when(mockChangingParameterListMetaData.getClassNameOfField(5)).thenReturn(byte[].class.getName());
		when(mockChangingParameterListMetaData.isOptional(5)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(5)).thenReturn("BYTE");
		when(mockChangingParameterListMetaData.getRecordTypeName(5)).thenReturn("ZBYTE");
		when(mockChangingParameterListMetaData.getLength(5)).thenReturn(1);
		when(mockChangingParameterListMetaData.getByteLength(5)).thenReturn(1);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(5)).thenReturn(1);
		when(mockChangingParameterListMetaData.getDecimals(5)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(5)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(5)).thenReturn(PARAM_LIST_BINARY_PARAM);
		
		when(mockChangingParameterListMetaData.getName(6)).thenReturn(PARAM_LIST_BINARY_ARRAY_PARAM);
		when(mockChangingParameterListMetaData.getType(6)).thenReturn(JCoMetaData.TYPE_BYTE);
		when(mockChangingParameterListMetaData.getClassNameOfField(6)).thenReturn(byte[].class.getName());
		when(mockChangingParameterListMetaData.isOptional(6)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(6)).thenReturn("BYTE");
		when(mockChangingParameterListMetaData.getRecordTypeName(6)).thenReturn("ZBYTE");
		when(mockChangingParameterListMetaData.getLength(6)).thenReturn(10);
		when(mockChangingParameterListMetaData.getByteLength(6)).thenReturn(10);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(6)).thenReturn(10);
		when(mockChangingParameterListMetaData.getDecimals(6)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(6)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(6)).thenReturn(PARAM_LIST_BINARY_ARRAY_PARAM);
		
		when(mockChangingParameterListMetaData.getName(7)).thenReturn(PARAM_LIST_DATE_PARAM);
		when(mockChangingParameterListMetaData.getType(7)).thenReturn(JCoMetaData.TYPE_DATE);
		when(mockChangingParameterListMetaData.getClassNameOfField(7)).thenReturn(Date.class.getName());
		when(mockChangingParameterListMetaData.isOptional(7)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(7)).thenReturn("DATE");
		when(mockChangingParameterListMetaData.getRecordTypeName(7)).thenReturn("D");
		when(mockChangingParameterListMetaData.getLength(7)).thenReturn(8);
		when(mockChangingParameterListMetaData.getByteLength(7)).thenReturn(8);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(7)).thenReturn(16);
		when(mockChangingParameterListMetaData.getDecimals(7)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(7)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(7)).thenReturn(PARAM_LIST_DATE_PARAM);
		
		when(mockChangingParameterListMetaData.getName(8)).thenReturn(PARAM_LIST_TIME_PARAM);
		when(mockChangingParameterListMetaData.getType(8)).thenReturn(JCoMetaData.TYPE_TIME);
		when(mockChangingParameterListMetaData.getClassNameOfField(8)).thenReturn(Date.class.getName());
		when(mockChangingParameterListMetaData.isOptional(8)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(8)).thenReturn("TIME");
		when(mockChangingParameterListMetaData.getRecordTypeName(8)).thenReturn("T");
		when(mockChangingParameterListMetaData.getLength(8)).thenReturn(6);
		when(mockChangingParameterListMetaData.getByteLength(8)).thenReturn(6);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(8)).thenReturn(12);
		when(mockChangingParameterListMetaData.getDecimals(8)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(8)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(8)).thenReturn(PARAM_LIST_TIME_PARAM);
		
		when(mockChangingParameterListMetaData.getName(9)).thenReturn(PARAM_LIST_STRING_PARAM);
		when(mockChangingParameterListMetaData.getType(9)).thenReturn(JCoMetaData.TYPE_STRING);
		when(mockChangingParameterListMetaData.getClassNameOfField(9)).thenReturn(String.class.getName());
		when(mockChangingParameterListMetaData.isOptional(9)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(9)).thenReturn("STRING");
		when(mockChangingParameterListMetaData.getRecordTypeName(9)).thenReturn("STRING");
		when(mockChangingParameterListMetaData.getLength(9)).thenReturn(0);
		when(mockChangingParameterListMetaData.getByteLength(9)).thenReturn(8);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(9)).thenReturn(8);
		when(mockChangingParameterListMetaData.getDecimals(9)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(9)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(9)).thenReturn(PARAM_LIST_STRING_PARAM);
		
		when(mockChangingParameterListMetaData.getName(10)).thenReturn(PARAM_LIST_STRUCTURE_PARAM);
		when(mockChangingParameterListMetaData.isStructure(10)).thenReturn(true);
		when(mockChangingParameterListMetaData.getRecordMetaData(10)).thenReturn(mockStructureMetaData);
		when(mockChangingParameterListMetaData.isOptional(10)).thenReturn(false);
		when(mockChangingParameterListMetaData.getTypeAsString(10)).thenReturn("STRUCTURE");
		when(mockChangingParameterListMetaData.getRecordTypeName(10)).thenReturn(STRUCTURE_TYPE_NAME);
		when(mockChangingParameterListMetaData.getLength(10)).thenReturn(0);
		when(mockChangingParameterListMetaData.getByteLength(10)).thenReturn(80);
		when(mockChangingParameterListMetaData.getUnicodeByteLength(10)).thenReturn(112);
		when(mockChangingParameterListMetaData.getDecimals(10)).thenReturn(0);
		when(mockChangingParameterListMetaData.isChanging(10)).thenReturn(true);
		when(mockChangingParameterListMetaData.getDescription(10)).thenReturn(PARAM_LIST_STRUCTURE_PARAM);
		
		when(mockChangingParameterListMetaData.getFieldCount()).thenReturn(11);

		when(mockTableParameterListMetaData.getName(0)).thenReturn(PARAM_LIST_TABLE_PARAM);
		when(mockTableParameterListMetaData.isTable(0)).thenReturn(true);
		when(mockTableParameterListMetaData.getRecordMetaData(0)).thenReturn(mockStructureMetaData);
		when(mockTableParameterListMetaData.isOptional(0)).thenReturn(false);
		when(mockTableParameterListMetaData.getTypeAsString(0)).thenReturn("TABLE");
		when(mockTableParameterListMetaData.getRecordTypeName(0)).thenReturn(STRUCTURE_TYPE_NAME);
		when(mockTableParameterListMetaData.getLength(0)).thenReturn(0);
		when(mockTableParameterListMetaData.getByteLength(0)).thenReturn(72);
		when(mockTableParameterListMetaData.getUnicodeByteLength(0)).thenReturn(104);
		when(mockTableParameterListMetaData.getDecimals(0)).thenReturn(0);
		when(mockTableParameterListMetaData.isChanging(0)).thenReturn(true);
		when(mockTableParameterListMetaData.getDescription(0)).thenReturn(PARAM_LIST_TABLE_PARAM);
		
		when(mockTableParameterListMetaData.getFieldCount()).thenReturn(1);
		
		/* Enhance Structure Meta Data mock*/
		
		when(mockStructureMetaData.getName(0)).thenReturn(CHAR_PARAM);
		when(mockStructureMetaData.getType(0)).thenReturn(JCoMetaData.TYPE_CHAR);
		when(mockStructureMetaData.getClassNameOfField(0)).thenReturn(String.class.getName());
		when(mockStructureMetaData.getTypeAsString(0)).thenReturn("CHAR");
		when(mockStructureMetaData.getRecordTypeName(0)).thenReturn("ZCHAR");
		when(mockStructureMetaData.getLength(0)).thenReturn(10);
		when(mockStructureMetaData.getByteLength(0)).thenReturn(10);
		when(mockStructureMetaData.getUnicodeByteLength(0)).thenReturn(20);
		when(mockStructureMetaData.getDecimals(0)).thenReturn(0);
		when(mockStructureMetaData.getDescription(0)).thenReturn(CHAR_PARAM);

		when(mockStructureMetaData.getName(1)).thenReturn(NUM_PARAM);
		when(mockStructureMetaData.getType(1)).thenReturn(JCoMetaData.TYPE_NUM);
		when(mockStructureMetaData.getClassNameOfField(1)).thenReturn(String.class.getName());
		when(mockStructureMetaData.getTypeAsString(1)).thenReturn("NUM");
		when(mockStructureMetaData.getRecordTypeName(1)).thenReturn("ZNUM");
		when(mockStructureMetaData.getLength(1)).thenReturn(10);
		when(mockStructureMetaData.getByteLength(1)).thenReturn(10);
		when(mockStructureMetaData.getUnicodeByteLength(1)).thenReturn(20);
		when(mockStructureMetaData.getDecimals(1)).thenReturn(0);
		when(mockStructureMetaData.getDescription(1)).thenReturn(NUM_PARAM);
		
		when(mockStructureMetaData.getName(2)).thenReturn(INT_PARAM);
		when(mockStructureMetaData.getType(2)).thenReturn(JCoMetaData.TYPE_INT);
		when(mockStructureMetaData.getClassNameOfField(2)).thenReturn(Integer.class.getName());
		when(mockStructureMetaData.getTypeAsString(2)).thenReturn("INT");
		when(mockStructureMetaData.getRecordTypeName(2)).thenReturn("ZINT");
		when(mockStructureMetaData.getLength(2)).thenReturn(4);
		when(mockStructureMetaData.getByteLength(2)).thenReturn(4);
		when(mockStructureMetaData.getUnicodeByteLength(2)).thenReturn(4);
		when(mockStructureMetaData.getDecimals(2)).thenReturn(0);
		when(mockStructureMetaData.getDescription(2)).thenReturn(INT_PARAM);
		
		when(mockStructureMetaData.getName(3)).thenReturn(FLOAT_PARAM);
		when(mockStructureMetaData.getType(3)).thenReturn(JCoMetaData.TYPE_FLOAT);
		when(mockStructureMetaData.getClassNameOfField(3)).thenReturn(Double.class.getName());
		when(mockStructureMetaData.getTypeAsString(3)).thenReturn("FLOAT");
		when(mockStructureMetaData.getRecordTypeName(3)).thenReturn("F");
		when(mockStructureMetaData.getLength(3)).thenReturn(8);
		when(mockStructureMetaData.getByteLength(3)).thenReturn(8);
		when(mockStructureMetaData.getUnicodeByteLength(3)).thenReturn(8);
		when(mockStructureMetaData.getDecimals(3)).thenReturn(0);
		when(mockStructureMetaData.getDescription(3)).thenReturn(FLOAT_PARAM);
		
		when(mockStructureMetaData.getName(4)).thenReturn(BCD_PARAM);
		when(mockStructureMetaData.getType(4)).thenReturn(JCoMetaData.TYPE_BCD);
		when(mockStructureMetaData.getClassNameOfField(4)).thenReturn(BigDecimal.class.getName());
		when(mockStructureMetaData.getTypeAsString(4)).thenReturn("BCD");
		when(mockStructureMetaData.getRecordTypeName(4)).thenReturn("ZBCD");
		when(mockStructureMetaData.getLength(4)).thenReturn(9);
		when(mockStructureMetaData.getByteLength(4)).thenReturn(9);
		when(mockStructureMetaData.getUnicodeByteLength(4)).thenReturn(9);
		when(mockStructureMetaData.getDecimals(4)).thenReturn(14);
		when(mockStructureMetaData.getDescription(4)).thenReturn(BCD_PARAM);
		
		when(mockStructureMetaData.getName(5)).thenReturn(BINARY_PARAM);
		when(mockStructureMetaData.getType(5)).thenReturn(JCoMetaData.TYPE_BYTE);
		when(mockStructureMetaData.getClassNameOfField(5)).thenReturn(byte[].class.getName());
		when(mockStructureMetaData.getTypeAsString(5)).thenReturn("BYTE");
		when(mockStructureMetaData.getRecordTypeName(5)).thenReturn("ZBYTE");
		when(mockStructureMetaData.getLength(5)).thenReturn(1);
		when(mockStructureMetaData.getByteLength(5)).thenReturn(1);
		when(mockStructureMetaData.getUnicodeByteLength(5)).thenReturn(1);
		when(mockStructureMetaData.getDecimals(5)).thenReturn(0);
		when(mockStructureMetaData.getDescription(5)).thenReturn(BINARY_PARAM);
		
		when(mockStructureMetaData.getName(6)).thenReturn(BINARY_ARRAY_PARAM);
		when(mockStructureMetaData.getType(6)).thenReturn(JCoMetaData.TYPE_BYTE);
		when(mockStructureMetaData.getClassNameOfField(6)).thenReturn(byte[].class.getName());
		when(mockStructureMetaData.getTypeAsString(6)).thenReturn("BYTE");
		when(mockStructureMetaData.getRecordTypeName(6)).thenReturn("ZBYTE");
		when(mockStructureMetaData.getLength(6)).thenReturn(10);
		when(mockStructureMetaData.getByteLength(6)).thenReturn(10);
		when(mockStructureMetaData.getUnicodeByteLength(6)).thenReturn(10);
		when(mockStructureMetaData.getDecimals(6)).thenReturn(0);
		when(mockStructureMetaData.getDescription(6)).thenReturn(BINARY_ARRAY_PARAM);
		
		when(mockStructureMetaData.getName(7)).thenReturn(DATE_PARAM);
		when(mockStructureMetaData.getType(7)).thenReturn(JCoMetaData.TYPE_DATE);
		when(mockStructureMetaData.getClassNameOfField(7)).thenReturn(Date.class.getName());
		when(mockStructureMetaData.getTypeAsString(7)).thenReturn("DATE");
		when(mockStructureMetaData.getRecordTypeName(7)).thenReturn("D");
		when(mockStructureMetaData.getLength(7)).thenReturn(8);
		when(mockStructureMetaData.getByteLength(7)).thenReturn(8);
		when(mockStructureMetaData.getUnicodeByteLength(7)).thenReturn(16);
		when(mockStructureMetaData.getDecimals(7)).thenReturn(0);
		when(mockStructureMetaData.getDescription(7)).thenReturn(DATE_PARAM);
		
		when(mockStructureMetaData.getName(8)).thenReturn(TIME_PARAM);
		when(mockStructureMetaData.getType(8)).thenReturn(JCoMetaData.TYPE_TIME);
		when(mockStructureMetaData.getClassNameOfField(8)).thenReturn(Date.class.getName());
		when(mockStructureMetaData.getTypeAsString(8)).thenReturn("TIME");
		when(mockStructureMetaData.getRecordTypeName(8)).thenReturn("T");
		when(mockStructureMetaData.getLength(8)).thenReturn(6);
		when(mockStructureMetaData.getByteLength(8)).thenReturn(6);
		when(mockStructureMetaData.getUnicodeByteLength(8)).thenReturn(12);
		when(mockStructureMetaData.getDecimals(8)).thenReturn(0);
		when(mockStructureMetaData.getDescription(8)).thenReturn(TIME_PARAM);
		
		when(mockStructureMetaData.getName(9)).thenReturn(STRING_PARAM);
		when(mockStructureMetaData.getType(9)).thenReturn(JCoMetaData.TYPE_STRING);
		when(mockStructureMetaData.getClassNameOfField(9)).thenReturn(String.class.getName());
		when(mockStructureMetaData.getTypeAsString(9)).thenReturn("STRING");
		when(mockStructureMetaData.getRecordTypeName(9)).thenReturn("STRING");
		when(mockStructureMetaData.getLength(9)).thenReturn(0);
		when(mockStructureMetaData.getByteLength(9)).thenReturn(8);
		when(mockStructureMetaData.getUnicodeByteLength(9)).thenReturn(8);
		when(mockStructureMetaData.getDecimals(9)).thenReturn(0);
		when(mockStructureMetaData.getDescription(9)).thenReturn(STRING_PARAM);
		
		when(mockStructureMetaData.getFieldCount()).thenReturn(10);
		
	}

	protected Structure createAndPopulateRequest() throws Exception {

		Structure request = RfcUtil.getRequest(mockRepository, FUNCTION_MODULE_NAME);
		
		request.put(PARAM_LIST_CHAR_PARAM, CHAR_PARAM_IN_VAL);
		request.put(PARAM_LIST_NUM_PARAM, NUM_PARAM_IN_VAL);
		request.put(PARAM_LIST_INT_PARAM, INT_PARAM_IN_VAL);
		request.put(PARAM_LIST_FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		request.put(PARAM_LIST_BCD_PARAM, BCD_PARAM_IN_VAL);
		request.put(PARAM_LIST_BINARY_PARAM, BINARY_PARAM_IN_VAL);
		request.put(PARAM_LIST_BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		request.put(PARAM_LIST_DATE_PARAM, DATE_PARAM_IN_VAL);
		request.put(PARAM_LIST_TIME_PARAM, TIME_PARAM_IN_VAL);
		request.put(PARAM_LIST_STRING_PARAM, STRING_PARAM_IN_VAL);
		
		Structure structure = request.get(PARAM_LIST_STRUCTURE_PARAM, Structure.class);
		structure.put(CHAR_PARAM, CHAR_PARAM_IN_VAL);
		structure.put(NUM_PARAM, NUM_PARAM_IN_VAL);
		structure.put(INT_PARAM, INT_PARAM_IN_VAL);
		structure.put(FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		structure.put(BCD_PARAM, BCD_PARAM_IN_VAL);
		structure.put(BINARY_PARAM, BINARY_PARAM_IN_VAL);
		structure.put(BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		structure.put(DATE_PARAM, DATE_PARAM_IN_VAL);
		structure.put(TIME_PARAM, TIME_PARAM_IN_VAL);
		structure.put(STRING_PARAM, STRING_PARAM_IN_VAL);
		
		@SuppressWarnings("unchecked")
		Table<Structure> table = request.get(PARAM_LIST_TABLE_PARAM, Table.class);
		Structure row = table.add();
		row.put(CHAR_PARAM, CHAR_PARAM_IN_VAL);
		row.put(NUM_PARAM, NUM_PARAM_IN_VAL);
		row.put(INT_PARAM, INT_PARAM_IN_VAL);
		row.put(FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		row.put(BCD_PARAM, BCD_PARAM_IN_VAL);
		row.put(BINARY_PARAM, BINARY_PARAM_IN_VAL);
		row.put(BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		row.put(DATE_PARAM, DATE_PARAM_IN_VAL);
		row.put(TIME_PARAM, TIME_PARAM_IN_VAL);
		row.put(STRING_PARAM, STRING_PARAM_IN_VAL);

		return request;
	}
	
	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new StaticApplicationContext();
	}
}
