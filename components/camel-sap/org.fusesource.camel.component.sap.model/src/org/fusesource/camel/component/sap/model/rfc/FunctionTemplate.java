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
 * A representation of the model object '<em><b>Function Template</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImports <em>Imports</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExports <em>Exports</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChanging <em>Changing</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTables <em>Tables</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptions <em>Exceptions</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImportParameterList <em>Import Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExportParameterList <em>Export Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChangingParameterList <em>Changing Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTableParameterList <em>Table Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptionList <em>Exception List</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate()
 * @model
 * @generated
 */
public interface FunctionTemplate extends EObject {
	/**
	 * Returns the value of the '<em><b>Imports</b></em>' reference list.
	 * The list contents are of type {@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Imports</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Imports</em>' reference list.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_Imports()
	 * @model
	 * @generated
	 */
	EList<ListFieldMetaData> getImports();

	/**
	 * Returns the value of the '<em><b>Exports</b></em>' reference list.
	 * The list contents are of type {@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exports</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exports</em>' reference list.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_Exports()
	 * @model
	 * @generated
	 */
	EList<ListFieldMetaData> getExports();

	/**
	 * Returns the value of the '<em><b>Changing</b></em>' reference list.
	 * The list contents are of type {@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Changing</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Changing</em>' reference list.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_Changing()
	 * @model
	 * @generated
	 */
	EList<ListFieldMetaData> getChanging();

	/**
	 * Returns the value of the '<em><b>Tables</b></em>' reference list.
	 * The list contents are of type {@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Tables</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tables</em>' reference list.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_Tables()
	 * @model
	 * @generated
	 */
	EList<ListFieldMetaData> getTables();

	/**
	 * Returns the value of the '<em><b>Exceptions</b></em>' reference list.
	 * The list contents are of type {@link org.fusesource.camel.component.sap.model.rfc.AbapException}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exceptions</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exceptions</em>' reference list.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_Exceptions()
	 * @model
	 * @generated
	 */
	EList<AbapException> getExceptions();

	/**
	 * Returns the value of the '<em><b>Import Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Import Parameter List</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Import Parameter List</em>' attribute.
	 * @see #setImportParameterList(List)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_ImportParameterList()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.ParameterList" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	List<ListFieldMetaData> getImportParameterList();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImportParameterList <em>Import Parameter List</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Import Parameter List</em>' attribute.
	 * @see #getImportParameterList()
	 * @generated
	 */
	void setImportParameterList(List<ListFieldMetaData> value);

	/**
	 * Returns the value of the '<em><b>Export Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Export Parameter List</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Export Parameter List</em>' attribute.
	 * @see #setExportParameterList(List)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_ExportParameterList()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.ParameterList" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	List<ListFieldMetaData> getExportParameterList();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExportParameterList <em>Export Parameter List</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Export Parameter List</em>' attribute.
	 * @see #getExportParameterList()
	 * @generated
	 */
	void setExportParameterList(List<ListFieldMetaData> value);

	/**
	 * Returns the value of the '<em><b>Changing Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Changing Parameter List</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Changing Parameter List</em>' attribute.
	 * @see #setChangingParameterList(List)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_ChangingParameterList()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.ParameterList" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	List<ListFieldMetaData> getChangingParameterList();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChangingParameterList <em>Changing Parameter List</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Changing Parameter List</em>' attribute.
	 * @see #getChangingParameterList()
	 * @generated
	 */
	void setChangingParameterList(List<ListFieldMetaData> value);

	/**
	 * Returns the value of the '<em><b>Table Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Table Parameter List</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Table Parameter List</em>' attribute.
	 * @see #setTableParameterList(List)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_TableParameterList()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.ParameterList" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	List<ListFieldMetaData> getTableParameterList();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTableParameterList <em>Table Parameter List</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Table Parameter List</em>' attribute.
	 * @see #getTableParameterList()
	 * @generated
	 */
	void setTableParameterList(List<ListFieldMetaData> value);

	/**
	 * Returns the value of the '<em><b>Exception List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exception List</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exception List</em>' attribute.
	 * @see #setExceptionList(List)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getFunctionTemplate_ExceptionList()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.AbapExceptionList" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	List<AbapException> getExceptionList();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptionList <em>Exception List</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exception List</em>' attribute.
	 * @see #getExceptionList()
	 * @generated
	 */
	void setExceptionList(List<AbapException> value);

} // FunctionTemplate
