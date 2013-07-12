package org.fusesource.camel.component.sap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
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

public class SAPComponentTest extends CamelSpringTestSupport {

	/*********************************************************************
	 * Test Function And Record Names
	 *********************************************************************/

	private static final String REPOSITORY_NAME = "TEST_REPOSITORY";
	private static final String FUNCTION_MODULE_NAME = "TEST_FUNCTION_MODULE";
	
	/*********************************************************************
	 * Test Structure Names
	 *********************************************************************/

	private static final String STRUCTURE_TYPE_NAME = "ZSTRUCTURE";
	
	/*********************************************************************
	 * Test Parameter List Names
	 *********************************************************************/
	private static final String PARAM_LIST_CHAR_PARAM = "PARAM_LIST_CHAR_PARAM";
	private static final String PARAM_LIST_NUM_PARAM = "PARAM_LIST_NUM_PARAM";
	private static final String PARAM_LIST_INT_PARAM = "PARAM_LIST_INT_PARAM";
	private static final String PARAM_LIST_FLOAT_PARAM = "PARAM_LIST_FLOAT_PARAM";
	private static final String PARAM_LIST_BCD_PARAM = "PARAM_LIST_BCD_PARAM";
	private static final String PARAM_LIST_BINARY_PARAM = "PARAM_LIST_BINARY_PARAM";
	private static final String PARAM_LIST_BINARY_ARRAY_PARAM = "PARAM_LIST_BINARY_ARRAY_PARAM";
	private static final String PARAM_LIST_DATE_PARAM = "PARAM_LIST_DATE_PARAM";
	private static final String PARAM_LIST_TIME_PARAM = "PARAM_LIST_TIME_PARAM";
	private static final String PARAM_LIST_STRING_PARAM = "PARAM_LIST_STRING_PARAM";
	private static final String PARAM_LIST_STRUCTURE_PARAM = "PARAM_LIST_STRUCTURE_PARAM";
	private static final String PARAM_LIST_TABLE_PARAM = "PARAM_LIST_TABLE_PARAM";

	/*********************************************************************
	 * Test Parameter Names
	 *********************************************************************/
	private static final String CHAR_PARAM = "CHAR_PARAM";
	private static final String NUM_PARAM = "NUM_PARAM";
	private static final String INT_PARAM = "INT_PARAM";
	private static final String FLOAT_PARAM = "FLOAT_PARAM";
	private static final String BCD_PARAM = "BCD_PARAM";
	private static final String BINARY_PARAM = "BINARY_PARAM";
	private static final String BINARY_ARRAY_PARAM = "BINARY_ARRAY_PARAM";
	private static final String DATE_PARAM = "DATE_PARAM";
	private static final String TIME_PARAM = "TIME_PARAM";
	private static final String STRING_PARAM = "STRING_PARAM";

	/*********************************************************************
	 * Test Parameter Input Values
	 *********************************************************************/
	private static final String CHAR_PARAM_IN_VAL = "ABCDEFGHIJ";
	private static final String NUM_PARAM_IN_VAL = "0123456789";
	private static final int INT_PARAM_IN_VAL = 0x75555555;
	private static final double FLOAT_PARAM_IN_VAL = Math.pow(10, 38); // This seems to be the biggest double to not come
																	// back as infinity.
	private static final BigDecimal BCD_PARAM_IN_VAL = new BigDecimal("100.00000000000001");
	private static final byte[] BINARY_PARAM_IN_VAL = new byte[] { (byte) 0x55 };
	private static final byte[] BINARY_ARRAY_PARAM_IN_VAL = new byte[] { (byte) 0xFF, (byte) 0x0F, (byte) 0x1E,
			(byte) 0x2D, (byte) 0x3C, (byte) 0x4B, (byte) 0x5A, (byte) 0x60, (byte) 0x79, (byte) 0x88 };
	private static final Date DATE_PARAM_IN_VAL = new GregorianCalendar(1861, 03, 12).getTime();
	private static final Date TIME_PARAM_IN_VAL = new GregorianCalendar(1970, 0, 1, 12, 15, 30).getTime();
	private static final String STRING_PARAM_IN_VAL = "Four score and seven years ago ...";

	/*********************************************************************
	 * Test Parameter Output Values
	 *********************************************************************/

