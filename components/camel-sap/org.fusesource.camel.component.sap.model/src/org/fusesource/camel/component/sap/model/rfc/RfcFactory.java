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

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage
 * @generated
 */
public interface RfcFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	RfcFactory eINSTANCE = org.fusesource.camel.component.sap.model.rfc.impl.RfcFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Destination</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Destination</em>'.
	 * @generated
	 */
	Destination createDestination();

	/**
	 * Returns a new object of class '<em>RFC</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>RFC</em>'.
	 * @generated
	 */
	RFC createRFC();

	/**
	 * Returns a new object of class '<em>Table</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Table</em>'.
	 * @generated
	 */
	<S extends Structure> Table<S> createTable();

	/**
	 * Returns a new object of class '<em>Structure</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Structure</em>'.
	 * @generated
	 */
	Structure createStructure();

	/**
	 * Returns a new object of class '<em>Destination Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Destination Data</em>'.
	 * @generated
	 */
	DestinationData createDestinationData();

	/**
	 * Returns a new object of class '<em>Destination Data Store</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Destination Data Store</em>'.
	 * @generated
	 */
	DestinationDataStore createDestinationDataStore();

	/**
	 * Returns a new object of class '<em>Server</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Server</em>'.
	 * @generated
	 */
	Server createServer();

	/**
	 * Returns a new object of class '<em>Server Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Server Data</em>'.
	 * @generated
	 */
	ServerData createServerData();

	/**
	 * Returns a new object of class '<em>Server Data Store</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Server Data Store</em>'.
	 * @generated
	 */
	ServerDataStore createServerDataStore();

	/**
	 * Returns a new object of class '<em>Function Template</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Function Template</em>'.
	 * @generated
	 */
	FunctionTemplate createFunctionTemplate();

	/**
	 * Returns a new object of class '<em>Record Meta Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Record Meta Data</em>'.
	 * @generated
	 */
	RecordMetaData createRecordMetaData();

	/**
	 * Returns a new object of class '<em>Field Meta Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Field Meta Data</em>'.
	 * @generated
	 */
	FieldMetaData createFieldMetaData();

	/**
	 * Returns a new object of class '<em>List Field Meta Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>List Field Meta Data</em>'.
	 * @generated
	 */
	ListFieldMetaData createListFieldMetaData();

	/**
	 * Returns a new object of class '<em>Abap Exception</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Abap Exception</em>'.
	 * @generated
	 */
	AbapException createAbapException();

	/**
	 * Returns a new object of class '<em>Repository Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Repository Data</em>'.
	 * @generated
	 */
	RepositoryData createRepositoryData();

	/**
	 * Returns a new object of class '<em>Repository Data Store</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Repository Data Store</em>'.
	 * @generated
	 */
	RepositoryDataStore createRepositoryDataStore();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	RfcPackage getRfcPackage();

} //RfcFactory
