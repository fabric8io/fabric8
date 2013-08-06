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
package org.fusesource.camel.component.sap.model.rfc.util;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.Switch;
import org.fusesource.camel.component.sap.model.rfc.*;
import org.fusesource.camel.component.sap.model.rfc.AbapException;
import org.fusesource.camel.component.sap.model.rfc.Destination;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.FieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.FunctionTemplate;
import org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RFC;
import org.fusesource.camel.component.sap.model.rfc.RepositoryData;
import org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.Server;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.ServerDataStore;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage
 * @generated
 */
public class RfcSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static RfcPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RfcSwitch() {
		if (modelPackage == null) {
			modelPackage = RfcPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @parameter ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case RfcPackage.DESTINATION: {
				Destination destination = (Destination)theEObject;
				T result = caseDestination(destination);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.RFC: {
				RFC rfc = (RFC)theEObject;
				T result = caseRFC(rfc);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.TABLE: {
				Table<?> table = (Table<?>)theEObject;
				T result = caseTable(table);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.STRUCTURE: {
				Structure structure = (Structure)theEObject;
				T result = caseStructure(structure);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.DESTINATION_DATA_ENTRY: {
				@SuppressWarnings("unchecked") Map.Entry<String, String> destinationDataEntry = (Map.Entry<String, String>)theEObject;
				T result = caseDestinationDataEntry(destinationDataEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.DESTINATION_DATA: {
				DestinationData destinationData = (DestinationData)theEObject;
				T result = caseDestinationData(destinationData);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.DESTINATION_DATA_STORE_ENTRY: {
				@SuppressWarnings("unchecked") Map.Entry<String, DestinationData> destinationDataStoreEntry = (Map.Entry<String, DestinationData>)theEObject;
				T result = caseDestinationDataStoreEntry(destinationDataStoreEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.DESTINATION_DATA_STORE: {
				DestinationDataStore destinationDataStore = (DestinationDataStore)theEObject;
				T result = caseDestinationDataStore(destinationDataStore);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.SERVER: {
				Server server = (Server)theEObject;
				T result = caseServer(server);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.SERVER_DATA_ENTRY: {
				@SuppressWarnings("unchecked") Map.Entry<String, String> serverDataEntry = (Map.Entry<String, String>)theEObject;
				T result = caseServerDataEntry(serverDataEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.SERVER_DATA: {
				ServerData serverData = (ServerData)theEObject;
				T result = caseServerData(serverData);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.SERVER_DATA_STORE_ENTRY: {
				@SuppressWarnings("unchecked") Map.Entry<String, ServerData> serverDataStoreEntry = (Map.Entry<String, ServerData>)theEObject;
				T result = caseServerDataStoreEntry(serverDataStoreEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.SERVER_DATA_STORE: {
				ServerDataStore serverDataStore = (ServerDataStore)theEObject;
				T result = caseServerDataStore(serverDataStore);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.FUNCTION_TEMPLATE: {
				FunctionTemplate functionTemplate = (FunctionTemplate)theEObject;
				T result = caseFunctionTemplate(functionTemplate);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.RECORD_META_DATA: {
				RecordMetaData recordMetaData = (RecordMetaData)theEObject;
				T result = caseRecordMetaData(recordMetaData);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.FIELD_META_DATA: {
				FieldMetaData fieldMetaData = (FieldMetaData)theEObject;
				T result = caseFieldMetaData(fieldMetaData);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.LIST_FIELD_META_DATA: {
				ListFieldMetaData listFieldMetaData = (ListFieldMetaData)theEObject;
				T result = caseListFieldMetaData(listFieldMetaData);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.ABAP_EXCEPTION: {
				AbapException abapException = (AbapException)theEObject;
				T result = caseAbapException(abapException);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.RESPOSITORY_DATA_ENTRY: {
				@SuppressWarnings("unchecked") Map.Entry<String, FunctionTemplate> respositoryDataEntry = (Map.Entry<String, FunctionTemplate>)theEObject;
				T result = caseRespositoryDataEntry(respositoryDataEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.REPOSITORY_DATA: {
				RepositoryData repositoryData = (RepositoryData)theEObject;
				T result = caseRepositoryData(repositoryData);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.REPOSITORY_DATA_STORE: {
				RepositoryDataStore repositoryDataStore = (RepositoryDataStore)theEObject;
				T result = caseRepositoryDataStore(repositoryDataStore);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case RfcPackage.RESPOSITORY_DATA_STORE_ENTRY: {
				@SuppressWarnings("unchecked") Map.Entry<String, RepositoryData> respositoryDataStoreEntry = (Map.Entry<String, RepositoryData>)theEObject;
				T result = caseRespositoryDataStoreEntry(respositoryDataStoreEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Destination</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Destination</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDestination(Destination object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>RFC</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>RFC</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRFC(RFC object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Table</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Table</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public <S extends Structure> T caseTable(Table<S> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Structure</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Structure</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseStructure(Structure object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Destination Data Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Destination Data Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDestinationDataEntry(Map.Entry<String, String> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Destination Data</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Destination Data</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDestinationData(DestinationData object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Destination Data Store Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Destination Data Store Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDestinationDataStoreEntry(Map.Entry<String, DestinationData> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Destination Data Store</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Destination Data Store</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDestinationDataStore(DestinationDataStore object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Server</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Server</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServer(Server object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Server Data Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Server Data Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServerDataEntry(Map.Entry<String, String> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Server Data</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Server Data</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServerData(ServerData object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Server Data Store Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Server Data Store Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServerDataStoreEntry(Map.Entry<String, ServerData> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Server Data Store</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Server Data Store</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseServerDataStore(ServerDataStore object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Function Template</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Function Template</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFunctionTemplate(FunctionTemplate object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Record Meta Data</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Record Meta Data</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRecordMetaData(RecordMetaData object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Field Meta Data</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Field Meta Data</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFieldMetaData(FieldMetaData object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>List Field Meta Data</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>List Field Meta Data</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseListFieldMetaData(ListFieldMetaData object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Abap Exception</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Abap Exception</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAbapException(AbapException object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Respository Data Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Respository Data Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRespositoryDataEntry(Map.Entry<String, FunctionTemplate> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Repository Data</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Repository Data</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRepositoryData(RepositoryData object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Repository Data Store</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Repository Data Store</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRepositoryDataStore(RepositoryDataStore object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Respository Data Store Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Respository Data Store Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRespositoryDataStoreEntry(Map.Entry<String, RepositoryData> object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //RfcSwitch
