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
package org.fusesource.camel.component.sap.util;

import static org.fusesource.camel.component.sap.model.rfc.RfcPackage.eNS_URI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.fusesource.camel.component.sap.model.rfc.AbapException;
import org.fusesource.camel.component.sap.model.rfc.DataType;
import org.fusesource.camel.component.sap.model.rfc.Destination;
import org.fusesource.camel.component.sap.model.rfc.FieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.FunctionTemplate;
import org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RFC;
import org.fusesource.camel.component.sap.model.rfc.RecordMetaData;
import org.fusesource.camel.component.sap.model.rfc.RepositoryData;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.xml.sax.InputSource;

import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoCustomRepository;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFieldIterator;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoRecord;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoRequest;
import com.sap.conn.jco.JCoTable;

public class RfcUtil {

	public static final String ROW = "row";

	public static final String GenNS_URI = "http://www.eclipse.org/emf/2002/GenModel";

	public static final String GenNS_DOCUMENTATION_KEY = "documentation";

	/**
	 * Details key for a parameter list entry annotation providing the
	 * description of the underlying JCo data field represented by the annotated
	 * parameter list entry. The details value is the description of the
	 * parameter list entry and its underlying JCo data field.
	 */
	public static final String RfcNS_DESCRIPTION_KEY = "description";

	/**
	 * Details key for a parameter list entry annotation providing the
	 * fully-qualified Java classname of the annotated parameter list entry. The
	 * details value is the fully-qualified Java classname of the parameter list
	 * entry.
	 */
	public static final String RfcNS_CLASS_NAME_OF_FIELD_KEY = "classNameOfField";

	/**
	 * Details key for a parameter list entry annotation indicating the table,
	 * structure or data element name of the underlying JCo data field
	 * represented by the annotated parameter list entry. The details value is
	 * the name of the table, structure or data element of the underlying JCo
	 * data field. The details values is <code>null</code> if the data element
	 * name is unavailable.
	 */
	public static final String RfcNS_RECORD_TYPE_NAME_KEY = "recordTypeName";

	/**
	 * Details key for a parameter list entry annotation providing the JCo data
	 * type of the underlying data field represented by the annotated parameter
	 * list entry. The details value is the JCo data type of the underlying data
	 * field represented by the parameter list entry.
	 */
	public static final String RfcNS_TYPE_KEY = "type";

	/**
	 * Details key for a parameter list entry annotation providing the String
	 * representation of the JCo data type of the underlying data field
	 * represented by the annotated parameter list entry. The details value is
	 * the String representation of the JCo data type of the underlying data
	 * field represented by the parameter list entry.
	 */
	public static final String RfcNS_TYPE_AS_STRING_KEY = "typeAsString";

	/**
	 * Details key for a record entry annotation providing the length of the
	 * underlying data field for Unicode layout represented by the annotated
	 * record entry. The details value is the length of the underlying data
	 * field for Unicode layout represented by the record entry.
	 */
	public static final String RfcNS_UNICODE_BYTE_LENGTH_KEY = "unicodeByteLength";

	/**
	 * Details key for a record entry annotation providing the offset of the
	 * underlying data field in a Unicode layout represented by the annotated
	 * record entry. The details value is the byte offset of the underlying data
	 * field in a Non-Unicode layout represented by the record entry.
	 */
	public static final String RfcNS_UNICODE_BYTE_OFFSET_KEY = "unicodeByteOffset";

	/**
	 * Details key for a record entry annotation providing the total length of
	 * an underlying JCo structure or single row in an underlying JCo table with
	 * a Unicode layout. The details value is the total length of of the
	 * underlying JCo structure or single row in an underlying JCo table with a
	 * Unicode layout. Note due to alignment constraints the length of a JCo
	 * structure or table row is not necessarily the same as the sum of the
	 * field lenth.
	 */
	public static final String RfcNS_UNICODE_RECORD_LENGTH_KEY = "unicodeRecordLength";

	/**
	 * Details key for a parameter list entry annotation providing the byte
	 * length of the underlying data field for Non-Unicode layout represented by
	 * the annotated record entry. The details value is the byte length of the
	 * underlying data field for Non-Unicode layout represented by the parameter
	 * list entry.
	 */
	public static final String RfcNS_BYTE_LENGTH_KEY = "byteLength";

	/**
	 * Details key for a record entry annotation providing the offset of the
	 * underlying data field in a Non-Unicode layout represented by the
	 * annotated parameter list entry. The details value is the byte offset of
	 * the underlying data field in a Non-Unicode layout represented by the
	 * record entry.
	 */
	public static final String RfcNS_BYTE_OFFSET_KEY = "byteOffset";

	/**
	 * Details key for a parameter list entry annotation providing the max
	 * length of the underlying data field represented by the annotated
	 * parameter list entry. The details value is the max length of the
	 * underlying data field represented by the parameter list entry.
	 * 
	 * <ul>
	 * <li>
	 * <p>
	 * For character based data element types the length is the char length.
	 * <li>
	 * <p>
	 * For <em><b>STRING</b></em> and <em><b>XSTRING</b></em> data element types
	 * the length is <em><b>0</b></em>.</li>
	 * <li>
	 * <p>
	 * For <em><b>STRUCTURE</b></em> and <em><b>TABLE</b></em> data element
	 * types the length is <em><b>0</b></em>.</li>
	 * <li>
	 * <p>
	 * For numerical based data element types the length is the byte length.
	 * </ul>
	 */
	public static final String RfcNS_LENGTH_KEY = "length";

	/**
	 * Details key for a parameter list entry annotation providing the number of
	 * decimals in the the underlying data field represented by the annotated
	 * parameter list entry. The details value is the number of decimals in the
	 * the underlying data field represented by the parameter list entry.
	 * 
	 * <p>
	 * The details value is possibly non-zero only for record entries whose
	 * underlying data field has the JCo data type <em><b>BCD</b></em> or
	 * <em><b>FLOAT</b></em>.
	 */
	public static final String RfcNS_DECIMALS_KEY = "decimals";

