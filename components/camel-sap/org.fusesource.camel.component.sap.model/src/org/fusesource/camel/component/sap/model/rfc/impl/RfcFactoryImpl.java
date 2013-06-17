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
package org.fusesource.camel.component.sap.model.rfc.impl;

import java.util.Map;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.fusesource.camel.component.sap.model.rfc.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class RfcFactoryImpl extends EFactoryImpl implements RfcFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static RfcFactory init() {
		try {
			RfcFactory theRfcFactory = (RfcFactory)EPackage.Registry.INSTANCE.getEFactory("http://sap.fusesource.org/rfc"); 
			if (theRfcFactory != null) {
				return theRfcFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new RfcFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RfcFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case RfcPackage.DESTINATION: return createDestination();
			case RfcPackage.RFC: return createRFC();
			case RfcPackage.TABLE: return createTable();
			case RfcPackage.STRUCTURE: return createStructure();
			case RfcPackage.DESTINATION_DATA_ENTRY: return (EObject)createDestinationDataEntry();
			case RfcPackage.DESTINATION_DATA: return createDestinationData();
			case RfcPackage.DESTINATION_DATA_STORE_ENTRY: return (EObject)createDestinationDataStoreEntry();
			case RfcPackage.DESTINATION_DATA_STORE: return createDestinationDataStore();
			case RfcPackage.SERVER: return createServer();
			case RfcPackage.SERVER_DATA_ENTRY: return (EObject)createServerDataEntry();
			case RfcPackage.SERVER_DATA: return createServerData();
			case RfcPackage.SERVER_DATA_STORE_ENTRY: return (EObject)createServerDataStoreEntry();
			case RfcPackage.SERVER_DATA_STORE: return createServerDataStore();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Destination createDestination() {
		DestinationImpl destination = new DestinationImpl();
		return destination;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RFC createRFC() {
		RFCImpl rfc = new RFCImpl();
		return rfc;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public <S extends Structure> Table<S> createTable() {
		TableImpl<S> table = new TableImpl<S>();
		return table;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Structure createStructure() {
		StructureImpl structure = new StructureImpl();
		return structure;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Map.Entry<String, String> createDestinationDataEntry() {
		DestinationDataEntryImpl destinationDataEntry = new DestinationDataEntryImpl();
		return destinationDataEntry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DestinationData createDestinationData() {
		DestinationDataImpl destinationData = new DestinationDataImpl();
		return destinationData;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Map.Entry<String, DestinationData> createDestinationDataStoreEntry() {
		DestinationDataStoreEntryImpl destinationDataStoreEntry = new DestinationDataStoreEntryImpl();
		return destinationDataStoreEntry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DestinationDataStore createDestinationDataStore() {
		DestinationDataStoreImpl destinationDataStore = new DestinationDataStoreImpl();
		return destinationDataStore;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Server createServer() {
		ServerImpl server = new ServerImpl();
		return server;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Map.Entry<String, String> createServerDataEntry() {
		ServerDataEntryImpl serverDataEntry = new ServerDataEntryImpl();
		return serverDataEntry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServerData createServerData() {
		ServerDataImpl serverData = new ServerDataImpl();
		return serverData;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Map.Entry<String, ServerData> createServerDataStoreEntry() {
		ServerDataStoreEntryImpl serverDataStoreEntry = new ServerDataStoreEntryImpl();
		return serverDataStoreEntry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServerDataStore createServerDataStore() {
		ServerDataStoreImpl serverDataStore = new ServerDataStoreImpl();
		return serverDataStore;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RfcPackage getRfcPackage() {
		return (RfcPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static RfcPackage getPackage() {
		return RfcPackage.eINSTANCE;
	}

} //RfcFactoryImpl
