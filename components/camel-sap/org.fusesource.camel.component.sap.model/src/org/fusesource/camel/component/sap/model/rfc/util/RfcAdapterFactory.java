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

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.EObject;
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
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage
 * @generated
 */
public class RfcAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static RfcPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RfcAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = RfcPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RfcSwitch<Adapter> modelSwitch =
		new RfcSwitch<Adapter>() {
			@Override
			public Adapter caseDestination(Destination object) {
				return createDestinationAdapter();
			}
			@Override
			public Adapter caseRFC(RFC object) {
				return createRFCAdapter();
			}
			@Override
			public <S extends Structure> Adapter caseTable(Table<S> object) {
				return createTableAdapter();
			}
			@Override
			public Adapter caseStructure(Structure object) {
				return createStructureAdapter();
			}
			@Override
			public Adapter caseDestinationDataEntry(Map.Entry<String, String> object) {
				return createDestinationDataEntryAdapter();
			}
			@Override
			public Adapter caseDestinationData(DestinationData object) {
				return createDestinationDataAdapter();
			}
			@Override
			public Adapter caseDestinationDataStoreEntry(Map.Entry<String, DestinationData> object) {
				return createDestinationDataStoreEntryAdapter();
			}
			@Override
			public Adapter caseDestinationDataStore(DestinationDataStore object) {
				return createDestinationDataStoreAdapter();
			}
			@Override
			public Adapter caseServer(Server object) {
				return createServerAdapter();
			}
			@Override
			public Adapter caseServerDataEntry(Map.Entry<String, String> object) {
				return createServerDataEntryAdapter();
			}
			@Override
			public Adapter caseServerData(ServerData object) {
				return createServerDataAdapter();
			}
			@Override
			public Adapter caseServerDataStoreEntry(Map.Entry<String, ServerData> object) {
				return createServerDataStoreEntryAdapter();
			}
			@Override
			public Adapter caseServerDataStore(ServerDataStore object) {
				return createServerDataStoreAdapter();
			}
			@Override
			public Adapter caseFunctionTemplate(FunctionTemplate object) {
				return createFunctionTemplateAdapter();
			}
			@Override
			public Adapter caseRecordMetaData(RecordMetaData object) {
				return createRecordMetaDataAdapter();
			}
			@Override
			public Adapter caseFieldMetaData(FieldMetaData object) {
				return createFieldMetaDataAdapter();
			}
			@Override
			public Adapter caseListFieldMetaData(ListFieldMetaData object) {
				return createListFieldMetaDataAdapter();
			}
			@Override
			public Adapter caseAbapException(AbapException object) {
				return createAbapExceptionAdapter();
			}
			@Override
			public Adapter caseRespositoryDataEntry(Map.Entry<String, FunctionTemplate> object) {
				return createRespositoryDataEntryAdapter();
			}
			@Override
			public Adapter caseRepositoryData(RepositoryData object) {
				return createRepositoryDataAdapter();
			}
			@Override
			public Adapter caseRepositoryDataStore(RepositoryDataStore object) {
				return createRepositoryDataStoreAdapter();
			}
			@Override
			public Adapter caseRespositoryDataStoreEntry(Map.Entry<String, RepositoryData> object) {
				return createRespositoryDataStoreEntryAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.Destination <em>Destination</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.Destination
	 * @generated
	 */
	public Adapter createDestinationAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.RFC <em>RFC</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC
	 * @generated
	 */
	public Adapter createRFCAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.Table <em>Table</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.Table
	 * @generated
	 */
	public Adapter createTableAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.Structure <em>Structure</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.Structure
	 * @generated
	 */
	public Adapter createStructureAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>Destination Data Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createDestinationDataEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData <em>Destination Data</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData
	 * @generated
	 */
	public Adapter createDestinationDataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>Destination Data Store Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createDestinationDataStoreEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.DestinationDataStore <em>Destination Data Store</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationDataStore
	 * @generated
	 */
	public Adapter createDestinationDataStoreAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.Server <em>Server</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.Server
	 * @generated
	 */
	public Adapter createServerAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>Server Data Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createServerDataEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.ServerData <em>Server Data</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData
	 * @generated
	 */
	public Adapter createServerDataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>Server Data Store Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createServerDataStoreEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.ServerDataStore <em>Server Data Store</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerDataStore
	 * @generated
	 */
	public Adapter createServerDataStoreAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate <em>Function Template</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate
	 * @generated
	 */
	public Adapter createFunctionTemplateAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData <em>Record Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.RecordMetaData
	 * @generated
	 */
	public Adapter createRecordMetaDataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData <em>Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData
	 * @generated
	 */
	public Adapter createFieldMetaDataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData <em>List Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData
	 * @generated
	 */
	public Adapter createListFieldMetaDataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.AbapException <em>Abap Exception</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.AbapException
	 * @generated
	 */
	public Adapter createAbapExceptionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>Respository Data Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createRespositoryDataEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData <em>Repository Data</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryData
	 * @generated
	 */
	public Adapter createRepositoryDataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore <em>Repository Data Store</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore
	 * @generated
	 */
	public Adapter createRepositoryDataStoreAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>Respository Data Store Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createRespositoryDataStoreEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //RfcAdapterFactory