	/**
	 * Details key for a parameter list entry annotation providing the default
	 * value of the annotated parameter list entry. The details value is the
	 * default value of the parameter list entry.
	 */
	public static final String RfcNS_DEFAULT_KEY = "default";

	/**
	 * Details key for a parameter list entry annotation providing the field
	 * name in an underlying JCo structure or table represented by the annotated
	 * parameter list entry. The details value is the name of the field in the
	 * underlying JCo structure or table if the JCo parameter represented by the
	 * entry is defined by referencing that field. The details values is
	 * <code>null</code> otherwise.
	 */
	public static final String RfcNS_RECORD_FIELD_NAME_KEY = "recordFieldName";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * annotated parameter list entry represents an underlying JCo
	 * <code>ABAP Object</code>. The details value is <code>true</code> if the
	 * parameter list entry represents an <code>ABAP Object</code>;
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_ABAP_OBJECT_KEY = "isAbapObject";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * annotated parameter list entry represents an underlying
	 * <code>TYPE1</code> JCo structure. The details value is <code>true</code>
	 * if the parameter list entry represents a <code>TYPE1</code> structure;
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_NESTED_TYPE1_STRUCTURE_KEY = "isNestedType1Structure";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * annotated parameter list entry is a <em><b>structure</b></em> type entry;
	 * i.e. is a {@link MappedRecord}. The details value is <code>true</code> if
	 * the parameter list entry is a {@link MappedRecord}; <code>false</code>
	 * otherwise.
	 */
	public static final String RfcNS_IS_STRUCTURE_KEY = "isStructure";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * annotated parameter list entry is a <em><b>table</b></em> type entry;
	 * i.e. is an {@link IndexedRecord}. The details value is <code>true</code>
	 * if the parameter list entry is a {@link IndexedRecord};
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_TABLE_KEY = "isTable";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * underlying data field represented by the annotated parameter list entry
	 * is an <em><b>import</b></em> parameter. The details value is
	 * <code>true</code> if the underlying data field is an import parameter;
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_IMPORT_KEY = "isImport";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * underlying data field represented by the annotated parameter list entry
	 * is an <em><b>export</b></em> parameter. The details value is
	 * <code>true</code> if the underlying data field is an export parameter;
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_EXPORT_KEY = "isExport";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * underlying data field represented by the annotated parameter list entry
	 * is a <em><b>changing</b></em> parameter. The details value is
	 * <code>true</code> if the underlying data field is a changing parameter;
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_CHANGING_KEY = "isChanging";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * underlying data field represented by the annotated parameter list entry
	 * is an <em><b>exception</b></em>. The details value is <code>true</code>
	 * if the underlying data field is an exception; <code>false</code>
	 * otherwise.
	 */
	public static final String RfcNS_IS_EXCEPTION_KEY = "isException";

	/**
	 * Details key for a parameter list entry annotation indicating whether the
	 * underlying data field represented by the annotated parameter list entry
	 * is an <em><b>optional</b></em> parameter. The details value is
	 * <code>true</code> if the underlying data field is an optional parameter;
	 * <code>false</code> otherwise.
	 */
	public static final String RfcNS_IS_OPTIONAL_KEY = "isOptional";

	private static final String STEXT_PARAM = "STEXT";
	private static final String FUNCTIONS_TABLE = "FUNCTIONS";
	private static final String FUNCNAME_PARAM = "FUNCNAME";
	private static final String RFC_FUNCTION_SEARCH_FUNCTION = "RFC_FUNCTION_SEARCH";
	private static final String GROUPNAME_PARAM = "GROUPNAME";

	private RfcUtil() {
	}

	public static Destination getDestination(String destinationName) {
		try {
			JCoDestination jcoDestination = JCoDestinationManager.getDestination(destinationName);
			Destination destination = RfcFactory.eINSTANCE.createDestination();
			String repositoryName = jcoDestination.getRepository().getName();
			destination.setName(destinationName);
			destination.setRepositoryName(repositoryName);
			return destination;
		} catch (JCoException e) {
			return null;
		}
	}

	public static List<RFC> getRFCs(JCoDestination jcoDestination, String functionNameFilter, String groupNameFilter) {
		List<RFC> rfcs = new ArrayList<RFC>();
		try {
			JCoFunction jcoFunction = jcoDestination.getRepository().getFunction(RFC_FUNCTION_SEARCH_FUNCTION);
			jcoFunction.getImportParameterList().setValue(FUNCNAME_PARAM, functionNameFilter);
			jcoFunction.getImportParameterList().setValue(GROUPNAME_PARAM, groupNameFilter);
			jcoFunction.execute(jcoDestination);
			JCoTable sapFunctions = jcoFunction.getTableParameterList().getTable(FUNCTIONS_TABLE);

			if (sapFunctions.getNumRows() > 0) {
				sapFunctions.firstRow();
				do {
					RFC rfc = RfcFactory.eINSTANCE.createRFC();
					String functionName = sapFunctions.getString(FUNCNAME_PARAM);
					String groupName = sapFunctions.getString(GROUPNAME_PARAM);
					rfc.setName(functionName);
					rfc.setGroup(groupName);
					String functionDescription = sapFunctions.getString(STEXT_PARAM);
					rfc.setDescription(functionDescription);
					rfcs.add(rfc);
				} while (sapFunctions.nextRow());
			}
		} catch (JCoException e) {
			// Assume No Function Found
		}
		return rfcs;
	}

	public static Structure executeFunction(JCoDestination destination, String functionName, Structure request) throws JCoException {
		JCoFunction jcoFunction = destination.getRepository().getFunction(functionName);
		fillJCoParameterListsFromRequest(request, jcoFunction);

		jcoFunction.execute(destination);

		Structure response = getResponse(destination.getRepository(), functionName);
		extractJCoParameterListsIntoResponse(jcoFunction, response);

		return response;
	}

