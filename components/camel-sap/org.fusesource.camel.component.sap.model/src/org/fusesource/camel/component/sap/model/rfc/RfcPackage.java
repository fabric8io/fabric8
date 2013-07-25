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

	}

} //RfcPackage