	private static final String CHAR_PARAM_OUT_VAL = "ZYXWVUTSRQ";
	private static final String NUM_PARAM_OUT_VAL = "9876543210";
	private static final int INT_PARAM_OUT_VAL = 0x7AAAAAAA;
	private static final double FLOAT_PARAM_OUT_VAL = 2 * Math.pow(10, 38);
	private static final BigDecimal BCD_PARAM_OUT_VAL = new BigDecimal("200.00000000000002");
	private static final byte[] BINARY_PARAM_OUT_VAL = new byte[] { (byte) 0xAA };
	private static final byte[] BINARY_ARRAY_PARAM_OUT_VAL = new byte[] { (byte) 0x88, (byte) 0x79, (byte) 0x60,
			(byte) 0x5A, (byte) 0x4B, (byte) 0x3C, (byte) 0x2D, (byte) 0x1E, (byte) 0x0F, (byte) 0xFF };
	private static final Date DATE_PARAM_OUT_VAL = new GregorianCalendar(1865, 03, 9).getTime();
	private static final Date TIME_PARAM_OUT_VAL = new GregorianCalendar(1970, 0, 1, 23, 45, 15).getTime();
	private static final String STRING_PARAM_OUT_VAL = "... shall not perish from this earth.";
	
	/****************************************************************************
	 * Mocks
	 ****************************************************************************/
	
	private JCoRepository mockRepository;
	private JCoFunction mockFunction;
	private JCoFunctionTemplate mockFunctionTemplate;
	private JCoListMetaData mockImportParameterListMetaData;
	private JCoListMetaData mockChangingParameterListMetaData;
	private JCoListMetaData mockTableParameterListMetaData;
	private JCoListMetaData mockExportParameterListMetaData;
	private JCoRecordMetaData mockStructureMetaData;
	private JCoParameterList mockImportParameterList;
	private JCoParameterList mockChangingParameterList;
	private JCoParameterList mockExportParameterList;
	private JCoParameterList mockTableParameterList;
	private JCoFieldIterator mockParameterListFieldIterator;
	private JCoFieldIterator mockTableParameterListFieldIterator;
	private JCoField mockParameterListCharField;
	private JCoField mockParameterListNumField;
	private JCoField mockParameterListIntField;
	private JCoField mockParameterListFloatField;
	private JCoField mockParameterListBCDField;
	private JCoField mockParameterListBinaryField;
	private JCoField mockParameterListBinaryArrayField;
	private JCoField mockParameterListDateField;
	private JCoField mockParameterListTimeField;
	private JCoField mockParameterListStringField;
	private JCoField mockParameterListStructureField;
	private JCoField mockParameterListTableField;
	private JCoFieldIterator mockStructureFieldIterator;
	private JCoField mockCharField;
	private JCoField mockNumField;
	private JCoField mockIntField;
	private JCoField mockFloatField;
	private JCoField mockBCDField;
	private JCoField mockBinaryField;
	private JCoField mockBinaryArrayField;
	private JCoField mockDateField;
	private JCoField mockTimeField;
	private JCoField mockStringField;
	private JCoStructure mockStructure;
	private JCoFieldIterator mockTableFieldIterator;
	private JCoRequest mockRequest;
	private JCoResponse mockResponse;
	private JCoDestination mockDestination;
	private JCoFieldIterator mockEmptyParameterListFieldIterator;
	
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
	
