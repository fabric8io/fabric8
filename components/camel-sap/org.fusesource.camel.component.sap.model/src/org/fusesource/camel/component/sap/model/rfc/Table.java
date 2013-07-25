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
 *  implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.camel.component.sap.model.rfc;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Table</b></em>'.
 * @extends java.util.List<S>
 * <!-- end-user-doc -->
 *
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getTable()
 * @model
 * @generated
 */
public interface Table<S extends Structure> extends EObject, java.util.List<S> {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation"
	 * @generated
	 */
	EList<S> getRows();


	/**
	 * <!-- begin-user-doc -->
	 * Create and add new row to end of table.
	 * 
	 * @return The new row added to table.
	 * <!-- end-user-doc -->
	 * @model
	 * @generated
	 */
	S add();
	
	/**
	 * <!-- begin-user-doc -->
	 * Create and add new row to table at <code>index</code>.
	 * 
	 * @return The new row added to table.
	 * <!-- end-user-doc -->
	 * @model
	 * @generated
	 */
	S add(int index);
	
} // Table
