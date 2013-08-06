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

import java.util.List;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Record Meta Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getFieldMetaData <em>Field Meta Data</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getName <em>Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getRecordFieldMetaData <em>Record Field Meta Data</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRecordMetaData()
 * @model
 * @generated
 */
public interface RecordMetaData extends EObject {
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRecordMetaData_FieldMetaData()
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRecordMetaData_Name()
	 * @model required="true"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Record Field Meta Data</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Record Field Meta Data</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Record Field Meta Data</em>' attribute.
	 * @see #setRecordFieldMetaData(List)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRecordMetaData_RecordFieldMetaData()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.FieldList" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	List<FieldMetaData> getRecordFieldMetaData();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getRecordFieldMetaData <em>Record Field Meta Data</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Record Field Meta Data</em>' attribute.
	 * @see #getRecordFieldMetaData()
	 * @generated
	 */
	void setRecordFieldMetaData(List<FieldMetaData> value);

} // RecordMetaData
