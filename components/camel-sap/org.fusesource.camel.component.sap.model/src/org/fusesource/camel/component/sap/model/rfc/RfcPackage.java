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

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see org.fusesource.camel.component.sap.model.rfc.RfcFactory
 * @model kind="package"
 * @generated
 */
public interface RfcPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "rfc";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://sap.fusesource.org/rfc";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "rfc";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	RfcPackage eINSTANCE = org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationImpl <em>Destination</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestination()
	 * @generated
	 */
	int DESTINATION = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION__NAME = 0;

	/**
	 * The feature id for the '<em><b>Repository Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION__REPOSITORY_NAME = 1;

	/**
	 * The feature id for the '<em><b>Rfcs</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION__RFCS = 2;

	/**
	 * The number of structural features of the '<em>Destination</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RFCImpl <em>RFC</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RFCImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRFC()
	 * @generated
	 */
	int RFC = 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC__NAME = 0;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC__GROUP = 1;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC__DESCRIPTION = 2;

	/**
	 * The feature id for the '<em><b>Request</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC__REQUEST = 3;

	/**
	 * The feature id for the '<em><b>Response</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC__RESPONSE = 4;

	/**
	 * The feature id for the '<em><b>Destination</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC__DESTINATION = 5;

	/**
	 * The number of structural features of the '<em>RFC</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RFC_FEATURE_COUNT = 6;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.TableImpl <em>Table</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.TableImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getTable()
	 * @generated
	 */
	int TABLE = 2;

	/**
	 * The number of structural features of the '<em>Table</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TABLE_FEATURE_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.StructureImpl <em>Structure</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.StructureImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getStructure()
	 * @generated
	 */
	int STRUCTURE = 3;

	/**
	 * The number of structural features of the '<em>Structure</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STRUCTURE_FEATURE_COUNT = 0;


	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataEntryImpl <em>Destination Data Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataEntryImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationDataEntry()
	 * @generated
	 */
	int DESTINATION_DATA_ENTRY = 4;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_ENTRY__KEY = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_ENTRY__VALUE = 1;

	/**
	 * The number of structural features of the '<em>Destination Data Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_ENTRY_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl <em>Destination Data</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationData()
	 * @generated
	 */
	int DESTINATION_DATA = 5;

	/**
	 * The feature id for the '<em><b>Entries</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__ENTRIES = 0;

	/**
	 * The feature id for the '<em><b>Alias User</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__ALIAS_USER = 1;

	/**
	 * The feature id for the '<em><b>Ashost</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__ASHOST = 2;

	/**
	 * The feature id for the '<em><b>Auth Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__AUTH_TYPE = 3;

	/**
	 * The feature id for the '<em><b>Client</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__CLIENT = 4;

	/**
	 * The feature id for the '<em><b>Codepage</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__CODEPAGE = 5;

	/**
	 * The feature id for the '<em><b>Cpic Trace</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__CPIC_TRACE = 6;

	/**
	 * The feature id for the '<em><b>Deny Initial Password</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__DENY_INITIAL_PASSWORD = 7;

	/**
	 * The feature id for the '<em><b>Expiration Period</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__EXPIRATION_PERIOD = 8;

	/**
	 * The feature id for the '<em><b>Expiration Time</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__EXPIRATION_TIME = 9;

	/**
	 * The feature id for the '<em><b>Getsso2</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__GETSSO2 = 10;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__GROUP = 11;

	/**
	 * The feature id for the '<em><b>Gwhost</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__GWHOST = 12;

	/**
	 * The feature id for the '<em><b>Gwserv</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__GWSERV = 13;

	/**
	 * The feature id for the '<em><b>Lang</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__LANG = 14;

	/**
	 * The feature id for the '<em><b>Lcheck</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__LCHECK = 15;

	/**
	 * The feature id for the '<em><b>Max Get Time</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__MAX_GET_TIME = 16;

	/**
	 * The feature id for the '<em><b>Mshost</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__MSHOST = 17;

	/**
	 * The feature id for the '<em><b>Msserv</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__MSSERV = 18;

	/**
	 * The feature id for the '<em><b>Mysapsso2</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__MYSAPSSO2 = 19;

	/**
	 * The feature id for the '<em><b>Passwd</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__PASSWD = 20;

	/**
	 * The feature id for the '<em><b>Password</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__PASSWORD = 21;

	/**
	 * The feature id for the '<em><b>Pcs</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__PCS = 22;

	/**
	 * The feature id for the '<em><b>Peak Limit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__PEAK_LIMIT = 23;

	/**
	 * The feature id for the '<em><b>Ping On Create</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__PING_ON_CREATE = 24;

	/**
	 * The feature id for the '<em><b>Pool Capacity</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__POOL_CAPACITY = 25;

	/**
	 * The feature id for the '<em><b>R3name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__R3NAME = 26;

	/**
	 * The feature id for the '<em><b>Repository Dest</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__REPOSITORY_DEST = 27;

	/**
	 * The feature id for the '<em><b>Repository Passwd</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__REPOSITORY_PASSWD = 28;

	/**
	 * The feature id for the '<em><b>Repository Roundtrip Optimization</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION = 29;

	/**
	 * The feature id for the '<em><b>Repository Snc</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__REPOSITORY_SNC = 30;

	/**
	 * The feature id for the '<em><b>Repository User</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__REPOSITORY_USER = 31;

	/**
	 * The feature id for the '<em><b>Saprouter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SAPROUTER = 32;

	/**
	 * The feature id for the '<em><b>Snc Library</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SNC_LIBRARY = 33;

	/**
	 * The feature id for the '<em><b>Snc Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SNC_MODE = 34;

	/**
	 * The feature id for the '<em><b>Snc Myname</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SNC_MYNAME = 35;

	/**
	 * The feature id for the '<em><b>Snc Partnername</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SNC_PARTNERNAME = 36;

	/**
	 * The feature id for the '<em><b>Snc Qop</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SNC_QOP = 37;

	/**
	 * The feature id for the '<em><b>Sysnr</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__SYSNR = 38;

	/**
	 * The feature id for the '<em><b>Tphost</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__TPHOST = 39;

	/**
	 * The feature id for the '<em><b>Tpname</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__TPNAME = 40;

	/**
	 * The feature id for the '<em><b>Trace</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__TRACE = 41;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__TYPE = 42;

	/**
	 * The feature id for the '<em><b>User Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__USER_NAME = 43;

	/**
	 * The feature id for the '<em><b>User</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__USER = 44;

	/**
	 * The feature id for the '<em><b>User Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__USER_ID = 45;

	/**
	 * The feature id for the '<em><b>Use Sapgui</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__USE_SAPGUI = 46;

	/**
	 * The feature id for the '<em><b>X50 9cert</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA__X509CERT = 47;

	/**
	 * The number of structural features of the '<em>Destination Data</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_FEATURE_COUNT = 48;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreEntryImpl <em>Destination Data Store Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreEntryImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationDataStoreEntry()
	 * @generated
	 */
	int DESTINATION_DATA_STORE_ENTRY = 6;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_STORE_ENTRY__KEY = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_STORE_ENTRY__VALUE = 1;

	/**
	 * The number of structural features of the '<em>Destination Data Store Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_STORE_ENTRY_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreImpl <em>Destination Data Store</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationDataStore()
	 * @generated
	 */
	int DESTINATION_DATA_STORE = 7;

	/**
	 * The feature id for the '<em><b>Entries</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_STORE__ENTRIES = 0;

	/**
	 * The number of structural features of the '<em>Destination Data Store</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DESTINATION_DATA_STORE_FEATURE_COUNT = 1;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerImpl <em>Server</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServer()
	 * @generated
	 */
	int SERVER = 8;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER__NAME = 0;

	/**
	 * The number of structural features of the '<em>Server</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_FEATURE_COUNT = 1;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataEntryImpl <em>Server Data Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataEntryImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerDataEntry()
	 * @generated
	 */
	int SERVER_DATA_ENTRY = 9;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_ENTRY__KEY = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_ENTRY__VALUE = 1;

	/**
	 * The number of structural features of the '<em>Server Data Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_ENTRY_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl <em>Server Data</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerData()
	 * @generated
	 */
	int SERVER_DATA = 10;

	/**
	 * The feature id for the '<em><b>Entries</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__ENTRIES = 0;

	/**
	 * The feature id for the '<em><b>Gwhost</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__GWHOST = 1;

	/**
	 * The feature id for the '<em><b>Gwserv</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__GWSERV = 2;

	/**
	 * The feature id for the '<em><b>Progid</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__PROGID = 3;

	/**
	 * The feature id for the '<em><b>Connection Count</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__CONNECTION_COUNT = 4;

	/**
	 * The feature id for the '<em><b>Saprouter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__SAPROUTER = 5;

	/**
	 * The feature id for the '<em><b>Max Start Up Delay</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__MAX_START_UP_DELAY = 6;

	/**
	 * The feature id for the '<em><b>Repository Destination</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__REPOSITORY_DESTINATION = 7;

	/**
	 * The feature id for the '<em><b>Repository Map</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__REPOSITORY_MAP = 8;

	/**
	 * The feature id for the '<em><b>Trace</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__TRACE = 9;

	/**
	 * The feature id for the '<em><b>Worker Thread Count</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__WORKER_THREAD_COUNT = 10;

	/**
	 * The feature id for the '<em><b>Worker Thread Min Count</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__WORKER_THREAD_MIN_COUNT = 11;

	/**
	 * The feature id for the '<em><b>Snc Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__SNC_MODE = 12;

	/**
	 * The feature id for the '<em><b>Snc Qop</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__SNC_QOP = 13;

	/**
	 * The feature id for the '<em><b>Snc Myname</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__SNC_MYNAME = 14;

	/**
	 * The feature id for the '<em><b>Snc Lib</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA__SNC_LIB = 15;

	/**
	 * The number of structural features of the '<em>Server Data</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_FEATURE_COUNT = 16;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreEntryImpl <em>Server Data Store Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreEntryImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerDataStoreEntry()
	 * @generated
	 */
	int SERVER_DATA_STORE_ENTRY = 11;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_STORE_ENTRY__KEY = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_STORE_ENTRY__VALUE = 1;

	/**
	 * The number of structural features of the '<em>Server Data Store Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_STORE_ENTRY_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreImpl <em>Server Data Store</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerDataStore()
	 * @generated
	 */
	int SERVER_DATA_STORE = 12;

	/**
	 * The feature id for the '<em><b>Entries</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_STORE__ENTRIES = 0;

	/**
	 * The number of structural features of the '<em>Server Data Store</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SERVER_DATA_STORE_FEATURE_COUNT = 1;


	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl <em>Function Template</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFunctionTemplate()
	 * @generated
	 */
	int FUNCTION_TEMPLATE = 13;

	/**
	 * The feature id for the '<em><b>Imports</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__IMPORTS = 0;

	/**
	 * The feature id for the '<em><b>Exports</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__EXPORTS = 1;

	/**
	 * The feature id for the '<em><b>Changing</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__CHANGING = 2;

	/**
	 * The feature id for the '<em><b>Tables</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__TABLES = 3;

	/**
	 * The feature id for the '<em><b>Exceptions</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__EXCEPTIONS = 4;

	/**
	 * The feature id for the '<em><b>Import Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST = 5;

	/**
	 * The feature id for the '<em><b>Export Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST = 6;

	/**
	 * The feature id for the '<em><b>Changing Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST = 7;

	/**
	 * The feature id for the '<em><b>Table Parameter List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST = 8;

	/**
	 * The feature id for the '<em><b>Exception List</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE__EXCEPTION_LIST = 9;

	/**
	 * The number of structural features of the '<em>Function Template</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FUNCTION_TEMPLATE_FEATURE_COUNT = 10;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl <em>Record Meta Data</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRecordMetaData()
	 * @generated
	 */
	int RECORD_META_DATA = 14;

	/**
	 * The feature id for the '<em><b>Field Meta Data</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RECORD_META_DATA__FIELD_META_DATA = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RECORD_META_DATA__NAME = 1;

	/**
	 * The feature id for the '<em><b>Record Field Meta Data</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RECORD_META_DATA__RECORD_FIELD_META_DATA = 2;

	/**
	 * The number of structural features of the '<em>Record Meta Data</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RECORD_META_DATA_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl <em>Field Meta Data</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFieldMetaData()
	 * @generated
	 */
	int FIELD_META_DATA = 15;

	/**
	 * The feature id for the '<em><b>Field Meta Data</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__FIELD_META_DATA = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__NAME = 1;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__TYPE = 2;

	/**
	 * The feature id for the '<em><b>Byte Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__BYTE_LENGTH = 3;

	/**
	 * The feature id for the '<em><b>Byte Offset</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__BYTE_OFFSET = 4;

	/**
	 * The feature id for the '<em><b>Unicode Byte Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__UNICODE_BYTE_LENGTH = 5;

	/**
	 * The feature id for the '<em><b>Unicode Byte Offset</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__UNICODE_BYTE_OFFSET = 6;

	/**
	 * The feature id for the '<em><b>Decimals</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__DECIMALS = 7;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__DESCRIPTION = 8;

	/**
	 * The feature id for the '<em><b>Record Meta Data</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA__RECORD_META_DATA = 9;

	/**
	 * The number of structural features of the '<em>Field Meta Data</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FIELD_META_DATA_FEATURE_COUNT = 10;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ListFieldMetaDataImpl <em>List Field Meta Data</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.ListFieldMetaDataImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getListFieldMetaData()
	 * @generated
	 */
	int LIST_FIELD_META_DATA = 16;

	/**
	 * The feature id for the '<em><b>Field Meta Data</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__FIELD_META_DATA = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__NAME = 1;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__TYPE = 2;

	/**
	 * The feature id for the '<em><b>Byte Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__BYTE_LENGTH = 3;

	/**
	 * The feature id for the '<em><b>Unicode Byte Length</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__UNICODE_BYTE_LENGTH = 4;

	/**
	 * The feature id for the '<em><b>Decimals</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__DECIMALS = 5;

	/**
	 * The feature id for the '<em><b>Defaults</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__DEFAULTS = 6;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__DESCRIPTION = 7;

	/**
	 * The feature id for the '<em><b>Import</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__IMPORT = 8;

	/**
	 * The feature id for the '<em><b>Changing</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__CHANGING = 9;

	/**
	 * The feature id for the '<em><b>Export</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__EXPORT = 10;

	/**
	 * The feature id for the '<em><b>Exception</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__EXCEPTION = 11;

	/**
	 * The feature id for the '<em><b>Optional</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__OPTIONAL = 12;

	/**
	 * The feature id for the '<em><b>Record Meta Data</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA__RECORD_META_DATA = 13;

	/**
	 * The number of structural features of the '<em>List Field Meta Data</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_FIELD_META_DATA_FEATURE_COUNT = 14;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.AbapExceptionImpl <em>Abap Exception</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.AbapExceptionImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getAbapException()
	 * @generated
	 */
	int ABAP_EXCEPTION = 17;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABAP_EXCEPTION__KEY = 0;

	/**
	 * The feature id for the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABAP_EXCEPTION__MESSAGE = 1;

	/**
	 * The number of structural features of the '<em>Abap Exception</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABAP_EXCEPTION_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataEntryImpl <em>Respository Data Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataEntryImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRespositoryDataEntry()
	 * @generated
	 */
	int RESPOSITORY_DATA_ENTRY = 18;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPOSITORY_DATA_ENTRY__KEY = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPOSITORY_DATA_ENTRY__VALUE = 1;

	/**
	 * The number of structural features of the '<em>Respository Data Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPOSITORY_DATA_ENTRY_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataImpl <em>Repository Data</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRepositoryData()
	 * @generated
	 */
	int REPOSITORY_DATA = 19;

	/**
	 * The feature id for the '<em><b>Entries</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPOSITORY_DATA__ENTRIES = 0;

	/**
	 * The feature id for the '<em><b>Function Templates</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPOSITORY_DATA__FUNCTION_TEMPLATES = 1;

	/**
	 * The number of structural features of the '<em>Repository Data</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPOSITORY_DATA_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataStoreImpl <em>Repository Data Store</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataStoreImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRepositoryDataStore()
	 * @generated
	 */
	int REPOSITORY_DATA_STORE = 20;

	/**
	 * The feature id for the '<em><b>Entries</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPOSITORY_DATA_STORE__ENTRIES = 0;

	/**
	 * The number of structural features of the '<em>Repository Data Store</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPOSITORY_DATA_STORE_FEATURE_COUNT = 1;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataStoreEntryImpl <em>Respository Data Store Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataStoreEntryImpl
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRespositoryDataStoreEntry()
	 * @generated
	 */
	int RESPOSITORY_DATA_STORE_ENTRY = 21;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPOSITORY_DATA_STORE_ENTRY__KEY = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPOSITORY_DATA_STORE_ENTRY__VALUE = 1;

	/**
	 * The number of structural features of the '<em>Respository Data Store Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPOSITORY_DATA_STORE_ENTRY_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.fusesource.camel.component.sap.model.rfc.DataType <em>Data Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.fusesource.camel.component.sap.model.rfc.DataType
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDataType()
	 * @generated
	 */
	int DATA_TYPE = 22;


	/**
	 * The meta object id for the '<em>Parameter List</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.util.List
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getParameterList()
	 * @generated
	 */
	int PARAMETER_LIST = 23;

	/**
	 * The meta object id for the '<em>Field List</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.util.List
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFieldList()
	 * @generated
	 */
	int FIELD_LIST = 24;

	/**
	 * The meta object id for the '<em>Abap Exception List</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.util.List
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getAbapExceptionList()
	 * @generated
	 */
	int ABAP_EXCEPTION_LIST = 25;


	/**
	 * The meta object id for the '<em>Function Template Map</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.util.Map
	 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFunctionTemplateMap()
	 * @generated
	 */
	int FUNCTION_TEMPLATE_MAP = 26;


	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.Destination <em>Destination</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Destination</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Destination
	 * @generated
	 */
	EClass getDestination();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.Destination#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Destination#getName()
	 * @see #getDestination()
	 * @generated
	 */
	EAttribute getDestination_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.Destination#getRepositoryName <em>Repository Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Destination#getRepositoryName()
	 * @see #getDestination()
	 * @generated
	 */
	EAttribute getDestination_RepositoryName();

	/**
	 * Returns the meta object for the containment reference list '{@link org.fusesource.camel.component.sap.model.rfc.Destination#getRfcs <em>Rfcs</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Rfcs</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Destination#getRfcs()
	 * @see #getDestination()
	 * @generated
	 */
	EReference getDestination_Rfcs();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.RFC <em>RFC</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>RFC</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC
	 * @generated
	 */
	EClass getRFC();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.RFC#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC#getName()
	 * @see #getRFC()
	 * @generated
	 */
	EAttribute getRFC_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.RFC#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Group</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC#getGroup()
	 * @see #getRFC()
	 * @generated
	 */
	EAttribute getRFC_Group();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.RFC#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC#getDescription()
	 * @see #getRFC()
	 * @generated
	 */
	EAttribute getRFC_Description();

	/**
	 * Returns the meta object for the containment reference '{@link org.fusesource.camel.component.sap.model.rfc.RFC#getRequest <em>Request</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Request</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC#getRequest()
	 * @see #getRFC()
	 * @generated
	 */
	EReference getRFC_Request();

	/**
	 * Returns the meta object for the containment reference '{@link org.fusesource.camel.component.sap.model.rfc.RFC#getResponse <em>Response</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Response</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC#getResponse()
	 * @see #getRFC()
	 * @generated
	 */
	EReference getRFC_Response();

	/**
	 * Returns the meta object for the container reference '{@link org.fusesource.camel.component.sap.model.rfc.RFC#getDestination <em>Destination</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Destination</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RFC#getDestination()
	 * @see #getRFC()
	 * @generated
	 */
	EReference getRFC_Destination();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.Table <em>Table</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Table</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Table
	 * @generated
	 */
	EClass getTable();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.Structure <em>Structure</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Structure</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Structure
	 * @generated
	 */
	EClass getStructure();

	/**
	 * Returns the meta object for class '{@link java.util.Map.Entry <em>Destination Data Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Destination Data Entry</em>'.
	 * @see java.util.Map.Entry
	 * @model keyDataType="org.eclipse.emf.ecore.EString" keyRequired="true"
	 *        valueDataType="org.eclipse.emf.ecore.EString" valueRequired="true"
	 * @generated
	 */
	EClass getDestinationDataEntry();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see java.util.Map.Entry
	 * @see #getDestinationDataEntry()
	 * @generated
	 */
	EAttribute getDestinationDataEntry_Key();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see java.util.Map.Entry
	 * @see #getDestinationDataEntry()
	 * @generated
	 */
	EAttribute getDestinationDataEntry_Value();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData <em>Destination Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Destination Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData
	 * @generated
	 */
	EClass getDestinationData();

	/**
	 * Returns the meta object for the map '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getEntries <em>Entries</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Entries</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getEntries()
	 * @see #getDestinationData()
	 * @generated
	 */
	EReference getDestinationData_Entries();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAliasUser <em>Alias User</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Alias User</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getAliasUser()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_AliasUser();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAshost <em>Ashost</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ashost</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getAshost()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Ashost();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAuthType <em>Auth Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Auth Type</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getAuthType()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_AuthType();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getClient <em>Client</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Client</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getClient()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Client();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getCodepage <em>Codepage</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Codepage</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getCodepage()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Codepage();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getCpicTrace <em>Cpic Trace</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Cpic Trace</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getCpicTrace()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_CpicTrace();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getDenyInitialPassword <em>Deny Initial Password</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Deny Initial Password</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getDenyInitialPassword()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_DenyInitialPassword();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationPeriod <em>Expiration Period</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expiration Period</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationPeriod()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_ExpirationPeriod();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationTime <em>Expiration Time</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expiration Time</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationTime()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_ExpirationTime();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGetsso2 <em>Getsso2</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Getsso2</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getGetsso2()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Getsso2();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Group</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getGroup()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Group();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwhost <em>Gwhost</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Gwhost</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwhost()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Gwhost();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwserv <em>Gwserv</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Gwserv</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwserv()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Gwserv();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getLang <em>Lang</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Lang</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getLang()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Lang();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getLcheck <em>Lcheck</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Lcheck</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getLcheck()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Lcheck();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMaxGetTime <em>Max Get Time</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Get Time</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getMaxGetTime()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_MaxGetTime();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMshost <em>Mshost</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Mshost</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getMshost()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Mshost();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMsserv <em>Msserv</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Msserv</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getMsserv()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Msserv();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMysapsso2 <em>Mysapsso2</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Mysapsso2</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getMysapsso2()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Mysapsso2();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPasswd <em>Passwd</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Passwd</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getPasswd()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Passwd();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPassword <em>Password</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Password</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getPassword()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Password();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPcs <em>Pcs</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pcs</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getPcs()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Pcs();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPeakLimit <em>Peak Limit</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Peak Limit</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getPeakLimit()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_PeakLimit();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPingOnCreate <em>Ping On Create</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ping On Create</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getPingOnCreate()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_PingOnCreate();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPoolCapacity <em>Pool Capacity</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pool Capacity</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getPoolCapacity()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_PoolCapacity();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getR3name <em>R3name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>R3name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getR3name()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_R3name();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryDest <em>Repository Dest</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Dest</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryDest()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_RepositoryDest();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryPasswd <em>Repository Passwd</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Passwd</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryPasswd()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_RepositoryPasswd();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryRoundtripOptimization <em>Repository Roundtrip Optimization</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Roundtrip Optimization</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryRoundtripOptimization()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_RepositoryRoundtripOptimization();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositorySnc <em>Repository Snc</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Snc</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositorySnc()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_RepositorySnc();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryUser <em>Repository User</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository User</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryUser()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_RepositoryUser();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSaprouter <em>Saprouter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Saprouter</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSaprouter()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Saprouter();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncLibrary <em>Snc Library</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Library</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncLibrary()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_SncLibrary();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMode <em>Snc Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Mode</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMode()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_SncMode();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMyname <em>Snc Myname</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Myname</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMyname()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_SncMyname();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncPartnername <em>Snc Partnername</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Partnername</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncPartnername()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_SncPartnername();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncQop <em>Snc Qop</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Qop</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncQop()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_SncQop();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSysnr <em>Sysnr</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Sysnr</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getSysnr()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Sysnr();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTphost <em>Tphost</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Tphost</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getTphost()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Tphost();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTpname <em>Tpname</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Tpname</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getTpname()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Tpname();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTrace <em>Trace</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Trace</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getTrace()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Trace();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getType()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_Type();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserName <em>User Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>User Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserName()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_UserName();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUser <em>User</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>User</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getUser()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_User();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserId <em>User Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>User Id</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserId()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_UserId();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUseSapgui <em>Use Sapgui</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Use Sapgui</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getUseSapgui()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_UseSapgui();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getX509cert <em>X50 9cert</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>X50 9cert</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationData#getX509cert()
	 * @see #getDestinationData()
	 * @generated
	 */
	EAttribute getDestinationData_X509cert();

	/**
	 * Returns the meta object for class '{@link java.util.Map.Entry <em>Destination Data Store Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Destination Data Store Entry</em>'.
	 * @see java.util.Map.Entry
	 * @model keyDataType="org.eclipse.emf.ecore.EString" keyRequired="true"
	 *        valueType="org.fusesource.camel.component.sap.model.rfc.DestinationData" valueRequired="true"
	 * @generated
	 */
	EClass getDestinationDataStoreEntry();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see java.util.Map.Entry
	 * @see #getDestinationDataStoreEntry()
	 * @generated
	 */
	EAttribute getDestinationDataStoreEntry_Key();

	/**
	 * Returns the meta object for the reference '{@link java.util.Map.Entry <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Value</em>'.
	 * @see java.util.Map.Entry
	 * @see #getDestinationDataStoreEntry()
	 * @generated
	 */
	EReference getDestinationDataStoreEntry_Value();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.DestinationDataStore <em>Destination Data Store</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Destination Data Store</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationDataStore
	 * @generated
	 */
	EClass getDestinationDataStore();

	/**
	 * Returns the meta object for the map '{@link org.fusesource.camel.component.sap.model.rfc.DestinationDataStore#getEntries <em>Entries</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Entries</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DestinationDataStore#getEntries()
	 * @see #getDestinationDataStore()
	 * @generated
	 */
	EReference getDestinationDataStore_Entries();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.Server <em>Server</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Server</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Server
	 * @generated
	 */
	EClass getServer();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.Server#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.Server#getName()
	 * @see #getServer()
	 * @generated
	 */
	EAttribute getServer_Name();

	/**
	 * Returns the meta object for class '{@link java.util.Map.Entry <em>Server Data Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Server Data Entry</em>'.
	 * @see java.util.Map.Entry
	 * @model keyDataType="org.eclipse.emf.ecore.EString" keyRequired="true"
	 *        valueDataType="org.eclipse.emf.ecore.EString" valueRequired="true"
	 * @generated
	 */
	EClass getServerDataEntry();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see java.util.Map.Entry
	 * @see #getServerDataEntry()
	 * @generated
	 */
	EAttribute getServerDataEntry_Key();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see java.util.Map.Entry
	 * @see #getServerDataEntry()
	 * @generated
	 */
	EAttribute getServerDataEntry_Value();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.ServerData <em>Server Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Server Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData
	 * @generated
	 */
	EClass getServerData();

	/**
	 * Returns the meta object for the map '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getEntries <em>Entries</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Entries</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getEntries()
	 * @see #getServerData()
	 * @generated
	 */
	EReference getServerData_Entries();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getGwhost <em>Gwhost</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Gwhost</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getGwhost()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_Gwhost();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getGwserv <em>Gwserv</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Gwserv</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getGwserv()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_Gwserv();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getProgid <em>Progid</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Progid</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getProgid()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_Progid();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getConnectionCount <em>Connection Count</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Connection Count</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getConnectionCount()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_ConnectionCount();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSaprouter <em>Saprouter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Saprouter</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getSaprouter()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_Saprouter();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getMaxStartUpDelay <em>Max Start Up Delay</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Start Up Delay</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getMaxStartUpDelay()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_MaxStartUpDelay();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryDestination <em>Repository Destination</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Destination</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryDestination()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_RepositoryDestination();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryMap <em>Repository Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Map</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryMap()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_RepositoryMap();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getTrace <em>Trace</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Trace</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getTrace()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_Trace();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadCount <em>Worker Thread Count</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Worker Thread Count</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadCount()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_WorkerThreadCount();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadMinCount <em>Worker Thread Min Count</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Worker Thread Min Count</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadMinCount()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_WorkerThreadMinCount();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMode <em>Snc Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Mode</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMode()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_SncMode();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncQop <em>Snc Qop</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Qop</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getSncQop()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_SncQop();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMyname <em>Snc Myname</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Myname</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMyname()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_SncMyname();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncLib <em>Snc Lib</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Snc Lib</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerData#getSncLib()
	 * @see #getServerData()
	 * @generated
	 */
	EAttribute getServerData_SncLib();

	/**
	 * Returns the meta object for class '{@link java.util.Map.Entry <em>Server Data Store Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Server Data Store Entry</em>'.
	 * @see java.util.Map.Entry
	 * @model keyDataType="org.eclipse.emf.ecore.EString" keyRequired="true"
	 *        valueType="org.fusesource.camel.component.sap.model.rfc.ServerData" valueRequired="true"
	 * @generated
	 */
	EClass getServerDataStoreEntry();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see java.util.Map.Entry
	 * @see #getServerDataStoreEntry()
	 * @generated
	 */
	EAttribute getServerDataStoreEntry_Key();

	/**
	 * Returns the meta object for the reference '{@link java.util.Map.Entry <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Value</em>'.
	 * @see java.util.Map.Entry
	 * @see #getServerDataStoreEntry()
	 * @generated
	 */
	EReference getServerDataStoreEntry_Value();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.ServerDataStore <em>Server Data Store</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Server Data Store</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerDataStore
	 * @generated
	 */
	EClass getServerDataStore();

	/**
	 * Returns the meta object for the map '{@link org.fusesource.camel.component.sap.model.rfc.ServerDataStore#getEntries <em>Entries</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Entries</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ServerDataStore#getEntries()
	 * @see #getServerDataStore()
	 * @generated
	 */
	EReference getServerDataStore_Entries();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate <em>Function Template</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Function Template</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate
	 * @generated
	 */
	EClass getFunctionTemplate();

	/**
	 * Returns the meta object for the reference list '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImports <em>Imports</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Imports</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImports()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EReference getFunctionTemplate_Imports();

	/**
	 * Returns the meta object for the reference list '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExports <em>Exports</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Exports</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExports()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EReference getFunctionTemplate_Exports();

	/**
	 * Returns the meta object for the reference list '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChanging <em>Changing</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Changing</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChanging()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EReference getFunctionTemplate_Changing();

	/**
	 * Returns the meta object for the reference list '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTables <em>Tables</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Tables</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTables()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EReference getFunctionTemplate_Tables();

	/**
	 * Returns the meta object for the reference list '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptions <em>Exceptions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Exceptions</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptions()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EReference getFunctionTemplate_Exceptions();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImportParameterList <em>Import Parameter List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Import Parameter List</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getImportParameterList()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EAttribute getFunctionTemplate_ImportParameterList();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExportParameterList <em>Export Parameter List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Export Parameter List</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExportParameterList()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EAttribute getFunctionTemplate_ExportParameterList();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChangingParameterList <em>Changing Parameter List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Changing Parameter List</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getChangingParameterList()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EAttribute getFunctionTemplate_ChangingParameterList();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTableParameterList <em>Table Parameter List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Table Parameter List</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getTableParameterList()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EAttribute getFunctionTemplate_TableParameterList();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptionList <em>Exception List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Exception List</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FunctionTemplate#getExceptionList()
	 * @see #getFunctionTemplate()
	 * @generated
	 */
	EAttribute getFunctionTemplate_ExceptionList();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData <em>Record Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Record Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RecordMetaData
	 * @generated
	 */
	EClass getRecordMetaData();

	/**
	 * Returns the meta object for the containment reference list '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getFieldMetaData <em>Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Field Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getFieldMetaData()
	 * @see #getRecordMetaData()
	 * @generated
	 */
	EReference getRecordMetaData_FieldMetaData();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getName()
	 * @see #getRecordMetaData()
	 * @generated
	 */
	EAttribute getRecordMetaData_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getRecordFieldMetaData <em>Record Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Record Field Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RecordMetaData#getRecordFieldMetaData()
	 * @see #getRecordMetaData()
	 * @generated
	 */
	EAttribute getRecordMetaData_RecordFieldMetaData();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData <em>Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Field Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData
	 * @generated
	 */
	EClass getFieldMetaData();

	/**
	 * Returns the meta object for the containment reference list '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getFieldMetaData <em>Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Field Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getFieldMetaData()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EReference getFieldMetaData_FieldMetaData();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getName()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getType()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_Type();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteLength <em>Byte Length</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Byte Length</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteLength()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_ByteLength();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteOffset <em>Byte Offset</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Byte Offset</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getByteOffset()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_ByteOffset();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteLength <em>Unicode Byte Length</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Unicode Byte Length</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteLength()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_UnicodeByteLength();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteOffset <em>Unicode Byte Offset</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Unicode Byte Offset</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getUnicodeByteOffset()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_UnicodeByteOffset();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDecimals <em>Decimals</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Decimals</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDecimals()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_Decimals();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getDescription()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EAttribute getFieldMetaData_Description();

	/**
	 * Returns the meta object for the reference '{@link org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getRecordMetaData <em>Record Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Record Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.FieldMetaData#getRecordMetaData()
	 * @see #getFieldMetaData()
	 * @generated
	 */
	EReference getFieldMetaData_RecordMetaData();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData <em>List Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>List Field Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData
	 * @generated
	 */
	EClass getListFieldMetaData();

	/**
	 * Returns the meta object for the containment reference list '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getFieldMetaData <em>Field Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Field Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getFieldMetaData()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EReference getListFieldMetaData_FieldMetaData();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getName()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getType()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Type();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getByteLength <em>Byte Length</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Byte Length</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getByteLength()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_ByteLength();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getUnicodeByteLength <em>Unicode Byte Length</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Unicode Byte Length</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getUnicodeByteLength()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_UnicodeByteLength();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDecimals <em>Decimals</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Decimals</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDecimals()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Decimals();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDefaults <em>Defaults</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Defaults</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDefaults()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Defaults();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getDescription()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Description();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isImport <em>Import</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Import</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isImport()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Import();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isChanging <em>Changing</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Changing</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isChanging()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Changing();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isExport <em>Export</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Export</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isExport()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Export();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isException <em>Exception</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Exception</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isException()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Exception();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isOptional <em>Optional</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Optional</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#isOptional()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EAttribute getListFieldMetaData_Optional();

	/**
	 * Returns the meta object for the reference '{@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getRecordMetaData <em>Record Meta Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Record Meta Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData#getRecordMetaData()
	 * @see #getListFieldMetaData()
	 * @generated
	 */
	EReference getListFieldMetaData_RecordMetaData();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.AbapException <em>Abap Exception</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Abap Exception</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.AbapException
	 * @generated
	 */
	EClass getAbapException();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.AbapException#getKey <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.AbapException#getKey()
	 * @see #getAbapException()
	 * @generated
	 */
	EAttribute getAbapException_Key();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.AbapException#getMessage <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Message</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.AbapException#getMessage()
	 * @see #getAbapException()
	 * @generated
	 */
	EAttribute getAbapException_Message();

	/**
	 * Returns the meta object for class '{@link java.util.Map.Entry <em>Respository Data Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Respository Data Entry</em>'.
	 * @see java.util.Map.Entry
	 * @model keyDataType="org.eclipse.emf.ecore.EString" keyRequired="true"
	 *        valueType="org.fusesource.camel.component.sap.model.rfc.FunctionTemplate" valueRequired="true"
	 * @generated
	 */
	EClass getRespositoryDataEntry();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see java.util.Map.Entry
	 * @see #getRespositoryDataEntry()
	 * @generated
	 */
	EAttribute getRespositoryDataEntry_Key();

	/**
	 * Returns the meta object for the reference '{@link java.util.Map.Entry <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Value</em>'.
	 * @see java.util.Map.Entry
	 * @see #getRespositoryDataEntry()
	 * @generated
	 */
	EReference getRespositoryDataEntry_Value();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData <em>Repository Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Repository Data</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryData
	 * @generated
	 */
	EClass getRepositoryData();

	/**
	 * Returns the meta object for the map '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData#getEntries <em>Entries</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Entries</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryData#getEntries()
	 * @see #getRepositoryData()
	 * @generated
	 */
	EReference getRepositoryData_Entries();

	/**
	 * Returns the meta object for the attribute '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryData#getFunctionTemplates <em>Function Templates</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Function Templates</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryData#getFunctionTemplates()
	 * @see #getRepositoryData()
	 * @generated
	 */
	EAttribute getRepositoryData_FunctionTemplates();

	/**
	 * Returns the meta object for class '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore <em>Repository Data Store</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Repository Data Store</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore
	 * @generated
	 */
	EClass getRepositoryDataStore();

	/**
	 * Returns the meta object for the map '{@link org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore#getEntries <em>Entries</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Entries</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore#getEntries()
	 * @see #getRepositoryDataStore()
	 * @generated
	 */
	EReference getRepositoryDataStore_Entries();

	/**
	 * Returns the meta object for class '{@link java.util.Map.Entry <em>Respository Data Store Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Respository Data Store Entry</em>'.
	 * @see java.util.Map.Entry
	 * @model keyDataType="org.eclipse.emf.ecore.EString" keyRequired="true"
	 *        valueType="org.fusesource.camel.component.sap.model.rfc.RepositoryData" valueRequired="true"
	 * @generated
	 */
	EClass getRespositoryDataStoreEntry();

	/**
	 * Returns the meta object for the attribute '{@link java.util.Map.Entry <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see java.util.Map.Entry
	 * @see #getRespositoryDataStoreEntry()
	 * @generated
	 */
	EAttribute getRespositoryDataStoreEntry_Key();

	/**
	 * Returns the meta object for the reference '{@link java.util.Map.Entry <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Value</em>'.
	 * @see java.util.Map.Entry
	 * @see #getRespositoryDataStoreEntry()
	 * @generated
	 */
	EReference getRespositoryDataStoreEntry_Value();

	/**
	 * Returns the meta object for enum '{@link org.fusesource.camel.component.sap.model.rfc.DataType <em>Data Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Data Type</em>'.
	 * @see org.fusesource.camel.component.sap.model.rfc.DataType
	 * @generated
	 */
	EEnum getDataType();

	/**
	 * Returns the meta object for data type '{@link java.util.List <em>Parameter List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Parameter List</em>'.
	 * @see java.util.List
	 * @model instanceClass="java.util.List<org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData>" serializeable="false"
	 * @generated
	 */
	EDataType getParameterList();

	/**
	 * Returns the meta object for data type '{@link java.util.List <em>Field List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Field List</em>'.
	 * @see java.util.List
	 * @model instanceClass="java.util.List<org.fusesource.camel.component.sap.model.rfc.FieldMetaData>" serializeable="false"
	 * @generated
	 */
	EDataType getFieldList();

	/**
	 * Returns the meta object for data type '{@link java.util.List <em>Abap Exception List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Abap Exception List</em>'.
	 * @see java.util.List
	 * @model instanceClass="java.util.List<org.fusesource.camel.component.sap.model.rfc.AbapException>" serializeable="false"
	 * @generated
	 */
	EDataType getAbapExceptionList();

	/**
	 * Returns the meta object for data type '{@link java.util.Map <em>Function Template Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Function Template Map</em>'.
	 * @see java.util.Map
	 * @model instanceClass="java.util.Map<java.lang.String, org.fusesource.camel.component.sap.model.rfc.FunctionTemplate>" serializeable="false"
	 * @generated
	 */
	EDataType getFunctionTemplateMap();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	RfcFactory getRfcFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationImpl <em>Destination</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestination()
		 * @generated
		 */
		EClass DESTINATION = eINSTANCE.getDestination();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION__NAME = eINSTANCE.getDestination_Name();

		/**
		 * The meta object literal for the '<em><b>Repository Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION__REPOSITORY_NAME = eINSTANCE.getDestination_RepositoryName();

		/**
		 * The meta object literal for the '<em><b>Rfcs</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DESTINATION__RFCS = eINSTANCE.getDestination_Rfcs();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RFCImpl <em>RFC</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RFCImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRFC()
		 * @generated
		 */
		EClass RFC = eINSTANCE.getRFC();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RFC__NAME = eINSTANCE.getRFC_Name();

		/**
		 * The meta object literal for the '<em><b>Group</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RFC__GROUP = eINSTANCE.getRFC_Group();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RFC__DESCRIPTION = eINSTANCE.getRFC_Description();

		/**
		 * The meta object literal for the '<em><b>Request</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RFC__REQUEST = eINSTANCE.getRFC_Request();

		/**
		 * The meta object literal for the '<em><b>Response</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RFC__RESPONSE = eINSTANCE.getRFC_Response();

		/**
		 * The meta object literal for the '<em><b>Destination</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RFC__DESTINATION = eINSTANCE.getRFC_Destination();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.TableImpl <em>Table</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.TableImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getTable()
		 * @generated
		 */
		EClass TABLE = eINSTANCE.getTable();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.StructureImpl <em>Structure</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.StructureImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getStructure()
		 * @generated
		 */
		EClass STRUCTURE = eINSTANCE.getStructure();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataEntryImpl <em>Destination Data Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataEntryImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationDataEntry()
		 * @generated
		 */
		EClass DESTINATION_DATA_ENTRY = eINSTANCE.getDestinationDataEntry();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA_ENTRY__KEY = eINSTANCE.getDestinationDataEntry_Key();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA_ENTRY__VALUE = eINSTANCE.getDestinationDataEntry_Value();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl <em>Destination Data</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationData()
		 * @generated
		 */
		EClass DESTINATION_DATA = eINSTANCE.getDestinationData();

		/**
		 * The meta object literal for the '<em><b>Entries</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DESTINATION_DATA__ENTRIES = eINSTANCE.getDestinationData_Entries();

		/**
		 * The meta object literal for the '<em><b>Alias User</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__ALIAS_USER = eINSTANCE.getDestinationData_AliasUser();

		/**
		 * The meta object literal for the '<em><b>Ashost</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__ASHOST = eINSTANCE.getDestinationData_Ashost();

		/**
		 * The meta object literal for the '<em><b>Auth Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__AUTH_TYPE = eINSTANCE.getDestinationData_AuthType();

		/**
		 * The meta object literal for the '<em><b>Client</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__CLIENT = eINSTANCE.getDestinationData_Client();

		/**
		 * The meta object literal for the '<em><b>Codepage</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__CODEPAGE = eINSTANCE.getDestinationData_Codepage();

		/**
		 * The meta object literal for the '<em><b>Cpic Trace</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__CPIC_TRACE = eINSTANCE.getDestinationData_CpicTrace();

		/**
		 * The meta object literal for the '<em><b>Deny Initial Password</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__DENY_INITIAL_PASSWORD = eINSTANCE.getDestinationData_DenyInitialPassword();

		/**
		 * The meta object literal for the '<em><b>Expiration Period</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__EXPIRATION_PERIOD = eINSTANCE.getDestinationData_ExpirationPeriod();

		/**
		 * The meta object literal for the '<em><b>Expiration Time</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__EXPIRATION_TIME = eINSTANCE.getDestinationData_ExpirationTime();

		/**
		 * The meta object literal for the '<em><b>Getsso2</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__GETSSO2 = eINSTANCE.getDestinationData_Getsso2();

		/**
		 * The meta object literal for the '<em><b>Group</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__GROUP = eINSTANCE.getDestinationData_Group();

		/**
		 * The meta object literal for the '<em><b>Gwhost</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__GWHOST = eINSTANCE.getDestinationData_Gwhost();

		/**
		 * The meta object literal for the '<em><b>Gwserv</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__GWSERV = eINSTANCE.getDestinationData_Gwserv();

		/**
		 * The meta object literal for the '<em><b>Lang</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__LANG = eINSTANCE.getDestinationData_Lang();

		/**
		 * The meta object literal for the '<em><b>Lcheck</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__LCHECK = eINSTANCE.getDestinationData_Lcheck();

		/**
		 * The meta object literal for the '<em><b>Max Get Time</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__MAX_GET_TIME = eINSTANCE.getDestinationData_MaxGetTime();

		/**
		 * The meta object literal for the '<em><b>Mshost</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__MSHOST = eINSTANCE.getDestinationData_Mshost();

		/**
		 * The meta object literal for the '<em><b>Msserv</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__MSSERV = eINSTANCE.getDestinationData_Msserv();

		/**
		 * The meta object literal for the '<em><b>Mysapsso2</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__MYSAPSSO2 = eINSTANCE.getDestinationData_Mysapsso2();

		/**
		 * The meta object literal for the '<em><b>Passwd</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__PASSWD = eINSTANCE.getDestinationData_Passwd();

		/**
		 * The meta object literal for the '<em><b>Password</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__PASSWORD = eINSTANCE.getDestinationData_Password();

		/**
		 * The meta object literal for the '<em><b>Pcs</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__PCS = eINSTANCE.getDestinationData_Pcs();

		/**
		 * The meta object literal for the '<em><b>Peak Limit</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__PEAK_LIMIT = eINSTANCE.getDestinationData_PeakLimit();

		/**
		 * The meta object literal for the '<em><b>Ping On Create</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__PING_ON_CREATE = eINSTANCE.getDestinationData_PingOnCreate();

		/**
		 * The meta object literal for the '<em><b>Pool Capacity</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__POOL_CAPACITY = eINSTANCE.getDestinationData_PoolCapacity();

		/**
		 * The meta object literal for the '<em><b>R3name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__R3NAME = eINSTANCE.getDestinationData_R3name();

		/**
		 * The meta object literal for the '<em><b>Repository Dest</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__REPOSITORY_DEST = eINSTANCE.getDestinationData_RepositoryDest();

		/**
		 * The meta object literal for the '<em><b>Repository Passwd</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__REPOSITORY_PASSWD = eINSTANCE.getDestinationData_RepositoryPasswd();

		/**
		 * The meta object literal for the '<em><b>Repository Roundtrip Optimization</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION = eINSTANCE.getDestinationData_RepositoryRoundtripOptimization();

		/**
		 * The meta object literal for the '<em><b>Repository Snc</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__REPOSITORY_SNC = eINSTANCE.getDestinationData_RepositorySnc();

		/**
		 * The meta object literal for the '<em><b>Repository User</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__REPOSITORY_USER = eINSTANCE.getDestinationData_RepositoryUser();

		/**
		 * The meta object literal for the '<em><b>Saprouter</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SAPROUTER = eINSTANCE.getDestinationData_Saprouter();

		/**
		 * The meta object literal for the '<em><b>Snc Library</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SNC_LIBRARY = eINSTANCE.getDestinationData_SncLibrary();

		/**
		 * The meta object literal for the '<em><b>Snc Mode</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SNC_MODE = eINSTANCE.getDestinationData_SncMode();

		/**
		 * The meta object literal for the '<em><b>Snc Myname</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SNC_MYNAME = eINSTANCE.getDestinationData_SncMyname();

		/**
		 * The meta object literal for the '<em><b>Snc Partnername</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SNC_PARTNERNAME = eINSTANCE.getDestinationData_SncPartnername();

		/**
		 * The meta object literal for the '<em><b>Snc Qop</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SNC_QOP = eINSTANCE.getDestinationData_SncQop();

		/**
		 * The meta object literal for the '<em><b>Sysnr</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__SYSNR = eINSTANCE.getDestinationData_Sysnr();

		/**
		 * The meta object literal for the '<em><b>Tphost</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__TPHOST = eINSTANCE.getDestinationData_Tphost();

		/**
		 * The meta object literal for the '<em><b>Tpname</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__TPNAME = eINSTANCE.getDestinationData_Tpname();

		/**
		 * The meta object literal for the '<em><b>Trace</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__TRACE = eINSTANCE.getDestinationData_Trace();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__TYPE = eINSTANCE.getDestinationData_Type();

		/**
		 * The meta object literal for the '<em><b>User Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__USER_NAME = eINSTANCE.getDestinationData_UserName();

		/**
		 * The meta object literal for the '<em><b>User</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__USER = eINSTANCE.getDestinationData_User();

		/**
		 * The meta object literal for the '<em><b>User Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__USER_ID = eINSTANCE.getDestinationData_UserId();

		/**
		 * The meta object literal for the '<em><b>Use Sapgui</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__USE_SAPGUI = eINSTANCE.getDestinationData_UseSapgui();

		/**
		 * The meta object literal for the '<em><b>X50 9cert</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA__X509CERT = eINSTANCE.getDestinationData_X509cert();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreEntryImpl <em>Destination Data Store Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreEntryImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationDataStoreEntry()
		 * @generated
		 */
		EClass DESTINATION_DATA_STORE_ENTRY = eINSTANCE.getDestinationDataStoreEntry();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DESTINATION_DATA_STORE_ENTRY__KEY = eINSTANCE.getDestinationDataStoreEntry_Key();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DESTINATION_DATA_STORE_ENTRY__VALUE = eINSTANCE.getDestinationDataStoreEntry_Value();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreImpl <em>Destination Data Store</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDestinationDataStore()
		 * @generated
		 */
		EClass DESTINATION_DATA_STORE = eINSTANCE.getDestinationDataStore();

		/**
		 * The meta object literal for the '<em><b>Entries</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DESTINATION_DATA_STORE__ENTRIES = eINSTANCE.getDestinationDataStore_Entries();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerImpl <em>Server</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServer()
		 * @generated
		 */
		EClass SERVER = eINSTANCE.getServer();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER__NAME = eINSTANCE.getServer_Name();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataEntryImpl <em>Server Data Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataEntryImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerDataEntry()
		 * @generated
		 */
		EClass SERVER_DATA_ENTRY = eINSTANCE.getServerDataEntry();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA_ENTRY__KEY = eINSTANCE.getServerDataEntry_Key();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA_ENTRY__VALUE = eINSTANCE.getServerDataEntry_Value();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl <em>Server Data</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerData()
		 * @generated
		 */
		EClass SERVER_DATA = eINSTANCE.getServerData();

		/**
		 * The meta object literal for the '<em><b>Entries</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVER_DATA__ENTRIES = eINSTANCE.getServerData_Entries();

		/**
		 * The meta object literal for the '<em><b>Gwhost</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__GWHOST = eINSTANCE.getServerData_Gwhost();

		/**
		 * The meta object literal for the '<em><b>Gwserv</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__GWSERV = eINSTANCE.getServerData_Gwserv();

		/**
		 * The meta object literal for the '<em><b>Progid</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__PROGID = eINSTANCE.getServerData_Progid();

		/**
		 * The meta object literal for the '<em><b>Connection Count</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__CONNECTION_COUNT = eINSTANCE.getServerData_ConnectionCount();

		/**
		 * The meta object literal for the '<em><b>Saprouter</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__SAPROUTER = eINSTANCE.getServerData_Saprouter();

		/**
		 * The meta object literal for the '<em><b>Max Start Up Delay</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__MAX_START_UP_DELAY = eINSTANCE.getServerData_MaxStartUpDelay();

		/**
		 * The meta object literal for the '<em><b>Repository Destination</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__REPOSITORY_DESTINATION = eINSTANCE.getServerData_RepositoryDestination();

		/**
		 * The meta object literal for the '<em><b>Repository Map</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__REPOSITORY_MAP = eINSTANCE.getServerData_RepositoryMap();

		/**
		 * The meta object literal for the '<em><b>Trace</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__TRACE = eINSTANCE.getServerData_Trace();

		/**
		 * The meta object literal for the '<em><b>Worker Thread Count</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__WORKER_THREAD_COUNT = eINSTANCE.getServerData_WorkerThreadCount();

		/**
		 * The meta object literal for the '<em><b>Worker Thread Min Count</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__WORKER_THREAD_MIN_COUNT = eINSTANCE.getServerData_WorkerThreadMinCount();

		/**
		 * The meta object literal for the '<em><b>Snc Mode</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__SNC_MODE = eINSTANCE.getServerData_SncMode();

		/**
		 * The meta object literal for the '<em><b>Snc Qop</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__SNC_QOP = eINSTANCE.getServerData_SncQop();

		/**
		 * The meta object literal for the '<em><b>Snc Myname</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__SNC_MYNAME = eINSTANCE.getServerData_SncMyname();

		/**
		 * The meta object literal for the '<em><b>Snc Lib</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA__SNC_LIB = eINSTANCE.getServerData_SncLib();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreEntryImpl <em>Server Data Store Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreEntryImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerDataStoreEntry()
		 * @generated
		 */
		EClass SERVER_DATA_STORE_ENTRY = eINSTANCE.getServerDataStoreEntry();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SERVER_DATA_STORE_ENTRY__KEY = eINSTANCE.getServerDataStoreEntry_Key();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVER_DATA_STORE_ENTRY__VALUE = eINSTANCE.getServerDataStoreEntry_Value();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreImpl <em>Server Data Store</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getServerDataStore()
		 * @generated
		 */
		EClass SERVER_DATA_STORE = eINSTANCE.getServerDataStore();

		/**
		 * The meta object literal for the '<em><b>Entries</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SERVER_DATA_STORE__ENTRIES = eINSTANCE.getServerDataStore_Entries();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl <em>Function Template</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFunctionTemplate()
		 * @generated
		 */
		EClass FUNCTION_TEMPLATE = eINSTANCE.getFunctionTemplate();

		/**
		 * The meta object literal for the '<em><b>Imports</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FUNCTION_TEMPLATE__IMPORTS = eINSTANCE.getFunctionTemplate_Imports();

		/**
		 * The meta object literal for the '<em><b>Exports</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FUNCTION_TEMPLATE__EXPORTS = eINSTANCE.getFunctionTemplate_Exports();

		/**
		 * The meta object literal for the '<em><b>Changing</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FUNCTION_TEMPLATE__CHANGING = eINSTANCE.getFunctionTemplate_Changing();

		/**
		 * The meta object literal for the '<em><b>Tables</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FUNCTION_TEMPLATE__TABLES = eINSTANCE.getFunctionTemplate_Tables();

		/**
		 * The meta object literal for the '<em><b>Exceptions</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FUNCTION_TEMPLATE__EXCEPTIONS = eINSTANCE.getFunctionTemplate_Exceptions();

		/**
		 * The meta object literal for the '<em><b>Import Parameter List</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST = eINSTANCE.getFunctionTemplate_ImportParameterList();

		/**
		 * The meta object literal for the '<em><b>Export Parameter List</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST = eINSTANCE.getFunctionTemplate_ExportParameterList();

		/**
		 * The meta object literal for the '<em><b>Changing Parameter List</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST = eINSTANCE.getFunctionTemplate_ChangingParameterList();

		/**
		 * The meta object literal for the '<em><b>Table Parameter List</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST = eINSTANCE.getFunctionTemplate_TableParameterList();

		/**
		 * The meta object literal for the '<em><b>Exception List</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FUNCTION_TEMPLATE__EXCEPTION_LIST = eINSTANCE.getFunctionTemplate_ExceptionList();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl <em>Record Meta Data</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRecordMetaData()
		 * @generated
		 */
		EClass RECORD_META_DATA = eINSTANCE.getRecordMetaData();

		/**
		 * The meta object literal for the '<em><b>Field Meta Data</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RECORD_META_DATA__FIELD_META_DATA = eINSTANCE.getRecordMetaData_FieldMetaData();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RECORD_META_DATA__NAME = eINSTANCE.getRecordMetaData_Name();

		/**
		 * The meta object literal for the '<em><b>Record Field Meta Data</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RECORD_META_DATA__RECORD_FIELD_META_DATA = eINSTANCE.getRecordMetaData_RecordFieldMetaData();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl <em>Field Meta Data</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFieldMetaData()
		 * @generated
		 */
		EClass FIELD_META_DATA = eINSTANCE.getFieldMetaData();

		/**
		 * The meta object literal for the '<em><b>Field Meta Data</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FIELD_META_DATA__FIELD_META_DATA = eINSTANCE.getFieldMetaData_FieldMetaData();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__NAME = eINSTANCE.getFieldMetaData_Name();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__TYPE = eINSTANCE.getFieldMetaData_Type();

		/**
		 * The meta object literal for the '<em><b>Byte Length</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__BYTE_LENGTH = eINSTANCE.getFieldMetaData_ByteLength();

		/**
		 * The meta object literal for the '<em><b>Byte Offset</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__BYTE_OFFSET = eINSTANCE.getFieldMetaData_ByteOffset();

		/**
		 * The meta object literal for the '<em><b>Unicode Byte Length</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__UNICODE_BYTE_LENGTH = eINSTANCE.getFieldMetaData_UnicodeByteLength();

		/**
		 * The meta object literal for the '<em><b>Unicode Byte Offset</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__UNICODE_BYTE_OFFSET = eINSTANCE.getFieldMetaData_UnicodeByteOffset();

		/**
		 * The meta object literal for the '<em><b>Decimals</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__DECIMALS = eINSTANCE.getFieldMetaData_Decimals();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FIELD_META_DATA__DESCRIPTION = eINSTANCE.getFieldMetaData_Description();

		/**
		 * The meta object literal for the '<em><b>Record Meta Data</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FIELD_META_DATA__RECORD_META_DATA = eINSTANCE.getFieldMetaData_RecordMetaData();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.ListFieldMetaDataImpl <em>List Field Meta Data</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.ListFieldMetaDataImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getListFieldMetaData()
		 * @generated
		 */
		EClass LIST_FIELD_META_DATA = eINSTANCE.getListFieldMetaData();

		/**
		 * The meta object literal for the '<em><b>Field Meta Data</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LIST_FIELD_META_DATA__FIELD_META_DATA = eINSTANCE.getListFieldMetaData_FieldMetaData();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__NAME = eINSTANCE.getListFieldMetaData_Name();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__TYPE = eINSTANCE.getListFieldMetaData_Type();

		/**
		 * The meta object literal for the '<em><b>Byte Length</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__BYTE_LENGTH = eINSTANCE.getListFieldMetaData_ByteLength();

		/**
		 * The meta object literal for the '<em><b>Unicode Byte Length</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__UNICODE_BYTE_LENGTH = eINSTANCE.getListFieldMetaData_UnicodeByteLength();

		/**
		 * The meta object literal for the '<em><b>Decimals</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__DECIMALS = eINSTANCE.getListFieldMetaData_Decimals();

		/**
		 * The meta object literal for the '<em><b>Defaults</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__DEFAULTS = eINSTANCE.getListFieldMetaData_Defaults();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__DESCRIPTION = eINSTANCE.getListFieldMetaData_Description();

		/**
		 * The meta object literal for the '<em><b>Import</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__IMPORT = eINSTANCE.getListFieldMetaData_Import();

		/**
		 * The meta object literal for the '<em><b>Changing</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__CHANGING = eINSTANCE.getListFieldMetaData_Changing();

		/**
		 * The meta object literal for the '<em><b>Export</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__EXPORT = eINSTANCE.getListFieldMetaData_Export();

		/**
		 * The meta object literal for the '<em><b>Exception</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__EXCEPTION = eINSTANCE.getListFieldMetaData_Exception();

		/**
		 * The meta object literal for the '<em><b>Optional</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LIST_FIELD_META_DATA__OPTIONAL = eINSTANCE.getListFieldMetaData_Optional();

		/**
		 * The meta object literal for the '<em><b>Record Meta Data</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LIST_FIELD_META_DATA__RECORD_META_DATA = eINSTANCE.getListFieldMetaData_RecordMetaData();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.AbapExceptionImpl <em>Abap Exception</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.AbapExceptionImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getAbapException()
		 * @generated
		 */
		EClass ABAP_EXCEPTION = eINSTANCE.getAbapException();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ABAP_EXCEPTION__KEY = eINSTANCE.getAbapException_Key();

		/**
		 * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ABAP_EXCEPTION__MESSAGE = eINSTANCE.getAbapException_Message();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataEntryImpl <em>Respository Data Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataEntryImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRespositoryDataEntry()
		 * @generated
		 */
		EClass RESPOSITORY_DATA_ENTRY = eINSTANCE.getRespositoryDataEntry();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RESPOSITORY_DATA_ENTRY__KEY = eINSTANCE.getRespositoryDataEntry_Key();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RESPOSITORY_DATA_ENTRY__VALUE = eINSTANCE.getRespositoryDataEntry_Value();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataImpl <em>Repository Data</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRepositoryData()
		 * @generated
		 */
		EClass REPOSITORY_DATA = eINSTANCE.getRepositoryData();

		/**
		 * The meta object literal for the '<em><b>Entries</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REPOSITORY_DATA__ENTRIES = eINSTANCE.getRepositoryData_Entries();

		/**
		 * The meta object literal for the '<em><b>Function Templates</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REPOSITORY_DATA__FUNCTION_TEMPLATES = eINSTANCE.getRepositoryData_FunctionTemplates();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataStoreImpl <em>Repository Data Store</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataStoreImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRepositoryDataStore()
		 * @generated
		 */
		EClass REPOSITORY_DATA_STORE = eINSTANCE.getRepositoryDataStore();

		/**
		 * The meta object literal for the '<em><b>Entries</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REPOSITORY_DATA_STORE__ENTRIES = eINSTANCE.getRepositoryDataStore_Entries();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataStoreEntryImpl <em>Respository Data Store Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RespositoryDataStoreEntryImpl
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getRespositoryDataStoreEntry()
		 * @generated
		 */
		EClass RESPOSITORY_DATA_STORE_ENTRY = eINSTANCE.getRespositoryDataStoreEntry();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RESPOSITORY_DATA_STORE_ENTRY__KEY = eINSTANCE.getRespositoryDataStoreEntry_Key();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RESPOSITORY_DATA_STORE_ENTRY__VALUE = eINSTANCE.getRespositoryDataStoreEntry_Value();

		/**
		 * The meta object literal for the '{@link org.fusesource.camel.component.sap.model.rfc.DataType <em>Data Type</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.fusesource.camel.component.sap.model.rfc.DataType
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getDataType()
		 * @generated
		 */
		EEnum DATA_TYPE = eINSTANCE.getDataType();

		/**
		 * The meta object literal for the '<em>Parameter List</em>' data type.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see java.util.List
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getParameterList()
		 * @generated
		 */
		EDataType PARAMETER_LIST = eINSTANCE.getParameterList();

		/**
		 * The meta object literal for the '<em>Field List</em>' data type.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see java.util.List
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFieldList()
		 * @generated
		 */
		EDataType FIELD_LIST = eINSTANCE.getFieldList();

		/**
		 * The meta object literal for the '<em>Abap Exception List</em>' data type.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see java.util.List
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getAbapExceptionList()
		 * @generated
		 */
		EDataType ABAP_EXCEPTION_LIST = eINSTANCE.getAbapExceptionList();

		/**
		 * The meta object literal for the '<em>Function Template Map</em>' data type.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see java.util.Map
		 * @see org.fusesource.camel.component.sap.model.rfc.impl.RfcPackageImpl#getFunctionTemplateMap()
		 * @generated
		 */
		EDataType FUNCTION_TEMPLATE_MAP = eINSTANCE.getFunctionTemplateMap();

	}

} //RfcPackage
