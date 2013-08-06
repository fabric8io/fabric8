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
 * A representation of the model object '<em><b>Field Meta Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getFieldMetaData <em>Field Meta Data</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getName <em>Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getType <em>Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteLength <em>Byte Length</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteOffset <em>Byte Offset</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteLength <em>Unicode Byte Length</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteOffset <em>Unicode Byte Offset</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDecimals <em>Decimals</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDescription <em>Description</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getRecordMetaData <em>Record Meta Data</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData()
 * @model
 * @generated
 */
public interface FieldMetaData extends EObject {
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_FieldMetaData()
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getName <em>Name</em>}' attribute.
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_Type()
	 * @model required="true"
	 * @generated
	 */
	DataType getType();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getType <em>Type</em>}' attribute.
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_ByteLength()
	 * @model
	 * @generated
	 */
	int getByteLength();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteLength <em>Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Byte Length</em>' attribute.
	 * @see #getByteLength()
	 * @generated
	 */
	void setByteLength(int value);

	/**
	 * Returns the value of the '<em><b>Byte Offset</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Byte Offset</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Byte Offset</em>' attribute.
	 * @see #setByteOffset(int)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_ByteOffset()
	 * @model
	 * @generated
	 */
	int getByteOffset();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteOffset <em>Byte Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Byte Offset</em>' attribute.
	 * @see #getByteOffset()
	 * @generated
	 */
	void setByteOffset(int value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_UnicodeByteLength()
	 * @model
	 * @generated
	 */
	int getUnicodeByteLength();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteLength <em>Unicode Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Unicode Byte Length</em>' attribute.
	 * @see #getUnicodeByteLength()
	 * @generated
	 */
	void setUnicodeByteLength(int value);

	/**
	 * Returns the value of the '<em><b>Unicode Byte Offset</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Unicode Byte Offset</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Unicode Byte Offset</em>' attribute.
	 * @see #setUnicodeByteOffset(int)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_UnicodeByteOffset()
	 * @model
	 * @generated
	 */
	int getUnicodeByteOffset();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteOffset <em>Unicode Byte Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Unicode Byte Offset</em>' attribute.
	 * @see #getUnicodeByteOffset()
	 * @generated
	 */
	void setUnicodeByteOffset(int value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_Decimals()
	 * @model
	 * @generated
	 */
	int getDecimals();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDecimals <em>Decimals</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Decimals</em>' attribute.
	 * @see #getDecimals()
	 * @generated
	 */
	void setDecimals(int value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFieldMetaData_RecordMetaData()
	 * @model
	 * @generated
	 */
	RecordMetaData getRecordMetaData();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getRecordMetaData <em>Record Meta Data</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Record Meta Data</em>' reference.
	 * @see #getRecordMetaData()
	 * @generated
	 */
	void setRecordMetaData(RecordMetaData value);

} // FieldMetaData