	public static void executeFunction(JCoDestination destination, String functionName, Structure request, String tid) throws JCoException {

		JCoFunction jcoFunction = destination.getRepository().getFunction(functionName);
		fillJCoParameterListsFromRequest(request, jcoFunction);

		jcoFunction.execute(destination, tid);
	}

	public static void executeFunction(JCoDestination destination, String functionName, Structure request, String tid, String queueName) throws JCoException {
		JCoFunction jcoFunction = destination.getRepository().getFunction(functionName);
		fillJCoParameterListsFromRequest(request, jcoFunction);

		jcoFunction.execute(destination, tid, queueName);
	}

	public static void fillJCoParameterListsFromRequest(Structure request, JCoFunction jcoFunction) {
		fillJCoRecordFromStructure(request, jcoFunction.getImportParameterList());
		fillJCoRecordFromStructure(request, jcoFunction.getChangingParameterList());
		fillJCoRecordFromStructure(request, jcoFunction.getTableParameterList());
	}

	public static void fillJCoParameterListsFromResponse(Structure response, JCoFunction jcoFunction) {
		fillJCoRecordFromStructure(response, jcoFunction.getChangingParameterList());
		fillJCoRecordFromStructure(response, jcoFunction.getTableParameterList());
		fillJCoRecordFromStructure(response, jcoFunction.getExportParameterList());
	}

	public static void extractJCoParameterListsIntoRequest(JCoFunction jcoFunction, Structure request) {
		extractJCoRecordIntoStructure(jcoFunction.getImportParameterList(), request);
		extractJCoRecordIntoStructure(jcoFunction.getChangingParameterList(), request);
		extractJCoRecordIntoStructure(jcoFunction.getTableParameterList(), request);
	}

	public static void extractJCoParameterListsIntoResponse(JCoFunction jcoFunction, Structure response) {
		extractJCoRecordIntoStructure(jcoFunction.getChangingParameterList(), response);
		extractJCoRecordIntoStructure(jcoFunction.getTableParameterList(), response);
		extractJCoRecordIntoStructure(jcoFunction.getExportParameterList(), response);
	}

	public static void beginTransaction(JCoDestination jcoDestination) {
		JCoContext.begin(jcoDestination);
	}

	public static void commitTransaction(JCoDestination jcoDestination) throws JCoException {
		try {
			JCoRequest request = jcoDestination.getRepository().getRequest("BAPI_TRANSACTION_COMMIT");
			request.setValue("WAIT", "X");
			request.execute(jcoDestination);
		} finally {
			JCoContext.end(jcoDestination);
		}
	}

	public static void rollbackTransaction(JCoDestination jcoDestination) throws JCoException {
		try {
			JCoRequest request = jcoDestination.getRepository().getRequest("BAPI_TRANSACTION_ROLLBACK");
			request.execute(jcoDestination);
		} finally {
			JCoContext.end(jcoDestination);
		}
	}

	public static Object getValue(EObject object, String featureName) {
		EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
		if (feature == null)
			return null;
		return getValue(object, feature);
	}

	public static Object getValue(EObject object, EStructuralFeature feature) {
		try {
			Object value = object.eGet(feature);
			if (value == null && feature instanceof EReference) {
				EClass eClass = ((EReference) feature).getEReferenceType();
				value = eClass.getEPackage().getEFactoryInstance().create(eClass);
				setValue(object, feature, value);
			}
			return value;
		} catch (Throwable exception) {
			return null;
		}
	}

	public static boolean setValue(EObject object, String featureName, Object value) {
		EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
		if (feature == null)
			return false;
		return setValue(object, feature, value);
	}

	public static boolean setValue(EObject object, EStructuralFeature feature, Object value) {
		try {
			EditingDomain editingDomain = AdapterFactoryEditingDomain.getEditingDomainFor(object);
			if (editingDomain == null) {
				object.eSet(feature, value);
			} else {
				Command setCommand = SetCommand.create(editingDomain, object, feature, value);
				editingDomain.getCommandStack().execute(setCommand);
			}
			return true;
		} catch (Throwable exception) {
			return false;
		}
	}

	public static Structure addTableRow(Table<? extends Structure> table) {
		EStructuralFeature feature = table.eClass().getEStructuralFeature(ROW);
		if (feature == null || !(feature instanceof EReference)) {
			return null;
		}
		EClass rowType = ((EReference) feature).getEReferenceType();
		@SuppressWarnings("unchecked")
		EList<Structure> records = (EList<Structure>) getValue(table, feature);

		Structure newRow = (Structure) rowType.getEPackage().getEFactoryInstance().create(rowType);
		records.add(newRow);
		return newRow;
	}

	public static Structure addTableRow(Table<? extends Structure> table, int index) {
		EStructuralFeature feature = table.eClass().getEStructuralFeature(ROW);
		if (feature == null || !(feature instanceof EReference)) {
			return null;
		}
		EClass rowType = ((EReference) feature).getEReferenceType();
		@SuppressWarnings("unchecked")
		EList<Structure> records = (EList<Structure>) getValue(table, feature);

		Structure newRow = (Structure) rowType.getEPackage().getEFactoryInstance().create(rowType);
		records.add(index, newRow);
		return newRow;
	}