	@Test
	public void testFillFromRequest() throws Exception {
		// Given
		enhanceParameterListMetaData();
		
		// When
		Structure request = RfcUtil.getRequest(mockRepository, FUNCTION_MODULE_NAME);
		
		RfcUtil.setValue(request, PARAM_LIST_CHAR_PARAM, CHAR_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_NUM_PARAM, NUM_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_INT_PARAM, INT_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_BCD_PARAM, BCD_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_BINARY_PARAM, BINARY_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_DATE_PARAM, DATE_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_TIME_PARAM, TIME_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_STRING_PARAM, STRING_PARAM_IN_VAL);
		
		Structure structure = (Structure) RfcUtil.getValue(request, PARAM_LIST_STRUCTURE_PARAM);
		RfcUtil.setValue(structure, CHAR_PARAM, CHAR_PARAM_IN_VAL);
		RfcUtil.setValue(structure, NUM_PARAM, NUM_PARAM_IN_VAL);
		RfcUtil.setValue(structure, INT_PARAM, INT_PARAM_IN_VAL);
		RfcUtil.setValue(structure, FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		RfcUtil.setValue(structure, BCD_PARAM, BCD_PARAM_IN_VAL);
		RfcUtil.setValue(structure, BINARY_PARAM, BINARY_PARAM_IN_VAL);
		RfcUtil.setValue(structure, BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		RfcUtil.setValue(structure, DATE_PARAM, DATE_PARAM_IN_VAL);
		RfcUtil.setValue(structure, TIME_PARAM, TIME_PARAM_IN_VAL);
		RfcUtil.setValue(structure, STRING_PARAM, STRING_PARAM_IN_VAL);
		
		@SuppressWarnings("unchecked")
		Table<? extends Structure> table = (Table<? extends Structure>) RfcUtil.getValue(request, PARAM_LIST_TABLE_PARAM);
		Structure row = RfcUtil.addTableRow(table);
		RfcUtil.setValue(row, CHAR_PARAM, CHAR_PARAM_IN_VAL);
		RfcUtil.setValue(row, NUM_PARAM, NUM_PARAM_IN_VAL);
		RfcUtil.setValue(row, INT_PARAM, INT_PARAM_IN_VAL);
		RfcUtil.setValue(row, FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		RfcUtil.setValue(row, BCD_PARAM, BCD_PARAM_IN_VAL);
		RfcUtil.setValue(row, BINARY_PARAM, BINARY_PARAM_IN_VAL);
		RfcUtil.setValue(row, BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		RfcUtil.setValue(row, DATE_PARAM, DATE_PARAM_IN_VAL);
		RfcUtil.setValue(row, TIME_PARAM, TIME_PARAM_IN_VAL);
		RfcUtil.setValue(row, STRING_PARAM, STRING_PARAM_IN_VAL);

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
		
		assertThat("RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_INT_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) RfcUtil.getValue(response, PARAM_LIST_INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("outputRecord.getP(ARAM_LIST_DATE_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM), is(DATE_PARAM_OUT_VAL));
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
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSynchronousRfc() throws Exception {
		// Given
		enhanceParameterListMetaData();
		
		// When
		Structure request = RfcUtil.getRequest(mockRepository, FUNCTION_MODULE_NAME);
		
		RfcUtil.setValue(request, PARAM_LIST_CHAR_PARAM, CHAR_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_NUM_PARAM, NUM_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_INT_PARAM, INT_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_BCD_PARAM, BCD_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_BINARY_PARAM, BINARY_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_DATE_PARAM, DATE_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_TIME_PARAM, TIME_PARAM_IN_VAL);
		RfcUtil.setValue(request, PARAM_LIST_STRING_PARAM, STRING_PARAM_IN_VAL);
		
		Structure structure = (Structure) RfcUtil.getValue(request, PARAM_LIST_STRUCTURE_PARAM);
		RfcUtil.setValue(structure, CHAR_PARAM, CHAR_PARAM_IN_VAL);
		RfcUtil.setValue(structure, NUM_PARAM, NUM_PARAM_IN_VAL);
		RfcUtil.setValue(structure, INT_PARAM, INT_PARAM_IN_VAL);
		RfcUtil.setValue(structure, FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		RfcUtil.setValue(structure, BCD_PARAM, BCD_PARAM_IN_VAL);
		RfcUtil.setValue(structure, BINARY_PARAM, BINARY_PARAM_IN_VAL);
		RfcUtil.setValue(structure, BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		RfcUtil.setValue(structure, DATE_PARAM, DATE_PARAM_IN_VAL);
		RfcUtil.setValue(structure, TIME_PARAM, TIME_PARAM_IN_VAL);
		RfcUtil.setValue(structure, STRING_PARAM, STRING_PARAM_IN_VAL);
		
		Table<? extends Structure> table = (Table<? extends Structure>) RfcUtil.getValue(request, PARAM_LIST_TABLE_PARAM);
		Structure row = RfcUtil.addTableRow(table);
		RfcUtil.setValue(row, CHAR_PARAM, CHAR_PARAM_IN_VAL);
		RfcUtil.setValue(row, NUM_PARAM, NUM_PARAM_IN_VAL);
		RfcUtil.setValue(row, INT_PARAM, INT_PARAM_IN_VAL);
		RfcUtil.setValue(row, FLOAT_PARAM, FLOAT_PARAM_IN_VAL);
		RfcUtil.setValue(row, BCD_PARAM, BCD_PARAM_IN_VAL);
		RfcUtil.setValue(row, BINARY_PARAM, BINARY_PARAM_IN_VAL);
		RfcUtil.setValue(row, BINARY_ARRAY_PARAM, BINARY_ARRAY_PARAM_IN_VAL);
		RfcUtil.setValue(row, DATE_PARAM, DATE_PARAM_IN_VAL);
		RfcUtil.setValue(row, TIME_PARAM, TIME_PARAM_IN_VAL);
		RfcUtil.setValue(row, STRING_PARAM, STRING_PARAM_IN_VAL);

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
		
		assertThat("RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM) + "' instead of expected value '" + CHAR_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_CHAR_PARAM), is(CHAR_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM) + "' instead of expected value '" + NUM_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_NUM_PARAM), is(NUM_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_INT_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_INT_PARAM) + "' instead of expected value '" + INT_PARAM_OUT_VAL + "'", (Integer) RfcUtil.getValue(response, PARAM_LIST_INT_PARAM), is(INT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM) + "' instead of expected value '" + FLOAT_PARAM_OUT_VAL + "'", (Double) RfcUtil.getValue(response, PARAM_LIST_FLOAT_PARAM), is(FLOAT_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM) + "' instead of expected value '" + BCD_PARAM_OUT_VAL + "'", (BigDecimal) RfcUtil.getValue(response, PARAM_LIST_BCD_PARAM), is(BCD_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM) + "' instead of expected value '" + BINARY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(response, PARAM_LIST_BINARY_PARAM), is(BINARY_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM) + "' instead of expected value '" + BINARY_ARRAY_PARAM_OUT_VAL + "'", (byte[]) RfcUtil.getValue(response, PARAM_LIST_BINARY_ARRAY_PARAM), is(BINARY_ARRAY_PARAM_OUT_VAL));
		assertThat("outputRecord.getP(ARAM_LIST_DATE_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM) + "' instead of expected value '" + DATE_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(response, PARAM_LIST_DATE_PARAM), is(DATE_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_TIME_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_TIME_PARAM) + "' instead of expected value '" + TIME_PARAM_OUT_VAL + "'", (Date) RfcUtil.getValue(response, PARAM_LIST_TIME_PARAM), is(TIME_PARAM_OUT_VAL));
		assertThat("RfcUtil.getValue(response, PARAM_LIST_STRING_PARAM) returned '" +  RfcUtil.getValue(response, PARAM_LIST_STRING_PARAM) + "' instead of expected value '" + STRING_PARAM_OUT_VAL + "'", (String) RfcUtil.getValue(response, PARAM_LIST_STRING_PARAM), is(STRING_PARAM_OUT_VAL));
		
		structure = (Structure) RfcUtil.getValue(response, PARAM_LIST_STRUCTURE_PARAM);
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
		
		table = (Table<? extends Structure>) RfcUtil.getValue(response, PARAM_LIST_TABLE_PARAM);
		assertThat("RfcUtil.getValue(response, PARAM_LIST_TABLE_PARAM) returned unexpected null value", table, notNullValue());
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
	
    //@Test
    public void testSAP() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);    
        
        
        JCoDestination destination = JCoDestinationManager.getDestination("nplDest");
        
        Structure request = RfcUtil.getRequest(destination.getRepository(), "BAPI_FLCUST_GETLIST");
        //RfcUtil.setValue(request, "MAX_ROWS", 1);
        
        Structure response = RfcUtil.getResponse(destination.getRepository(), "BAPI_FLCUST_GETLIST");
        
        template.sendBody("direct:getFlcustList", request);
        
        assertMockEndpointsSatisfied();
        
        response = mock.getExchanges().get(0).getIn().getBody(Structure.class);
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
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("org/fusesource/camel/component/sap/SAPComponentTestConfig.xml");
	}
}
