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

import java.util.Map;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Repository Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData#getEntries <em>Entries</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData#getFunctionTemplates <em>Function Templates</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRepositoryData()
 * @model
 * @generated
 */
public interface RepositoryData extends EObject {
	/**
	 * Returns the value of the '<em><b>Entries</b></em>' map.
	 * The key is of type {@link java.lang.String},
	 * and the value is of type {@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate},
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Entries</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Entries</em>' map.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRepositoryData_Entries()
	 * @model mapType="org.fusesource.camel.component.sap.model.rfc.RespositoryDataEntry<org.eclipse.emf.ecore.EString, org.fusesource.camel.component.sap.model.rfc.FunctionTemplate>"
	 * @generated
	 */
	EMap<String, FunctionTemplate> getEntries();

	/**
	 * Returns the value of the '<em><b>Function Templates</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Function Templates</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Function Templates</em>' attribute.
	 * @see #setFunctionTemplates(Map)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getRepositoryData_FunctionTemplates()
	 * @model dataType="org.fusesource.camel.component.sap.model.rfc.FunctionTemplateMap" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	Map<String, FunctionTemplate> getFunctionTemplates();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData#getFunctionTemplates <em>Function Templates</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Function Templates</em>' attribute.
	 * @see #getFunctionTemplates()
	 * @generated
	 */
	void setFunctionTemplates(Map<String, FunctionTemplate> value);

} // RepositoryData
