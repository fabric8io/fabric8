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
package org.fusesource.camel.component.sap.model.rfc;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>List Field Meta Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getFieldMetaData <em>Field Meta Data</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getName <em>Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getType <em>Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getByteLength <em>Byte Length</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getUnicodeByteLength <em>Unicode Byte Length</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDecimals <em>Decimals</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDefaults <em>Defaults</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDescription <em>Description</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isImport <em>Import</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isChanging <em>Changing</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isExport <em>Export</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isException <em>Exception</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isOptional <em>Optional</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getRecordMetaData <em>Record Meta Data</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData()
 * @model
 * @generated
 */
public interface ListFieldMetaData extends EObject {
	/**
	 * Returns the value of the '<em><b>Field Meta Data</b></em>' containment reference list.
	 * The list contents are of type {@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Field Meta Data</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Field Meta Data</em>' containment reference list.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_FieldMetaData()
	 * @model containment="true"
	 * @generated
	 */
	EList<FieldMetaData> getFieldMetaData();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link org.fusesource.camel.component.sap.model.rfc.DataType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Type</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see org.fusesource.camel.component.sap.model.rfc.DataType
	 * @see #setType(DataType)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Type()
	 * @model required="true"
	 * @generated
	 */
	DataType getType();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see org.fusesource.camel.component.sap.model.rfc.DataType
	 * @see #getType()
	 * @generated
	 */
	void setType(DataType value);

	/**
	 * Returns the value of the '<em><b>Byte Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Byte Length</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Byte Length</em>' attribute.
	 * @see #setByteLength(int)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_ByteLength()
	 * @model
	 * @generated
	 */
	int getByteLength();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getByteLength <em>Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Byte Length</em>' attribute.
	 * @see #getByteLength()
	 * @generated
	 */
	void setByteLength(int value);

	/**
	 * Returns the value of the '<em><b>Unicode Byte Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Unicode Byte Length</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Unicode Byte Length</em>' attribute.
	 * @see #setUnicodeByteLength(int)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_UnicodeByteLength()
	 * @model
	 * @generated
	 */
	int getUnicodeByteLength();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getUnicodeByteLength <em>Unicode Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Unicode Byte Length</em>' attribute.
	 * @see #getUnicodeByteLength()
	 * @generated
	 */
	void setUnicodeByteLength(int value);

	/**
	 * Returns the value of the '<em><b>Decimals</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Decimals</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Decimals</em>' attribute.
	 * @see #setDecimals(int)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Decimals()
	 * @model
	 * @generated
	 */
	int getDecimals();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDecimals <em>Decimals</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Decimals</em>' attribute.
	 * @see #getDecimals()
	 * @generated
	 */
	void setDecimals(int value);

	/**
	 * Returns the value of the '<em><b>Defaults</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Defaults</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Defaults</em>' attribute.
	 * @see #setDefaults(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Defaults()
	 * @model
	 * @generated
	 */
	String getDefaults();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDefaults <em>Defaults</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Defaults</em>' attribute.
	 * @see #getDefaults()
	 * @generated
	 */
	void setDefaults(String value);

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Description</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Import</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Import</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Import</em>' attribute.
	 * @see #setImport(boolean)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Import()
	 * @model
	 * @generated
	 */
	boolean isImport();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isImport <em>Import</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Import</em>' attribute.
	 * @see #isImport()
	 * @generated
	 */
	void setImport(boolean value);

	/**
	 * Returns the value of the '<em><b>Changing</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Changing</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Changing</em>' attribute.
	 * @see #setChanging(boolean)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Changing()
	 * @model
	 * @generated
	 */
	boolean isChanging();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isChanging <em>Changing</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Changing</em>' attribute.
	 * @see #isChanging()
	 * @generated
	 */
	void setChanging(boolean value);

	/**
	 * Returns the value of the '<em><b>Export</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Export</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Export</em>' attribute.
	 * @see #setExport(boolean)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Export()
	 * @model
	 * @generated
	 */
	boolean isExport();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isExport <em>Export</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Export</em>' attribute.
	 * @see #isExport()
	 * @generated
	 */
	void setExport(boolean value);

	/**
	 * Returns the value of the '<em><b>Exception</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exception</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exception</em>' attribute.
	 * @see #setException(boolean)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Exception()
	 * @model
	 * @generated
	 */
	boolean isException();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isException <em>Exception</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exception</em>' attribute.
	 * @see #isException()
	 * @generated
	 */
	void setException(boolean value);

	/**
	 * Returns the value of the '<em><b>Optional</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Optional</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Optional</em>' attribute.
	 * @see #setOptional(boolean)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_Optional()
	 * @model
	 * @generated
	 */
	boolean isOptional();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isOptional <em>Optional</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Optional</em>' attribute.
	 * @see #isOptional()
	 * @generated
	 */
	void setOptional(boolean value);

	/**
	 * Returns the value of the '<em><b>Record Meta Data</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Record Meta Data</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Record Meta Data</em>' reference.
	 * @see #setRecordMetaData(RecordMetaData)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getListFieldMetaData_RecordMetaData()
	 * @model
	 * @generated
	 */
	RecordMetaData getRecordMetaData();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getRecordMetaData <em>Record Meta Data</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Record Meta Data</em>' reference.
	 * @see #getRecordMetaData()
	 * @generated
	 */
	void setRecordMetaData(RecordMetaData value);

} // ListFieldMetaData
