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
package org.fusesource.camel.component.sap.model.rfc.impl;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EMap;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EcoreEMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Destination Data</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getEntries <em>Entries</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getAliasUser <em>Alias User</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getAshost <em>Ashost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getAuthType <em>Auth Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getClient <em>Client</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getCodepage <em>Codepage</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getCpicTrace <em>Cpic Trace</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getDenyInitialPassword <em>Deny Initial Password</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getExpirationPeriod <em>Expiration Period</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getExpirationTime <em>Expiration Time</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getGetsso2 <em>Getsso2</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getGroup <em>Group</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getGwhost <em>Gwhost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getGwserv <em>Gwserv</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getLang <em>Lang</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getLcheck <em>Lcheck</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getMaxGetTime <em>Max Get Time</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getMshost <em>Mshost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getMsserv <em>Msserv</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getMysapsso2 <em>Mysapsso2</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getPasswd <em>Passwd</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getPassword <em>Password</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getPcs <em>Pcs</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getPeakLimit <em>Peak Limit</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getPingOnCreate <em>Ping On Create</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getPoolCapacity <em>Pool Capacity</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getR3name <em>R3name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getRepositoryDest <em>Repository Dest</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getRepositoryPasswd <em>Repository Passwd</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getRepositoryRoundtripOptimization <em>Repository Roundtrip Optimization</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getRepositorySnc <em>Repository Snc</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getRepositoryUser <em>Repository User</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSaprouter <em>Saprouter</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSncLibrary <em>Snc Library</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSncMode <em>Snc Mode</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSncMyname <em>Snc Myname</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSncPartnername <em>Snc Partnername</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSncQop <em>Snc Qop</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getSysnr <em>Sysnr</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getTphost <em>Tphost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getTpname <em>Tpname</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getTrace <em>Trace</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getType <em>Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getUserName <em>User Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getUser <em>User</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getUserId <em>User Id</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getUseSapgui <em>Use Sapgui</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl#getX509cert <em>X50 9cert</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DestinationDataImpl extends EObjectImpl implements DestinationData {
	/**
	 * The cached value of the '{@link #getEntries() <em>Entries</em>}' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEntries()
	 * @generated
	 * @ordered
	 */
	protected EMap<String, String> entries;

	/**
	 * The default value of the '{@link #getAliasUser() <em>Alias User</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAliasUser()
	 * @generated
	 * @ordered
	 */
	protected static final String ALIAS_USER_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getAshost() <em>Ashost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAshost()
	 * @generated
	 * @ordered
	 */
	protected static final String ASHOST_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getAuthType() <em>Auth Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAuthType()
	 * @generated
	 * @ordered
	 */
	protected static final String AUTH_TYPE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getClient() <em>Client</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClient()
	 * @generated
	 * @ordered
	 */
	protected static final String CLIENT_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getCodepage() <em>Codepage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCodepage()
	 * @generated
	 * @ordered
	 */
	protected static final String CODEPAGE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getCpicTrace() <em>Cpic Trace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCpicTrace()
	 * @generated
	 * @ordered
	 */
	protected static final String CPIC_TRACE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getDenyInitialPassword() <em>Deny Initial Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDenyInitialPassword()
	 * @generated
	 * @ordered
	 */
	protected static final String DENY_INITIAL_PASSWORD_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getExpirationPeriod() <em>Expiration Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpirationPeriod()
	 * @generated
	 * @ordered
	 */
	protected static final String EXPIRATION_PERIOD_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getExpirationTime() <em>Expiration Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpirationTime()
	 * @generated
	 * @ordered
	 */
	protected static final String EXPIRATION_TIME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getGetsso2() <em>Getsso2</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGetsso2()
	 * @generated
	 * @ordered
	 */
	protected static final String GETSSO2_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getGroup() <em>Group</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGroup()
	 * @generated
	 * @ordered
	 */
	protected static final String GROUP_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getGwhost() <em>Gwhost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGwhost()
	 * @generated
	 * @ordered
	 */
	protected static final String GWHOST_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getGwserv() <em>Gwserv</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGwserv()
	 * @generated
	 * @ordered
	 */
	protected static final String GWSERV_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getLang() <em>Lang</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLang()
	 * @generated
	 * @ordered
	 */
	protected static final String LANG_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getLcheck() <em>Lcheck</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLcheck()
	 * @generated
	 * @ordered
	 */
	protected static final String LCHECK_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getMaxGetTime() <em>Max Get Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxGetTime()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_GET_TIME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getMshost() <em>Mshost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMshost()
	 * @generated
	 * @ordered
	 */
	protected static final String MSHOST_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getMsserv() <em>Msserv</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMsserv()
	 * @generated
	 * @ordered
	 */
	protected static final String MSSERV_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getMysapsso2() <em>Mysapsso2</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMysapsso2()
	 * @generated
	 * @ordered
	 */
	protected static final String MYSAPSSO2_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getPasswd() <em>Passwd</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPasswd()
	 * @generated
	 * @ordered
	 */
	protected static final String PASSWD_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getPassword() <em>Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPassword()
	 * @generated
	 * @ordered
	 */
	protected static final String PASSWORD_EDEFAULT = null;

	/**
	 * The default value of the '{@link #getPcs() <em>Pcs</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPcs()
	 * @generated
	 * @ordered
	 */
	protected static final String PCS_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getPeakLimit() <em>Peak Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPeakLimit()
	 * @generated
	 * @ordered
	 */
	protected static final String PEAK_LIMIT_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getPingOnCreate() <em>Ping On Create</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPingOnCreate()
	 * @generated
	 * @ordered
	 */
	protected static final String PING_ON_CREATE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getPoolCapacity() <em>Pool Capacity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPoolCapacity()
	 * @generated
	 * @ordered
	 */
	protected static final String POOL_CAPACITY_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getR3name() <em>R3name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getR3name()
	 * @generated
	 * @ordered
	 */
	protected static final String R3NAME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositoryDest() <em>Repository Dest</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryDest()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_DEST_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositoryPasswd() <em>Repository Passwd</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryPasswd()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_PASSWD_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositoryRoundtripOptimization() <em>Repository Roundtrip Optimization</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryRoundtripOptimization()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_ROUNDTRIP_OPTIMIZATION_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositorySnc() <em>Repository Snc</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositorySnc()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_SNC_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositoryUser() <em>Repository User</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryUser()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_USER_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSaprouter() <em>Saprouter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSaprouter()
	 * @generated
	 * @ordered
	 */
	protected static final String SAPROUTER_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSncLibrary() <em>Snc Library</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncLibrary()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_LIBRARY_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSncMode() <em>Snc Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncMode()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_MODE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSncMyname() <em>Snc Myname</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncMyname()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_MYNAME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSncPartnername() <em>Snc Partnername</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncPartnername()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_PARTNERNAME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSncQop() <em>Snc Qop</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncQop()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_QOP_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getSysnr() <em>Sysnr</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSysnr()
	 * @generated
	 * @ordered
	 */
	protected static final String SYSNR_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getTphost() <em>Tphost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTphost()
	 * @generated
	 * @ordered
	 */
	protected static final String TPHOST_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getTpname() <em>Tpname</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTpname()
	 * @generated
	 * @ordered
	 */
	protected static final String TPNAME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getTrace() <em>Trace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTrace()
	 * @generated
	 * @ordered
	 */
	protected static final String TRACE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final String TYPE_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getUserName() <em>User Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUserName()
	 * @generated
	 * @ordered
	 */
	protected static final String USER_NAME_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getUser() <em>User</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUser()
	 * @generated
	 * @ordered
	 */
	protected static final String USER_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getUserId() <em>User Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUserId()
	 * @generated
	 * @ordered
	 */
	protected static final String USER_ID_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getUseSapgui() <em>Use Sapgui</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUseSapgui()
	 * @generated
	 * @ordered
	 */
	protected static final String USE_SAPGUI_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getX509cert() <em>X50 9cert</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getX509cert()
	 * @generated
	 * @ordered
	 */
	protected static final String X509CERT_EDEFAULT = "";

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DestinationDataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return RfcPackage.Literals.DESTINATION_DATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EMap<String, String> getEntries() {
		if (entries == null) {
			entries = new EcoreEMap<String,String>(RfcPackage.Literals.DESTINATION_DATA_ENTRY, DestinationDataEntryImpl.class, this, RfcPackage.DESTINATION_DATA__ENTRIES);
		}
		return entries;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getAliasUser() {
		// TODO: implement this method to return the 'Alias User' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setAliasUser(String newAliasUser) {
		// TODO: implement this method to set the 'Alias User' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getAshost() {
		// TODO: implement this method to return the 'Ashost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setAshost(String newAshost) {
		// TODO: implement this method to set the 'Ashost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getAuthType() {
		// TODO: implement this method to return the 'Auth Type' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setAuthType(String newAuthType) {
		// TODO: implement this method to set the 'Auth Type' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getClient() {
		// TODO: implement this method to return the 'Client' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setClient(String newClient) {
		// TODO: implement this method to set the 'Client' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getCodepage() {
		// TODO: implement this method to return the 'Codepage' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setCodepage(String newCodepage) {
		// TODO: implement this method to set the 'Codepage' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getCpicTrace() {
		// TODO: implement this method to return the 'Cpic Trace' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setCpicTrace(String newCpicTrace) {
		// TODO: implement this method to set the 'Cpic Trace' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDenyInitialPassword() {
		// TODO: implement this method to return the 'Deny Initial Password' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDenyInitialPassword(String newDenyInitialPassword) {
		// TODO: implement this method to set the 'Deny Initial Password' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getExpirationPeriod() {
		// TODO: implement this method to return the 'Expiration Period' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExpirationPeriod(String newExpirationPeriod) {
		// TODO: implement this method to set the 'Expiration Period' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getExpirationTime() {
		// TODO: implement this method to return the 'Expiration Time' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExpirationTime(String newExpirationTime) {
		// TODO: implement this method to set the 'Expiration Time' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getGetsso2() {
		// TODO: implement this method to return the 'Getsso2' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setGetsso2(String newGetsso2) {
		// TODO: implement this method to set the 'Getsso2' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getGroup() {
		// TODO: implement this method to return the 'Group' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setGroup(String newGroup) {
		// TODO: implement this method to set the 'Group' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getGwhost() {
		// TODO: implement this method to return the 'Gwhost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setGwhost(String newGwhost) {
		// TODO: implement this method to set the 'Gwhost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getGwserv() {
		// TODO: implement this method to return the 'Gwserv' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setGwserv(String newGwserv) {
		// TODO: implement this method to set the 'Gwserv' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLang() {
		// TODO: implement this method to return the 'Lang' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLang(String newLang) {
		// TODO: implement this method to set the 'Lang' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLcheck() {
		// TODO: implement this method to return the 'Lcheck' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLcheck(String newLcheck) {
		// TODO: implement this method to set the 'Lcheck' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxGetTime() {
		// TODO: implement this method to return the 'Max Get Time' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxGetTime(String newMaxGetTime) {
		// TODO: implement this method to set the 'Max Get Time' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMshost() {
		// TODO: implement this method to return the 'Mshost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMshost(String newMshost) {
		// TODO: implement this method to set the 'Mshost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMsserv() {
		// TODO: implement this method to return the 'Msserv' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMsserv(String newMsserv) {
		// TODO: implement this method to set the 'Msserv' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMysapsso2() {
		// TODO: implement this method to return the 'Mysapsso2' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMysapsso2(String newMysapsso2) {
		// TODO: implement this method to set the 'Mysapsso2' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPasswd() {
		// TODO: implement this method to return the 'Passwd' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPasswd(String newPasswd) {
		// TODO: implement this method to set the 'Passwd' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPassword() {
		// TODO: implement this method to return the 'Password' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPassword(String newPassword) {
		// TODO: implement this method to set the 'Password' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPcs() {
		// TODO: implement this method to return the 'Pcs' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPcs(String newPcs) {
		// TODO: implement this method to set the 'Pcs' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPeakLimit() {
		// TODO: implement this method to return the 'Peak Limit' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPeakLimit(String newPeakLimit) {
		// TODO: implement this method to set the 'Peak Limit' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPingOnCreate() {
		// TODO: implement this method to return the 'Ping On Create' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPingOnCreate(String newPingOnCreate) {
		// TODO: implement this method to set the 'Ping On Create' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPoolCapacity() {
		// TODO: implement this method to return the 'Pool Capacity' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPoolCapacity(String newPoolCapacity) {
		// TODO: implement this method to set the 'Pool Capacity' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getR3name() {
		// TODO: implement this method to return the 'R3name' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setR3name(String newR3name) {
		// TODO: implement this method to set the 'R3name' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositoryDest() {
		// TODO: implement this method to return the 'Repository Dest' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositoryDest(String newRepositoryDest) {
		// TODO: implement this method to set the 'Repository Dest' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositoryPasswd() {
		// TODO: implement this method to return the 'Repository Passwd' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositoryPasswd(String newRepositoryPasswd) {
		// TODO: implement this method to set the 'Repository Passwd' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositoryRoundtripOptimization() {
		// TODO: implement this method to return the 'Repository Roundtrip Optimization' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositoryRoundtripOptimization(String newRepositoryRoundtripOptimization) {
		// TODO: implement this method to set the 'Repository Roundtrip Optimization' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositorySnc() {
		// TODO: implement this method to return the 'Repository Snc' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositorySnc(String newRepositorySnc) {
		// TODO: implement this method to set the 'Repository Snc' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositoryUser() {
		// TODO: implement this method to return the 'Repository User' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositoryUser(String newRepositoryUser) {
		// TODO: implement this method to set the 'Repository User' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSaprouter() {
		// TODO: implement this method to return the 'Saprouter' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSaprouter(String newSaprouter) {
		// TODO: implement this method to set the 'Saprouter' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSncLibrary() {
		// TODO: implement this method to return the 'Snc Library' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSncLibrary(String newSncLibrary) {
		// TODO: implement this method to set the 'Snc Library' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSncMode() {
		// TODO: implement this method to return the 'Snc Mode' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSncMode(String newSncMode) {
		// TODO: implement this method to set the 'Snc Mode' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSncMyname() {
		// TODO: implement this method to return the 'Snc Myname' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSncMyname(String newSncMyname) {
		// TODO: implement this method to set the 'Snc Myname' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSncPartnername() {
		// TODO: implement this method to return the 'Snc Partnername' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSncPartnername(String newSncPartnername) {
		// TODO: implement this method to set the 'Snc Partnername' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSncQop() {
		// TODO: implement this method to return the 'Snc Qop' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSncQop(String newSncQop) {
		// TODO: implement this method to set the 'Snc Qop' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSysnr() {
		// TODO: implement this method to return the 'Sysnr' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSysnr(String newSysnr) {
		// TODO: implement this method to set the 'Sysnr' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getTphost() {
		// TODO: implement this method to return the 'Tphost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTphost(String newTphost) {
		// TODO: implement this method to set the 'Tphost' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getTpname() {
		// TODO: implement this method to return the 'Tpname' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTpname(String newTpname) {
		// TODO: implement this method to set the 'Tpname' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getTrace() {
		// TODO: implement this method to return the 'Trace' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTrace(String newTrace) {
		// TODO: implement this method to set the 'Trace' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getType() {
		// TODO: implement this method to return the 'Type' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setType(String newType) {
		// TODO: implement this method to set the 'Type' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getUserName() {
		// TODO: implement this method to return the 'User Name' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUserName(String newUserName) {
		// TODO: implement this method to set the 'User Name' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getUser() {
		// TODO: implement this method to return the 'User' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUser(String newUser) {
		// TODO: implement this method to set the 'User' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getUserId() {
		// TODO: implement this method to return the 'User Id' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUserId(String newUserId) {
		// TODO: implement this method to set the 'User Id' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getUseSapgui() {
		// TODO: implement this method to return the 'Use Sapgui' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUseSapgui(String newUseSapgui) {
		// TODO: implement this method to set the 'Use Sapgui' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getX509cert() {
		// TODO: implement this method to return the 'X50 9cert' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setX509cert(String newX509cert) {
		// TODO: implement this method to set the 'X50 9cert' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case RfcPackage.DESTINATION_DATA__ENTRIES:
				return ((InternalEList<?>)getEntries()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case RfcPackage.DESTINATION_DATA__ENTRIES:
				if (coreType) return getEntries();
				else return getEntries().map();
			case RfcPackage.DESTINATION_DATA__ALIAS_USER:
				return getAliasUser();
			case RfcPackage.DESTINATION_DATA__ASHOST:
				return getAshost();
			case RfcPackage.DESTINATION_DATA__AUTH_TYPE:
				return getAuthType();
			case RfcPackage.DESTINATION_DATA__CLIENT:
				return getClient();
			case RfcPackage.DESTINATION_DATA__CODEPAGE:
				return getCodepage();
			case RfcPackage.DESTINATION_DATA__CPIC_TRACE:
				return getCpicTrace();
			case RfcPackage.DESTINATION_DATA__DENY_INITIAL_PASSWORD:
				return getDenyInitialPassword();
			case RfcPackage.DESTINATION_DATA__EXPIRATION_PERIOD:
				return getExpirationPeriod();
			case RfcPackage.DESTINATION_DATA__EXPIRATION_TIME:
				return getExpirationTime();
			case RfcPackage.DESTINATION_DATA__GETSSO2:
				return getGetsso2();
			case RfcPackage.DESTINATION_DATA__GROUP:
				return getGroup();
			case RfcPackage.DESTINATION_DATA__GWHOST:
				return getGwhost();
			case RfcPackage.DESTINATION_DATA__GWSERV:
				return getGwserv();
			case RfcPackage.DESTINATION_DATA__LANG:
				return getLang();
			case RfcPackage.DESTINATION_DATA__LCHECK:
				return getLcheck();
			case RfcPackage.DESTINATION_DATA__MAX_GET_TIME:
				return getMaxGetTime();
			case RfcPackage.DESTINATION_DATA__MSHOST:
				return getMshost();
			case RfcPackage.DESTINATION_DATA__MSSERV:
				return getMsserv();
			case RfcPackage.DESTINATION_DATA__MYSAPSSO2:
				return getMysapsso2();
			case RfcPackage.DESTINATION_DATA__PASSWD:
				return getPasswd();
			case RfcPackage.DESTINATION_DATA__PASSWORD:
				return getPassword();
			case RfcPackage.DESTINATION_DATA__PCS:
				return getPcs();
			case RfcPackage.DESTINATION_DATA__PEAK_LIMIT:
				return getPeakLimit();
			case RfcPackage.DESTINATION_DATA__PING_ON_CREATE:
				return getPingOnCreate();
			case RfcPackage.DESTINATION_DATA__POOL_CAPACITY:
				return getPoolCapacity();
			case RfcPackage.DESTINATION_DATA__R3NAME:
				return getR3name();
			case RfcPackage.DESTINATION_DATA__REPOSITORY_DEST:
				return getRepositoryDest();
			case RfcPackage.DESTINATION_DATA__REPOSITORY_PASSWD:
				return getRepositoryPasswd();
			case RfcPackage.DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION:
				return getRepositoryRoundtripOptimization();
			case RfcPackage.DESTINATION_DATA__REPOSITORY_SNC:
				return getRepositorySnc();
			case RfcPackage.DESTINATION_DATA__REPOSITORY_USER:
				return getRepositoryUser();
			case RfcPackage.DESTINATION_DATA__SAPROUTER:
				return getSaprouter();
			case RfcPackage.DESTINATION_DATA__SNC_LIBRARY:
				return getSncLibrary();
			case RfcPackage.DESTINATION_DATA__SNC_MODE:
				return getSncMode();
			case RfcPackage.DESTINATION_DATA__SNC_MYNAME:
				return getSncMyname();
			case RfcPackage.DESTINATION_DATA__SNC_PARTNERNAME:
				return getSncPartnername();
			case RfcPackage.DESTINATION_DATA__SNC_QOP:
				return getSncQop();
			case RfcPackage.DESTINATION_DATA__SYSNR:
				return getSysnr();
			case RfcPackage.DESTINATION_DATA__TPHOST:
				return getTphost();
			case RfcPackage.DESTINATION_DATA__TPNAME:
				return getTpname();
			case RfcPackage.DESTINATION_DATA__TRACE:
				return getTrace();
			case RfcPackage.DESTINATION_DATA__TYPE:
				return getType();
			case RfcPackage.DESTINATION_DATA__USER_NAME:
				return getUserName();
			case RfcPackage.DESTINATION_DATA__USER:
				return getUser();
			case RfcPackage.DESTINATION_DATA__USER_ID:
				return getUserId();
			case RfcPackage.DESTINATION_DATA__USE_SAPGUI:
				return getUseSapgui();
			case RfcPackage.DESTINATION_DATA__X509CERT:
				return getX509cert();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case RfcPackage.DESTINATION_DATA__ENTRIES:
				((EStructuralFeature.Setting)getEntries()).set(newValue);
				return;
			case RfcPackage.DESTINATION_DATA__ALIAS_USER:
				setAliasUser((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__ASHOST:
				setAshost((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__AUTH_TYPE:
				setAuthType((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__CLIENT:
				setClient((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__CODEPAGE:
				setCodepage((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__CPIC_TRACE:
				setCpicTrace((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__DENY_INITIAL_PASSWORD:
				setDenyInitialPassword((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__EXPIRATION_PERIOD:
				setExpirationPeriod((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__EXPIRATION_TIME:
				setExpirationTime((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__GETSSO2:
				setGetsso2((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__GROUP:
				setGroup((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__GWHOST:
				setGwhost((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__GWSERV:
				setGwserv((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__LANG:
				setLang((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__LCHECK:
				setLcheck((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__MAX_GET_TIME:
				setMaxGetTime((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__MSHOST:
				setMshost((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__MSSERV:
				setMsserv((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__MYSAPSSO2:
				setMysapsso2((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__PASSWD:
				setPasswd((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__PASSWORD:
				setPassword((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__PCS:
				setPcs((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__PEAK_LIMIT:
				setPeakLimit((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__PING_ON_CREATE:
				setPingOnCreate((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__POOL_CAPACITY:
				setPoolCapacity((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__R3NAME:
				setR3name((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_DEST:
				setRepositoryDest((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_PASSWD:
				setRepositoryPasswd((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION:
				setRepositoryRoundtripOptimization((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_SNC:
				setRepositorySnc((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_USER:
				setRepositoryUser((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SAPROUTER:
				setSaprouter((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_LIBRARY:
				setSncLibrary((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_MODE:
				setSncMode((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_MYNAME:
				setSncMyname((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_PARTNERNAME:
				setSncPartnername((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_QOP:
				setSncQop((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__SYSNR:
				setSysnr((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__TPHOST:
				setTphost((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__TPNAME:
				setTpname((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__TRACE:
				setTrace((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__TYPE:
				setType((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__USER_NAME:
				setUserName((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__USER:
				setUser((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__USER_ID:
				setUserId((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__USE_SAPGUI:
				setUseSapgui((String)newValue);
				return;
			case RfcPackage.DESTINATION_DATA__X509CERT:
				setX509cert((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case RfcPackage.DESTINATION_DATA__ENTRIES:
				getEntries().clear();
				return;
			case RfcPackage.DESTINATION_DATA__ALIAS_USER:
				setAliasUser(ALIAS_USER_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__ASHOST:
				setAshost(ASHOST_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__AUTH_TYPE:
				setAuthType(AUTH_TYPE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__CLIENT:
				setClient(CLIENT_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__CODEPAGE:
				setCodepage(CODEPAGE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__CPIC_TRACE:
				setCpicTrace(CPIC_TRACE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__DENY_INITIAL_PASSWORD:
				setDenyInitialPassword(DENY_INITIAL_PASSWORD_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__EXPIRATION_PERIOD:
				setExpirationPeriod(EXPIRATION_PERIOD_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__EXPIRATION_TIME:
				setExpirationTime(EXPIRATION_TIME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__GETSSO2:
				setGetsso2(GETSSO2_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__GROUP:
				setGroup(GROUP_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__GWHOST:
				setGwhost(GWHOST_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__GWSERV:
				setGwserv(GWSERV_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__LANG:
				setLang(LANG_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__LCHECK:
				setLcheck(LCHECK_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__MAX_GET_TIME:
				setMaxGetTime(MAX_GET_TIME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__MSHOST:
				setMshost(MSHOST_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__MSSERV:
				setMsserv(MSSERV_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__MYSAPSSO2:
				setMysapsso2(MYSAPSSO2_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__PASSWD:
				setPasswd(PASSWD_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__PASSWORD:
				setPassword(PASSWORD_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__PCS:
				setPcs(PCS_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__PEAK_LIMIT:
				setPeakLimit(PEAK_LIMIT_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__PING_ON_CREATE:
				setPingOnCreate(PING_ON_CREATE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__POOL_CAPACITY:
				setPoolCapacity(POOL_CAPACITY_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__R3NAME:
				setR3name(R3NAME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_DEST:
				setRepositoryDest(REPOSITORY_DEST_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_PASSWD:
				setRepositoryPasswd(REPOSITORY_PASSWD_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION:
				setRepositoryRoundtripOptimization(REPOSITORY_ROUNDTRIP_OPTIMIZATION_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_SNC:
				setRepositorySnc(REPOSITORY_SNC_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__REPOSITORY_USER:
				setRepositoryUser(REPOSITORY_USER_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SAPROUTER:
				setSaprouter(SAPROUTER_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_LIBRARY:
				setSncLibrary(SNC_LIBRARY_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_MODE:
				setSncMode(SNC_MODE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_MYNAME:
				setSncMyname(SNC_MYNAME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_PARTNERNAME:
				setSncPartnername(SNC_PARTNERNAME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SNC_QOP:
				setSncQop(SNC_QOP_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__SYSNR:
				setSysnr(SYSNR_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__TPHOST:
				setTphost(TPHOST_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__TPNAME:
				setTpname(TPNAME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__TRACE:
				setTrace(TRACE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__TYPE:
				setType(TYPE_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__USER_NAME:
				setUserName(USER_NAME_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__USER:
				setUser(USER_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__USER_ID:
				setUserId(USER_ID_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__USE_SAPGUI:
				setUseSapgui(USE_SAPGUI_EDEFAULT);
				return;
			case RfcPackage.DESTINATION_DATA__X509CERT:
				setX509cert(X509CERT_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case RfcPackage.DESTINATION_DATA__ENTRIES:
				return entries != null && !entries.isEmpty();
			case RfcPackage.DESTINATION_DATA__ALIAS_USER:
				return ALIAS_USER_EDEFAULT == null ? getAliasUser() != null : !ALIAS_USER_EDEFAULT.equals(getAliasUser());
			case RfcPackage.DESTINATION_DATA__ASHOST:
				return ASHOST_EDEFAULT == null ? getAshost() != null : !ASHOST_EDEFAULT.equals(getAshost());
			case RfcPackage.DESTINATION_DATA__AUTH_TYPE:
				return AUTH_TYPE_EDEFAULT == null ? getAuthType() != null : !AUTH_TYPE_EDEFAULT.equals(getAuthType());
			case RfcPackage.DESTINATION_DATA__CLIENT:
				return CLIENT_EDEFAULT == null ? getClient() != null : !CLIENT_EDEFAULT.equals(getClient());
			case RfcPackage.DESTINATION_DATA__CODEPAGE:
				return CODEPAGE_EDEFAULT == null ? getCodepage() != null : !CODEPAGE_EDEFAULT.equals(getCodepage());
			case RfcPackage.DESTINATION_DATA__CPIC_TRACE:
				return CPIC_TRACE_EDEFAULT == null ? getCpicTrace() != null : !CPIC_TRACE_EDEFAULT.equals(getCpicTrace());
			case RfcPackage.DESTINATION_DATA__DENY_INITIAL_PASSWORD:
				return DENY_INITIAL_PASSWORD_EDEFAULT == null ? getDenyInitialPassword() != null : !DENY_INITIAL_PASSWORD_EDEFAULT.equals(getDenyInitialPassword());
			case RfcPackage.DESTINATION_DATA__EXPIRATION_PERIOD:
				return EXPIRATION_PERIOD_EDEFAULT == null ? getExpirationPeriod() != null : !EXPIRATION_PERIOD_EDEFAULT.equals(getExpirationPeriod());
			case RfcPackage.DESTINATION_DATA__EXPIRATION_TIME:
				return EXPIRATION_TIME_EDEFAULT == null ? getExpirationTime() != null : !EXPIRATION_TIME_EDEFAULT.equals(getExpirationTime());
			case RfcPackage.DESTINATION_DATA__GETSSO2:
				return GETSSO2_EDEFAULT == null ? getGetsso2() != null : !GETSSO2_EDEFAULT.equals(getGetsso2());
			case RfcPackage.DESTINATION_DATA__GROUP:
				return GROUP_EDEFAULT == null ? getGroup() != null : !GROUP_EDEFAULT.equals(getGroup());
			case RfcPackage.DESTINATION_DATA__GWHOST:
				return GWHOST_EDEFAULT == null ? getGwhost() != null : !GWHOST_EDEFAULT.equals(getGwhost());
			case RfcPackage.DESTINATION_DATA__GWSERV:
				return GWSERV_EDEFAULT == null ? getGwserv() != null : !GWSERV_EDEFAULT.equals(getGwserv());
			case RfcPackage.DESTINATION_DATA__LANG:
				return LANG_EDEFAULT == null ? getLang() != null : !LANG_EDEFAULT.equals(getLang());
			case RfcPackage.DESTINATION_DATA__LCHECK:
				return LCHECK_EDEFAULT == null ? getLcheck() != null : !LCHECK_EDEFAULT.equals(getLcheck());
			case RfcPackage.DESTINATION_DATA__MAX_GET_TIME:
				return MAX_GET_TIME_EDEFAULT == null ? getMaxGetTime() != null : !MAX_GET_TIME_EDEFAULT.equals(getMaxGetTime());
			case RfcPackage.DESTINATION_DATA__MSHOST:
				return MSHOST_EDEFAULT == null ? getMshost() != null : !MSHOST_EDEFAULT.equals(getMshost());
			case RfcPackage.DESTINATION_DATA__MSSERV:
				return MSSERV_EDEFAULT == null ? getMsserv() != null : !MSSERV_EDEFAULT.equals(getMsserv());
			case RfcPackage.DESTINATION_DATA__MYSAPSSO2:
				return MYSAPSSO2_EDEFAULT == null ? getMysapsso2() != null : !MYSAPSSO2_EDEFAULT.equals(getMysapsso2());
			case RfcPackage.DESTINATION_DATA__PASSWD:
				return PASSWD_EDEFAULT == null ? getPasswd() != null : !PASSWD_EDEFAULT.equals(getPasswd());
			case RfcPackage.DESTINATION_DATA__PASSWORD:
				return PASSWORD_EDEFAULT == null ? getPassword() != null : !PASSWORD_EDEFAULT.equals(getPassword());
			case RfcPackage.DESTINATION_DATA__PCS:
				return PCS_EDEFAULT == null ? getPcs() != null : !PCS_EDEFAULT.equals(getPcs());
			case RfcPackage.DESTINATION_DATA__PEAK_LIMIT:
				return PEAK_LIMIT_EDEFAULT == null ? getPeakLimit() != null : !PEAK_LIMIT_EDEFAULT.equals(getPeakLimit());
			case RfcPackage.DESTINATION_DATA__PING_ON_CREATE:
				return PING_ON_CREATE_EDEFAULT == null ? getPingOnCreate() != null : !PING_ON_CREATE_EDEFAULT.equals(getPingOnCreate());
			case RfcPackage.DESTINATION_DATA__POOL_CAPACITY:
				return POOL_CAPACITY_EDEFAULT == null ? getPoolCapacity() != null : !POOL_CAPACITY_EDEFAULT.equals(getPoolCapacity());
			case RfcPackage.DESTINATION_DATA__R3NAME:
				return R3NAME_EDEFAULT == null ? getR3name() != null : !R3NAME_EDEFAULT.equals(getR3name());
			case RfcPackage.DESTINATION_DATA__REPOSITORY_DEST:
				return REPOSITORY_DEST_EDEFAULT == null ? getRepositoryDest() != null : !REPOSITORY_DEST_EDEFAULT.equals(getRepositoryDest());
			case RfcPackage.DESTINATION_DATA__REPOSITORY_PASSWD:
				return REPOSITORY_PASSWD_EDEFAULT == null ? getRepositoryPasswd() != null : !REPOSITORY_PASSWD_EDEFAULT.equals(getRepositoryPasswd());
			case RfcPackage.DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION:
				return REPOSITORY_ROUNDTRIP_OPTIMIZATION_EDEFAULT == null ? getRepositoryRoundtripOptimization() != null : !REPOSITORY_ROUNDTRIP_OPTIMIZATION_EDEFAULT.equals(getRepositoryRoundtripOptimization());
			case RfcPackage.DESTINATION_DATA__REPOSITORY_SNC:
				return REPOSITORY_SNC_EDEFAULT == null ? getRepositorySnc() != null : !REPOSITORY_SNC_EDEFAULT.equals(getRepositorySnc());
			case RfcPackage.DESTINATION_DATA__REPOSITORY_USER:
				return REPOSITORY_USER_EDEFAULT == null ? getRepositoryUser() != null : !REPOSITORY_USER_EDEFAULT.equals(getRepositoryUser());
			case RfcPackage.DESTINATION_DATA__SAPROUTER:
				return SAPROUTER_EDEFAULT == null ? getSaprouter() != null : !SAPROUTER_EDEFAULT.equals(getSaprouter());
			case RfcPackage.DESTINATION_DATA__SNC_LIBRARY:
				return SNC_LIBRARY_EDEFAULT == null ? getSncLibrary() != null : !SNC_LIBRARY_EDEFAULT.equals(getSncLibrary());
			case RfcPackage.DESTINATION_DATA__SNC_MODE:
				return SNC_MODE_EDEFAULT == null ? getSncMode() != null : !SNC_MODE_EDEFAULT.equals(getSncMode());
			case RfcPackage.DESTINATION_DATA__SNC_MYNAME:
				return SNC_MYNAME_EDEFAULT == null ? getSncMyname() != null : !SNC_MYNAME_EDEFAULT.equals(getSncMyname());
			case RfcPackage.DESTINATION_DATA__SNC_PARTNERNAME:
				return SNC_PARTNERNAME_EDEFAULT == null ? getSncPartnername() != null : !SNC_PARTNERNAME_EDEFAULT.equals(getSncPartnername());
			case RfcPackage.DESTINATION_DATA__SNC_QOP:
				return SNC_QOP_EDEFAULT == null ? getSncQop() != null : !SNC_QOP_EDEFAULT.equals(getSncQop());
			case RfcPackage.DESTINATION_DATA__SYSNR:
				return SYSNR_EDEFAULT == null ? getSysnr() != null : !SYSNR_EDEFAULT.equals(getSysnr());
			case RfcPackage.DESTINATION_DATA__TPHOST:
				return TPHOST_EDEFAULT == null ? getTphost() != null : !TPHOST_EDEFAULT.equals(getTphost());
			case RfcPackage.DESTINATION_DATA__TPNAME:
				return TPNAME_EDEFAULT == null ? getTpname() != null : !TPNAME_EDEFAULT.equals(getTpname());
			case RfcPackage.DESTINATION_DATA__TRACE:
				return TRACE_EDEFAULT == null ? getTrace() != null : !TRACE_EDEFAULT.equals(getTrace());
			case RfcPackage.DESTINATION_DATA__TYPE:
				return TYPE_EDEFAULT == null ? getType() != null : !TYPE_EDEFAULT.equals(getType());
			case RfcPackage.DESTINATION_DATA__USER_NAME:
				return USER_NAME_EDEFAULT == null ? getUserName() != null : !USER_NAME_EDEFAULT.equals(getUserName());
			case RfcPackage.DESTINATION_DATA__USER:
				return USER_EDEFAULT == null ? getUser() != null : !USER_EDEFAULT.equals(getUser());
			case RfcPackage.DESTINATION_DATA__USER_ID:
				return USER_ID_EDEFAULT == null ? getUserId() != null : !USER_ID_EDEFAULT.equals(getUserId());
			case RfcPackage.DESTINATION_DATA__USE_SAPGUI:
				return USE_SAPGUI_EDEFAULT == null ? getUseSapgui() != null : !USE_SAPGUI_EDEFAULT.equals(getUseSapgui());
			case RfcPackage.DESTINATION_DATA__X509CERT:
				return X509CERT_EDEFAULT == null ? getX509cert() != null : !X509CERT_EDEFAULT.equals(getX509cert());
		}
		return super.eIsSet(featureID);
	}

} //DestinationDataImpl