	@SuppressWarnings("unchecked")
	public static void extractJCoRecordIntoStructure(JCoRecord jrecord, Structure eObject) {
		if (jrecord == null || eObject == null)
			return;

		EClass eClass = eObject.eClass();
		JCoFieldIterator iterator = jrecord.getFieldIterator();
		while (iterator.hasNextField()) {
			JCoField field = iterator.nextField();
			EStructuralFeature feature = eClass.getEStructuralFeature(field.getName());
			Object value = getValue(eObject, feature);
			if (field.isStructure()) {
				if (value == null || !(value instanceof EObject))
					continue;
				extractJCoRecordIntoStructure(field.getStructure(), (Structure) value);
			} else if (field.isTable()) {
				if (value == null || !(value instanceof EObject))
					continue;
				extractJCoTableIntoTable((JCoTable) field.getTable(), (Table<? extends Structure>) value);
			} else {
				setValue(eObject, feature, field.getValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void fillJCoRecordFromStructure(Structure eObject, JCoRecord jcoRecord) {
		if (jcoRecord == null || eObject == null)
			return;

		EClass eClass = eObject.eClass();
		JCoFieldIterator iterator = jcoRecord.getFieldIterator();
		while (iterator.hasNextField()) {
			JCoField field = iterator.nextField();
			EStructuralFeature feature = eClass.getEStructuralFeature(field.getName());
			Object value = getValue(eObject, feature);
			if (field.isStructure()) {
				if (value == null || !(value instanceof Structure))
					continue;
				fillJCoRecordFromStructure((Structure) value, field.getStructure());
			} else if (field.isTable()) {
				if (value == null || !(value instanceof Table))
					continue;
				fillJCoTableFromTable((Table<? extends Structure>) value, field.getTable());
			} else {
				field.setValue(value);
			}
		}

	}

	public static void extractJCoTableIntoTable(JCoTable jcoTable, Table<? extends Structure> table) {
		if (table == null || jcoTable == null)
			return;

		EStructuralFeature feature = table.eClass().getEStructuralFeature(ROW);
		if (feature == null || !(feature instanceof EReference)) {
			return;
		}
		EClass rowType = ((EReference) feature).getEReferenceType();
		@SuppressWarnings("unchecked")
		EList<Structure> records = (EList<Structure>) getValue(table, feature);

		jcoTable.firstRow();
		for (int i = 0; i < jcoTable.getNumRows(); i++, jcoTable.nextRow()) {
			Structure newRow = (Structure) rowType.getEPackage().getEFactoryInstance().create(rowType);
			records.add(newRow);
			extractJCoRecordIntoStructure(jcoTable, newRow);
		}
	}

	public static void fillJCoTableFromTable(Table<? extends Structure> table, JCoTable jcoTable) {
		if (table == null || jcoTable == null)
			return;

		EStructuralFeature feature = table.eClass().getEStructuralFeature(ROW);
		@SuppressWarnings("unchecked")
		EList<Structure> records = (EList<Structure>) getValue(table, feature);
		for (Structure row : records) {
			jcoTable.appendRow();
			fillJCoRecordFromStructure(row, (JCoRecord) jcoTable);
		}
	}

	public static Structure getRequest(JCoRepository repository, String functionModuleName) {
		return (Structure) getInstance(repository, functionModuleName, "Request");
	}

	public static Structure getResponse(JCoRepository repository, String functionModuleName) {
		return (Structure) getInstance(repository, functionModuleName, "Response");
	}

	/**
	 * Returns an {@link EObject} instance defined by the {@link EClass} with
	 * the name <code>eClassName</code> in the {@link EPackage} associated with
	 * the <code>functionModuleName</code> described in the
	 * <code>repository</code>.
	 * 
	 * @param repository
	 *            - the {@link JCoRepository} describing
	 * @param functionModuleName
	 * @param eClassName
	 * @return
	 */
	public static EObject getInstance(JCoRepository repository, String functionModuleName, String eClassName) {
		String nsURI = eNS_URI + "/" + repository.getName() + "/" + functionModuleName;

		EPackage ePackage = getEPackage(repository, nsURI);
		EClassifier classifier = ePackage.getEClassifier(eClassName);
		if (!(classifier instanceof EClass))
			return null;

		EClass eClass = (EClass) classifier;
		EObject eObject = ePackage.getEFactoryInstance().create(eClass);

		return eObject;
	}

	/**
	 * Returns (and creates if necessary) the {@link EPackage} instance
	 * containing the definition of the input and output {@link Structure}s
	 * passed to the {@link JcoFunction} designated by <code>nsURi</code> and
	 * described in the given <code>repository</code>.
	 * 
	 * @param repository
	 *            - the {@link JCoRepository} containing the meta data *
	 *            describing the designated {@link JCoFunction}.
	 * @param nsURI
	 *            - the URI designating {@link JCoFunction}. The URI format is
	 *            of the form:
	 *            http://sap.fusesource.org/rfc/{repository-name}/{jco
	 *            -function-name}.
	 * @return The {@link EPackage} instance.
	 */
	public static EPackage getEPackage(JCoRepository repository, String nsURI) {

		// Check whether the requested package has already been built.
		EPackage ePackage = (EPackage) EPackage.Registry.INSTANCE.get(nsURI);
		if (ePackage != null) {
			return ePackage;
		}

		// Check whether the requested package is defined by the destination's
		// repository.
		if (nsURI.startsWith(eNS_URI + "/" + repository.getName())) {

			// Extract the function module name from the URI.
			int prefixLength = eNS_URI.length() + repository.getName().length() + 2; // Length
																						// of
																						// "http://sap.fusesource.org/<repo-name>/"
																						// prefix.
			String functionModuleName = nsURI.substring(prefixLength);

			// Retrieve the function module's meta-data.
			JCoFunctionTemplate functionTemplate;
			try {
				functionTemplate = repository.getFunctionTemplate(functionModuleName);
			} catch (JCoException e) {
				return null;
			}
			JCoListMetaData importParameterListMetaData = functionTemplate.getImportParameterList();
			JCoListMetaData changingParameterListMetaData = functionTemplate.getChangingParameterList();
			JCoListMetaData tableParameterListMetaData = functionTemplate.getTableParameterList();
			JCoListMetaData exportParameterListMetaData = functionTemplate.getExportParameterList();

			// Create and initialize package
			EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
			ePackage = ecoreFactory.createEPackage();
			ePackage.setName(functionModuleName);
			ePackage.setNsPrefix(functionModuleName);
			ePackage.setNsURI(nsURI);

			// Create Request Class
			EClass requestClass = ecoreFactory.createEClass();
			ePackage.getEClassifiers().add(requestClass);
			requestClass.setName("Request");
			requestClass.getESuperTypes().add(RfcPackage.eINSTANCE.getStructure());
			addListMetaData(requestClass, importParameterListMetaData);
			addListMetaData(requestClass, changingParameterListMetaData);
			addListMetaData(requestClass, tableParameterListMetaData);
			addAnnotation(requestClass, GenNS_URI, GenNS_DOCUMENTATION_KEY, "Request for " + functionModuleName);

			// Create Response Class
			EClass responseClass = ecoreFactory.createEClass();
			ePackage.getEClassifiers().add(responseClass);
			responseClass.setName("Response");
			responseClass.getESuperTypes().add(RfcPackage.eINSTANCE.getStructure());
			addListMetaData(responseClass, exportParameterListMetaData);
			addListMetaData(responseClass, changingParameterListMetaData);
			addListMetaData(responseClass, tableParameterListMetaData);
			addAnnotation(responseClass, GenNS_URI, GenNS_DOCUMENTATION_KEY, "Response for " + functionModuleName);

			// Register Package
			EPackage.Registry.INSTANCE.put(nsURI, ePackage);
		}
		return ePackage;
	}

	/**
	 * Adds detail entry to designated annotation of given model element.
	 * 
	 * @param modelElement
	 *            - the model element to be annotated.
	 * @param source
	 *            - the source URL of annotation to be added to.
	 * @param key
	 *            - the key of the detail entry to be added to annotation.
	 * @param value
	 *            - the value of the detail entry to added to annotation.
	 */
	public static void addAnnotation(EModelElement modelElement, String source, String key, String value) {
		EAnnotation annotation = modelElement.getEAnnotation(source);
		if (annotation == null) {
			annotation = EcoreFactory.eINSTANCE.createEAnnotation();
			annotation.setSource(source);
			annotation.setEModelElement(modelElement);
		}
		annotation.getDetails().put(key, value);
	}

	/**
	 * Populate the given {@link EClass} with structural features and
	 * annotations derived from the meta-data of the given
	 * {@link JCoListMetaData}.
	 * 
	 * @param eClass
	 *            - the {@link EClass} populated with meta-data.
	 * @param jcoListMetaData
	 *            - the {@link JCoListMetaData} from which the meta-data is
	 *            derived.
	 */
	public static void addListMetaData(EClass eClass, JCoListMetaData jcoListMetaData) {
		if (jcoListMetaData == null)
			return;

		EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
		EPackage ePackage = eClass.getEPackage();
		for (int i = 0; i < jcoListMetaData.getFieldCount(); i++) {
			EStructuralFeature structuralFeature;
			if (jcoListMetaData.isStructure(i)) {
				JCoRecordMetaData jcoRecordMetaData = jcoListMetaData.getRecordMetaData(i);
				EClass structureClass = getStructureClass(ePackage, jcoRecordMetaData);
				EReference reference = ecoreFactory.createEReference();
				reference.setEType(structureClass);
				reference.setContainment(true);
				structuralFeature = reference;
				addAnnotation(structuralFeature, eNS_URI, RfcNS_CLASS_NAME_OF_FIELD_KEY, EObject.class.getName());
			} else if (jcoListMetaData.isTable(i)) {
				JCoRecordMetaData jcoRecordMetaData = jcoListMetaData.getRecordMetaData(i);
				EClass tableClass = getTableClass(ePackage, jcoRecordMetaData);
				EReference reference = ecoreFactory.createEReference();
				reference.setEType(tableClass);
				reference.setContainment(true);
				structuralFeature = reference;
				addAnnotation(structuralFeature, eNS_URI, RfcNS_CLASS_NAME_OF_FIELD_KEY, EObject.class.getName());
			} else {
				EAttribute attribute = ecoreFactory.createEAttribute();
				attribute.setEType(getEDataType(jcoListMetaData.getType(i)));
				structuralFeature = attribute;
				addAnnotation(structuralFeature, eNS_URI, RfcNS_CLASS_NAME_OF_FIELD_KEY, jcoListMetaData.getClassNameOfField(i));
			}
			structuralFeature.setName(jcoListMetaData.getName(i));
			if (!jcoListMetaData.isOptional(i))
				structuralFeature.setLowerBound(1);
			if (jcoListMetaData.getDefault(i) != null)
				structuralFeature.setDefaultValueLiteral(jcoListMetaData.getDefault(i));
			addAnnotation(structuralFeature, GenNS_URI, GenNS_DOCUMENTATION_KEY, jcoListMetaData.getDescription(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_DESCRIPTION_KEY, jcoListMetaData.getDescription(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_TYPE_KEY, Integer.toString(jcoListMetaData.getType(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_TYPE_AS_STRING_KEY, jcoListMetaData.getTypeAsString(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_RECORD_TYPE_NAME_KEY, jcoListMetaData.getRecordTypeName(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_LENGTH_KEY, Integer.toString(jcoListMetaData.getLength(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_BYTE_LENGTH_KEY, Integer.toString(jcoListMetaData.getByteLength(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_UNICODE_BYTE_LENGTH_KEY, Integer.toString(jcoListMetaData.getUnicodeByteLength(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_DECIMALS_KEY, Integer.toString(jcoListMetaData.getDecimals(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_DEFAULT_KEY, jcoListMetaData.getDefault(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_RECORD_FIELD_NAME_KEY, jcoListMetaData.getRecordFieldName(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_ABAP_OBJECT_KEY, Boolean.toString(jcoListMetaData.isAbapObject(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_NESTED_TYPE1_STRUCTURE_KEY, Boolean.toString(jcoListMetaData.isNestedType1Structure(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_STRUCTURE_KEY, Boolean.toString(jcoListMetaData.isStructure(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_TABLE_KEY, Boolean.toString(jcoListMetaData.isTable(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_IMPORT_KEY, Boolean.toString(jcoListMetaData.isImport(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_EXCEPTION_KEY, Boolean.toString(jcoListMetaData.isException(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_EXPORT_KEY, Boolean.toString(jcoListMetaData.isExport(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_CHANGING_KEY, Boolean.toString(jcoListMetaData.isChanging(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_OPTIONAL_KEY, Boolean.toString(jcoListMetaData.isOptional(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_RECORD_FIELD_NAME_KEY, jcoListMetaData.getRecordFieldName(i));
			eClass.getEStructuralFeatures().add(structuralFeature);
		}
	}

	/**
	 * Create and return an {@link EClass} in <code>ePackage</code> deriving
	 * from {@link Structure} and representing a {@link JCoRecord} described by
	 * <code>jcoRecordMetaData</code>.
	 * 
	 * @param ePackage
	 *            - the {@link EPackage} instance containing the {@link EClass}
	 *            definition.
	 * @param jcoRecordMetaData
	 *            - the {@link JCoRecordMetaData} instance describing an
	 *            underlying {@link JCoRecord} instance.
	 * @return The {@link EClass} instance created.
	 */
	public static EClass getStructureClass(EPackage ePackage, JCoRecordMetaData jcoRecordMetaData) {

		// Check package to see if structure class has already been defined.
		EClassifier structureClass = ePackage.getEClassifier(jcoRecordMetaData.getName());

		// Build structure class if not already built.
		if (!(structureClass instanceof EClass)) {

			structureClass = EcoreFactory.eINSTANCE.createEClass();
			ePackage.getEClassifiers().add(structureClass);
			structureClass.setName(jcoRecordMetaData.getName());
			addRecordMetaData(((EClass) structureClass), jcoRecordMetaData);
			((EClass) structureClass).getESuperTypes().add(RfcPackage.eINSTANCE.getStructure());
		}
		return (EClass) structureClass;
	}

	/**
	 * Create and return an {@link EClass} in <code>ePackage</code> deriving
	 * from {@link Table<? extends Structure>} and representing a
	 * {@link JCoTable} described by <code>jcoRecordMetaData</code>.
	 * 
	 * @param ePackage
	 *            - the {@link EPackage} instance containing the {@link EClass}
	 *            definition.
	 * @param jcoRecordMetaData
	 *            - the {@link JCoRecordMetaData} instance describing an
	 *            underlying {@link JCoTable} instance.
	 * @return The {@link EClass} instance created.
	 */
	public static EClass getTableClass(EPackage ePackage, JCoRecordMetaData jcoRecordMetaData) {

		// Check package to see if table class has already been defined.
		EClassifier tableClass = ePackage.getEClassifier(jcoRecordMetaData.getName() + "_TABLE");

		// Build table class if not already built.
		if (!(tableClass instanceof EClass)) {

			// Create the super type inherited by this Table subclass: i.e.
			// 'Table<S extends Structure>'
			EGenericType tableGenericSuperType = EcoreFactory.eINSTANCE.createEGenericType();
			EClass tableSuperClass = RfcPackage.eINSTANCE.getTable();
			tableGenericSuperType.setEClassifier(tableSuperClass);

			// Create type parameter for row type: i.e. the 'S' in 'S extends
			// Structure'
			EGenericType rowGenericType = EcoreFactory.eINSTANCE.createEGenericType();
			EClass structureType = getStructureClass(ePackage, jcoRecordMetaData);
			rowGenericType.setEClassifier(structureType);

			// Add the type parameter to super type: i.e. 'S'
			tableGenericSuperType.getETypeArguments().add(rowGenericType);

			// Create the Table subclass and add to package
			tableClass = EcoreFactory.eINSTANCE.createEClass();
			ePackage.getEClassifiers().add(tableClass);
			tableClass.setName(jcoRecordMetaData.getName() + "_TABLE");
			((EClass) tableClass).getEGenericSuperTypes().add(tableGenericSuperType);

			// Workaround for type erasure in EMF Generic feature.
			EReference rowReference = EcoreFactory.eINSTANCE.createEReference();
			rowReference.setEType(structureType);
			rowReference.setName(ROW);
			rowReference.setContainment(true);
			rowReference.setLowerBound(0);
			rowReference.setUpperBound(-1);
			((EClass) tableClass).getEStructuralFeatures().add(rowReference);

		}
		return (EClass) tableClass;
	}

	/**
	 * Populate the given {@link EClass} with structural features and
	 * annotations derived from the meta-data of the given
	 * {@link JCoRecordMetaData}.
	 * 
	 * @param eClass
	 *            - the {@link EClass} populated with meta-data.
	 * @param jcoRecordMetaData
	 *            - the {@link JCoRecordMetaData} from which the meta-data is
	 *            derived.
	 */
	public static void addRecordMetaData(EClass eClass, JCoRecordMetaData jcoRecordMetaData) {
		EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
		EPackage ePackage = eClass.getEPackage();
		for (int i = 0; i < jcoRecordMetaData.getFieldCount(); i++) {
			EStructuralFeature structuralFeature;
			if (jcoRecordMetaData.isStructure(i)) {
				JCoRecordMetaData jcoSubRecordMetaData = jcoRecordMetaData.getRecordMetaData(i);
				EClass structureClass = getStructureClass(ePackage, jcoSubRecordMetaData);
				EReference reference = ecoreFactory.createEReference();
				structuralFeature = reference;
				reference.setEType(structureClass);
				reference.setContainment(true);
				addAnnotation(structuralFeature, eNS_URI, RfcNS_CLASS_NAME_OF_FIELD_KEY, EObject.class.getName());
			} else if (jcoRecordMetaData.isTable(i)) {
				JCoRecordMetaData jcoSubRecordMetaData = jcoRecordMetaData.getRecordMetaData(i);
				EClass tableClass = getTableClass(ePackage, jcoSubRecordMetaData);
				EReference reference = ecoreFactory.createEReference();
				structuralFeature = reference;
				reference.setEType(tableClass);
				reference.setContainment(true);
				addAnnotation(structuralFeature, eNS_URI, RfcNS_CLASS_NAME_OF_FIELD_KEY, EObject.class.getName());
			} else {
				EAttribute attribute = ecoreFactory.createEAttribute();
				structuralFeature = attribute;
				attribute.setEType(getEDataType(jcoRecordMetaData.getType(i)));
				addAnnotation(structuralFeature, eNS_URI, RfcNS_CLASS_NAME_OF_FIELD_KEY, jcoRecordMetaData.getClassNameOfField(i));
			}
			structuralFeature.setName(jcoRecordMetaData.getName(i));
			addAnnotation(structuralFeature, GenNS_URI, GenNS_DOCUMENTATION_KEY, jcoRecordMetaData.getDescription(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_DESCRIPTION_KEY, jcoRecordMetaData.getDescription(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_TYPE_KEY, Integer.toString(jcoRecordMetaData.getType(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_TYPE_AS_STRING_KEY, jcoRecordMetaData.getTypeAsString(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_RECORD_TYPE_NAME_KEY, jcoRecordMetaData.getRecordTypeName(i));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_LENGTH_KEY, Integer.toString(jcoRecordMetaData.getLength(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_BYTE_LENGTH_KEY, Integer.toString(jcoRecordMetaData.getByteLength(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_BYTE_OFFSET_KEY, Integer.toString(jcoRecordMetaData.getByteOffset(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_UNICODE_BYTE_LENGTH_KEY, Integer.toString(jcoRecordMetaData.getUnicodeByteLength(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_UNICODE_BYTE_OFFSET_KEY, Integer.toString(jcoRecordMetaData.getUnicodeByteOffset(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_DECIMALS_KEY, Integer.toString(jcoRecordMetaData.getDecimals(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_ABAP_OBJECT_KEY, Boolean.toString(jcoRecordMetaData.isAbapObject(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_NESTED_TYPE1_STRUCTURE_KEY, Boolean.toString(jcoRecordMetaData.isNestedType1Structure(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_STRUCTURE_KEY, Boolean.toString(jcoRecordMetaData.isStructure(i)));
			addAnnotation(structuralFeature, eNS_URI, RfcNS_IS_TABLE_KEY, Boolean.toString(jcoRecordMetaData.isTable(i)));
			eClass.getEStructuralFeatures().add(structuralFeature);

		}
	}

	/**
	 * Return the {@link EClassifier} corresponding to the given JCo data type.
	 * 
	 * @param jcoDataType
	 *            - the JCo data type.
	 * @return the {@link EClassifier} corresponding to the given JCo data type.
	 */
	public static EClassifier getEDataType(int jcoDataType) {
		switch (jcoDataType) {

		case JCoMetaData.TYPE_INT:
		case JCoMetaData.TYPE_INT1:
		case JCoMetaData.TYPE_INT2:
			return EcorePackage.Literals.EINT;

		case JCoMetaData.TYPE_CHAR:
			return EcorePackage.Literals.ESTRING;

		case JCoMetaData.TYPE_NUM:
			return EcorePackage.Literals.ESTRING;

		case JCoMetaData.TYPE_BCD:
			return EcorePackage.Literals.EBIG_DECIMAL;

		case JCoMetaData.TYPE_DATE:
			return EcorePackage.Literals.EDATE;

		case JCoMetaData.TYPE_TIME:
			return EcorePackage.Literals.EDATE;

		case JCoMetaData.TYPE_FLOAT:
			return EcorePackage.Literals.EDOUBLE;

		case JCoMetaData.TYPE_BYTE:
			return EcorePackage.Literals.EBYTE_ARRAY;

		case JCoMetaData.TYPE_STRING:
			return EcorePackage.Literals.ESTRING;

		case JCoMetaData.TYPE_XSTRING:
			return EcorePackage.Literals.EBYTE_ARRAY;

		case JCoMetaData.TYPE_DECF16:
			return EcorePackage.Literals.EBIG_DECIMAL;

		case JCoMetaData.TYPE_DECF34:
			return EcorePackage.Literals.EBIG_DECIMAL;

		case JCoMetaData.TYPE_STRUCTURE:
			return EcorePackage.Literals.EOBJECT;

		case JCoMetaData.TYPE_TABLE:
			return EcorePackage.Literals.EOBJECT;

		default:
			return EcorePackage.Literals.EBYTE_ARRAY;
		}
	}
	
	public static void print(EObject eObject) throws IOException {
		XMLResource resource = new XMLResourceImpl();
		resource.getContents().add(eObject);
		resource.save(System.out, null);
	}
	
	public static OutputStream toOutputStream(EObject eObject) throws IOException {
		XMLResource resource = new XMLResourceImpl();
		eObject = EcoreUtil.copy(eObject);
		resource.getContents().add(eObject);
		OutputStream out = new ByteArrayOutputStream();
		resource.save(out, null);
		return out;
	}
	
	public static InputStream toInputStream(EObject eObject) throws IOException {
		String string = marshal(eObject);
		ByteArrayInputStream in = new ByteArrayInputStream(string.getBytes());
		return in;
	}
	
	public static EObject fromInputStream(InputStream in) throws IOException {
		XMLResource resource = new XMLResourceImpl();
		resource.load(in, null);
		return resource.getContents().get(0);
	}

	/**
	 * Marshals the given {@link EObject} into a string.
	 * 
	 * @param eObject
	 *            - the {@link EObject} to be marshalled.
	 * @return The marshaled content of {@link EObject}.
	 * @throws IOException
	 */
	public static String marshal(EObject eObject) throws IOException {
		XMLResource resource = new XMLResourceImpl();
		eObject = EcoreUtil.copy(eObject);
		resource.getContents().add(eObject);
		StringWriter out = new StringWriter();
		resource.save(out, null);
		return out.toString();
	}

	/**
	 * Unmarshals the given string content into an {@link EObject} instance.
	 * 
	 * @param string
	 *            - the string content to unmarshal.
	 * @return The {@link EObject} instance unmarshalled from the string
	 *         content.
	 * @throws IOException
	 */
	public static EObject unmarshal(String string) throws IOException {
		XMLResource resource = new XMLResourceImpl();
		StringReader in = new StringReader(string);
		resource.load(new InputSource(in), null);
		return resource.getContents().get(0);
	}

	public static JCoCustomRepository createRepository(String repositoryName, RepositoryData repositoryData) {
		JCoCustomRepository customRepository = JCo.createCustomRepository(repositoryName);
		for (String functionTemplateName: repositoryData.getEntries().keySet()) {
			FunctionTemplate functionTemplate = repositoryData.getEntries().get(functionTemplateName);
			JCoFunctionTemplate jcoFunctionTemplate = createJCoFunctionTemplate(functionTemplateName, functionTemplate);
			customRepository.addFunctionTemplateToCache(jcoFunctionTemplate);
		}
		return customRepository;
	}
	
	public static JCoFunctionTemplate createJCoFunctionTemplate(String name, FunctionTemplate functionTemplate) {
		JCoListMetaData importsMetaData= createJCoListMetaData("IMPORTS", functionTemplate.getImportParameterList());
		JCoListMetaData exportsMetaData = createJCoListMetaData("EXPORTS", functionTemplate.getExportParameterList());
		JCoListMetaData changingMetaData = createJCoListMetaData("CHANGING", functionTemplate.getChangingParameterList());
		JCoListMetaData tablesMetaData = createJCoListMetaData("TABLES", functionTemplate.getTables());
		com.sap.conn.jco.AbapException[] jcoAbapExceptions = createAbapExceptions(functionTemplate.getExceptionList());
		JCoFunctionTemplate jcoFunctionTemplate = JCo.createFunctionTemplate(name, importsMetaData, exportsMetaData, changingMetaData, tablesMetaData, jcoAbapExceptions);
		
		return jcoFunctionTemplate;
	}

	public static JCoListMetaData createJCoListMetaData(String name, List<ListFieldMetaData> listDescriptors) {
		JCoListMetaData jcoListMetaData = JCo.createListMetaData(name);

		for(ListFieldMetaData listFieldDescriptor: listDescriptors) {
			int flags = 0;
			flags |= listFieldDescriptor.isImport() ? JCoListMetaData.IMPORT_PARAMETER : 0;
			flags |= listFieldDescriptor.isExport() ? JCoListMetaData.EXPORT_PARAMETER : 0;
			flags |= listFieldDescriptor.isChanging() ? JCoListMetaData.CHANGING_PARAMETER : 0;
			flags |= listFieldDescriptor.isOptional() ? JCoListMetaData.OPTIONAL_PARAMETER : 0;
			switch(listFieldDescriptor.getType().getValue()) {
			case DataType.STRUCTURE_VALUE:
			case DataType.TABLE_VALUE:
				JCoRecordMetaData jcoRecordMetaData = createJCoRecordMetaData(listFieldDescriptor.getRecordMetaData());
				jcoListMetaData.add(listFieldDescriptor.getName(), listFieldDescriptor.getType().getValue(), jcoRecordMetaData, flags);
				break;
			default: 
				jcoListMetaData.add(listFieldDescriptor.getName(), listFieldDescriptor.getType().getValue(), listFieldDescriptor.getByteLength(), listFieldDescriptor.getUnicodeByteLength(), listFieldDescriptor.getDecimals(), listFieldDescriptor.getDescription(), null, flags, null, null);
			}
		}
		jcoListMetaData.lock();
		return jcoListMetaData;
	}

	public static JCoRecordMetaData createJCoRecordMetaData(RecordMetaData recordMetaData) {
		JCoRecordMetaData jcoRecordMetaData = JCo.createRecordMetaData(recordMetaData.getName());

		for (FieldMetaData fieldDescriptor : recordMetaData.getRecordFieldMetaData()) {
			switch (fieldDescriptor.getType().getValue()) {
			case DataType.STRUCTURE_VALUE:
			case DataType.TABLE_VALUE:
				JCoRecordMetaData recordMetaData2 = createJCoRecordMetaData(fieldDescriptor.getRecordMetaData());
				jcoRecordMetaData.add(fieldDescriptor.getName(), fieldDescriptor.getType().getValue(), fieldDescriptor.getByteOffset(),
						fieldDescriptor.getUnicodeByteOffset(), recordMetaData2);
				break;
			default:
				jcoRecordMetaData.add(fieldDescriptor.getName(), fieldDescriptor.getType().getValue(), fieldDescriptor.getByteLength(),
						fieldDescriptor.getByteOffset(), fieldDescriptor.getUnicodeByteLength(), fieldDescriptor.getUnicodeByteOffset(),
						fieldDescriptor.getDecimals(), fieldDescriptor.getDescription(), null, null);
			}
		}
		jcoRecordMetaData.lock();
		return jcoRecordMetaData;
	}
	
	public static com.sap.conn.jco.AbapException[] createAbapExceptions(List<AbapException> abapExceptions) {
		com.sap.conn.jco.AbapException[] jcoAbapExceptions = new com.sap.conn.jco.AbapException[abapExceptions.size()];
		for (int i = 0; i < abapExceptions.size(); i++) {
			AbapException abapException = abapExceptions.get(i);
			jcoAbapExceptions[i] = new com.sap.conn.jco.AbapException(abapException.getKey(), abapException.getMessage());
		}
		return jcoAbapExceptions;
	}
}
