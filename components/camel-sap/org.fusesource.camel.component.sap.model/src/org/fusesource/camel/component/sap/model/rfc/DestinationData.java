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

import org.eclipse.emf.common.util.EMap;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Destination Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getEntries <em>Entries</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAliasUser <em>Alias User</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAshost <em>Ashost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAuthType <em>Auth Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getClient <em>Client</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getCodepage <em>Codepage</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getCpicTrace <em>Cpic Trace</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getDenyInitialPassword <em>Deny Initial Password</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationPeriod <em>Expiration Period</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationTime <em>Expiration Time</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGetsso2 <em>Getsso2</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGroup <em>Group</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwhost <em>Gwhost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwserv <em>Gwserv</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getLang <em>Lang</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getLcheck <em>Lcheck</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMaxGetTime <em>Max Get Time</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMshost <em>Mshost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMsserv <em>Msserv</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMysapsso2 <em>Mysapsso2</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPasswd <em>Passwd</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPassword <em>Password</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPcs <em>Pcs</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPeakLimit <em>Peak Limit</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPingOnCreate <em>Ping On Create</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPoolCapacity <em>Pool Capacity</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getR3name <em>R3name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryDest <em>Repository Dest</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryPasswd <em>Repository Passwd</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryRoundtripOptimization <em>Repository Roundtrip Optimization</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositorySnc <em>Repository Snc</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryUser <em>Repository User</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSaprouter <em>Saprouter</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncLibrary <em>Snc Library</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMode <em>Snc Mode</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMyname <em>Snc Myname</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncPartnername <em>Snc Partnername</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncQop <em>Snc Qop</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSysnr <em>Sysnr</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTphost <em>Tphost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTpname <em>Tpname</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTrace <em>Trace</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getType <em>Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserName <em>User Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUser <em>User</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserId <em>User Id</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUseSapgui <em>Use Sapgui</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getX509cert <em>X50 9cert</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData()
 * @model
 * @generated
 */
public interface DestinationData extends EObject {
	/**
	 * Returns the value of the '<em><b>Entries</b></em>' map.
	 * The key is of type {@link java.lang.String},
	 * and the value is of type {@link java.lang.String},
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Entries</em>' map isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Entries</em>' map.
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Entries()
	 * @model mapType="org.fusesource.camel.component.sap.model.rfc.DestinationDataEntry<org.eclipse.emf.ecore.EString, org.eclipse.emf.ecore.EString>"
	 * @generated
	 */
	EMap<String, String> getEntries();

	/**
	 * Returns the value of the '<em><b>Alias User</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Alias User</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Alias User</em>' attribute.
	 * @see #setAliasUser(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_AliasUser()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getAliasUser();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAliasUser <em>Alias User</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Alias User</em>' attribute.
	 * @see #getAliasUser()
	 * @generated
	 */
	void setAliasUser(String value);

	/**
	 * Returns the value of the '<em><b>Ashost</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ashost</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ashost</em>' attribute.
	 * @see #setAshost(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Ashost()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getAshost();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAshost <em>Ashost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ashost</em>' attribute.
	 * @see #getAshost()
	 * @generated
	 */
	void setAshost(String value);

	/**
	 * Returns the value of the '<em><b>Auth Type</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Auth Type</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Auth Type</em>' attribute.
	 * @see #setAuthType(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_AuthType()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getAuthType();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getAuthType <em>Auth Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Auth Type</em>' attribute.
	 * @see #getAuthType()
	 * @generated
	 */
	void setAuthType(String value);

	/**
	 * Returns the value of the '<em><b>Client</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Client</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Client</em>' attribute.
	 * @see #setClient(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Client()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getClient();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getClient <em>Client</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Client</em>' attribute.
	 * @see #getClient()
	 * @generated
	 */
	void setClient(String value);

	/**
	 * Returns the value of the '<em><b>Codepage</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Codepage</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Codepage</em>' attribute.
	 * @see #setCodepage(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Codepage()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getCodepage();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getCodepage <em>Codepage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Codepage</em>' attribute.
	 * @see #getCodepage()
	 * @generated
	 */
	void setCodepage(String value);

	/**
	 * Returns the value of the '<em><b>Cpic Trace</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Cpic Trace</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Cpic Trace</em>' attribute.
	 * @see #setCpicTrace(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_CpicTrace()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getCpicTrace();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getCpicTrace <em>Cpic Trace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Cpic Trace</em>' attribute.
	 * @see #getCpicTrace()
	 * @generated
	 */
	void setCpicTrace(String value);

	/**
	 * Returns the value of the '<em><b>Deny Initial Password</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Deny Initial Password</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Deny Initial Password</em>' attribute.
	 * @see #setDenyInitialPassword(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_DenyInitialPassword()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getDenyInitialPassword();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getDenyInitialPassword <em>Deny Initial Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Deny Initial Password</em>' attribute.
	 * @see #getDenyInitialPassword()
	 * @generated
	 */
	void setDenyInitialPassword(String value);

	/**
	 * Returns the value of the '<em><b>Expiration Period</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Expiration Period</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Expiration Period</em>' attribute.
	 * @see #setExpirationPeriod(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_ExpirationPeriod()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getExpirationPeriod();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationPeriod <em>Expiration Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Expiration Period</em>' attribute.
	 * @see #getExpirationPeriod()
	 * @generated
	 */
	void setExpirationPeriod(String value);

	/**
	 * Returns the value of the '<em><b>Expiration Time</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Expiration Time</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Expiration Time</em>' attribute.
	 * @see #setExpirationTime(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_ExpirationTime()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getExpirationTime();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getExpirationTime <em>Expiration Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Expiration Time</em>' attribute.
	 * @see #getExpirationTime()
	 * @generated
	 */
	void setExpirationTime(String value);

	/**
	 * Returns the value of the '<em><b>Getsso2</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Getsso2</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Getsso2</em>' attribute.
	 * @see #setGetsso2(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Getsso2()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getGetsso2();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGetsso2 <em>Getsso2</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Getsso2</em>' attribute.
	 * @see #getGetsso2()
	 * @generated
	 */
	void setGetsso2(String value);

	/**
	 * Returns the value of the '<em><b>Group</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Group</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Group</em>' attribute.
	 * @see #setGroup(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Group()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getGroup();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGroup <em>Group</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Group</em>' attribute.
	 * @see #getGroup()
	 * @generated
	 */
	void setGroup(String value);

	/**
	 * Returns the value of the '<em><b>Gwhost</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Gwhost</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Gwhost</em>' attribute.
	 * @see #setGwhost(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Gwhost()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getGwhost();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwhost <em>Gwhost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Gwhost</em>' attribute.
	 * @see #getGwhost()
	 * @generated
	 */
	void setGwhost(String value);

	/**
	 * Returns the value of the '<em><b>Gwserv</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Gwserv</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Gwserv</em>' attribute.
	 * @see #setGwserv(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Gwserv()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getGwserv();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getGwserv <em>Gwserv</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Gwserv</em>' attribute.
	 * @see #getGwserv()
	 * @generated
	 */
	void setGwserv(String value);

	/**
	 * Returns the value of the '<em><b>Lang</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Lang</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Lang</em>' attribute.
	 * @see #setLang(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Lang()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getLang();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getLang <em>Lang</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Lang</em>' attribute.
	 * @see #getLang()
	 * @generated
	 */
	void setLang(String value);

	/**
	 * Returns the value of the '<em><b>Lcheck</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Lcheck</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Lcheck</em>' attribute.
	 * @see #setLcheck(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Lcheck()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getLcheck();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getLcheck <em>Lcheck</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Lcheck</em>' attribute.
	 * @see #getLcheck()
	 * @generated
	 */
	void setLcheck(String value);

	/**
	 * Returns the value of the '<em><b>Max Get Time</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Get Time</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Get Time</em>' attribute.
	 * @see #setMaxGetTime(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_MaxGetTime()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getMaxGetTime();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMaxGetTime <em>Max Get Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Get Time</em>' attribute.
	 * @see #getMaxGetTime()
	 * @generated
	 */
	void setMaxGetTime(String value);

	/**
	 * Returns the value of the '<em><b>Mshost</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mshost</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mshost</em>' attribute.
	 * @see #setMshost(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Mshost()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getMshost();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMshost <em>Mshost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mshost</em>' attribute.
	 * @see #getMshost()
	 * @generated
	 */
	void setMshost(String value);

	/**
	 * Returns the value of the '<em><b>Msserv</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Msserv</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Msserv</em>' attribute.
	 * @see #setMsserv(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Msserv()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getMsserv();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMsserv <em>Msserv</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Msserv</em>' attribute.
	 * @see #getMsserv()
	 * @generated
	 */
	void setMsserv(String value);

	/**
	 * Returns the value of the '<em><b>Mysapsso2</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mysapsso2</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mysapsso2</em>' attribute.
	 * @see #setMysapsso2(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Mysapsso2()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getMysapsso2();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getMysapsso2 <em>Mysapsso2</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mysapsso2</em>' attribute.
	 * @see #getMysapsso2()
	 * @generated
	 */
	void setMysapsso2(String value);

	/**
	 * Returns the value of the '<em><b>Passwd</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Passwd</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Passwd</em>' attribute.
	 * @see #setPasswd(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Passwd()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getPasswd();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPasswd <em>Passwd</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Passwd</em>' attribute.
	 * @see #getPasswd()
	 * @generated
	 */
	void setPasswd(String value);

	/**
	 * Returns the value of the '<em><b>Password</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Password</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Password</em>' attribute.
	 * @see #setPassword(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Password()
	 * @model transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getPassword();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPassword <em>Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Password</em>' attribute.
	 * @see #getPassword()
	 * @generated
	 */
	void setPassword(String value);

	/**
	 * Returns the value of the '<em><b>Pcs</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Pcs</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Pcs</em>' attribute.
	 * @see #setPcs(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Pcs()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getPcs();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPcs <em>Pcs</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Pcs</em>' attribute.
	 * @see #getPcs()
	 * @generated
	 */
	void setPcs(String value);

	/**
	 * Returns the value of the '<em><b>Peak Limit</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Peak Limit</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Peak Limit</em>' attribute.
	 * @see #setPeakLimit(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_PeakLimit()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getPeakLimit();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPeakLimit <em>Peak Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Peak Limit</em>' attribute.
	 * @see #getPeakLimit()
	 * @generated
	 */
	void setPeakLimit(String value);

	/**
	 * Returns the value of the '<em><b>Ping On Create</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ping On Create</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ping On Create</em>' attribute.
	 * @see #setPingOnCreate(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_PingOnCreate()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getPingOnCreate();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPingOnCreate <em>Ping On Create</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ping On Create</em>' attribute.
	 * @see #getPingOnCreate()
	 * @generated
	 */
	void setPingOnCreate(String value);

	/**
	 * Returns the value of the '<em><b>Pool Capacity</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Pool Capacity</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Pool Capacity</em>' attribute.
	 * @see #setPoolCapacity(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_PoolCapacity()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getPoolCapacity();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getPoolCapacity <em>Pool Capacity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Pool Capacity</em>' attribute.
	 * @see #getPoolCapacity()
	 * @generated
	 */
	void setPoolCapacity(String value);

	/**
	 * Returns the value of the '<em><b>R3name</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>R3name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>R3name</em>' attribute.
	 * @see #setR3name(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_R3name()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getR3name();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getR3name <em>R3name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>R3name</em>' attribute.
	 * @see #getR3name()
	 * @generated
	 */
	void setR3name(String value);

	/**
	 * Returns the value of the '<em><b>Repository Dest</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository Dest</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Dest</em>' attribute.
	 * @see #setRepositoryDest(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_RepositoryDest()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositoryDest();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryDest <em>Repository Dest</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Dest</em>' attribute.
	 * @see #getRepositoryDest()
	 * @generated
	 */
	void setRepositoryDest(String value);

	/**
	 * Returns the value of the '<em><b>Repository Passwd</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository Passwd</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Passwd</em>' attribute.
	 * @see #setRepositoryPasswd(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_RepositoryPasswd()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositoryPasswd();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryPasswd <em>Repository Passwd</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Passwd</em>' attribute.
	 * @see #getRepositoryPasswd()
	 * @generated
	 */
	void setRepositoryPasswd(String value);

	/**
	 * Returns the value of the '<em><b>Repository Roundtrip Optimization</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository Roundtrip Optimization</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Roundtrip Optimization</em>' attribute.
	 * @see #setRepositoryRoundtripOptimization(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_RepositoryRoundtripOptimization()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositoryRoundtripOptimization();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryRoundtripOptimization <em>Repository Roundtrip Optimization</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Roundtrip Optimization</em>' attribute.
	 * @see #getRepositoryRoundtripOptimization()
	 * @generated
	 */
	void setRepositoryRoundtripOptimization(String value);

	/**
	 * Returns the value of the '<em><b>Repository Snc</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository Snc</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Snc</em>' attribute.
	 * @see #setRepositorySnc(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_RepositorySnc()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositorySnc();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositorySnc <em>Repository Snc</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Snc</em>' attribute.
	 * @see #getRepositorySnc()
	 * @generated
	 */
	void setRepositorySnc(String value);

	/**
	 * Returns the value of the '<em><b>Repository User</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository User</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository User</em>' attribute.
	 * @see #setRepositoryUser(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_RepositoryUser()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositoryUser();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getRepositoryUser <em>Repository User</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository User</em>' attribute.
	 * @see #getRepositoryUser()
	 * @generated
	 */
	void setRepositoryUser(String value);

	/**
	 * Returns the value of the '<em><b>Saprouter</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Saprouter</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Saprouter</em>' attribute.
	 * @see #setSaprouter(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Saprouter()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSaprouter();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSaprouter <em>Saprouter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Saprouter</em>' attribute.
	 * @see #getSaprouter()
	 * @generated
	 */
	void setSaprouter(String value);

	/**
	 * Returns the value of the '<em><b>Snc Library</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Snc Library</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snc Library</em>' attribute.
	 * @see #setSncLibrary(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_SncLibrary()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncLibrary();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncLibrary <em>Snc Library</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Library</em>' attribute.
	 * @see #getSncLibrary()
	 * @generated
	 */
	void setSncLibrary(String value);

	/**
	 * Returns the value of the '<em><b>Snc Mode</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Snc Mode</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snc Mode</em>' attribute.
	 * @see #setSncMode(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_SncMode()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncMode();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMode <em>Snc Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Mode</em>' attribute.
	 * @see #getSncMode()
	 * @generated
	 */
	void setSncMode(String value);

	/**
	 * Returns the value of the '<em><b>Snc Myname</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Snc Myname</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snc Myname</em>' attribute.
	 * @see #setSncMyname(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_SncMyname()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncMyname();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncMyname <em>Snc Myname</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Myname</em>' attribute.
	 * @see #getSncMyname()
	 * @generated
	 */
	void setSncMyname(String value);

	/**
	 * Returns the value of the '<em><b>Snc Partnername</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Snc Partnername</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snc Partnername</em>' attribute.
	 * @see #setSncPartnername(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_SncPartnername()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncPartnername();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncPartnername <em>Snc Partnername</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Partnername</em>' attribute.
	 * @see #getSncPartnername()
	 * @generated
	 */
	void setSncPartnername(String value);

	/**
	 * Returns the value of the '<em><b>Snc Qop</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Snc Qop</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snc Qop</em>' attribute.
	 * @see #setSncQop(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_SncQop()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncQop();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSncQop <em>Snc Qop</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Qop</em>' attribute.
	 * @see #getSncQop()
	 * @generated
	 */
	void setSncQop(String value);

	/**
	 * Returns the value of the '<em><b>Sysnr</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Sysnr</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sysnr</em>' attribute.
	 * @see #setSysnr(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Sysnr()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSysnr();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getSysnr <em>Sysnr</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Sysnr</em>' attribute.
	 * @see #getSysnr()
	 * @generated
	 */
	void setSysnr(String value);

	/**
	 * Returns the value of the '<em><b>Tphost</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Tphost</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tphost</em>' attribute.
	 * @see #setTphost(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Tphost()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getTphost();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTphost <em>Tphost</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Tphost</em>' attribute.
	 * @see #getTphost()
	 * @generated
	 */
	void setTphost(String value);

	/**
	 * Returns the value of the '<em><b>Tpname</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Tpname</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tpname</em>' attribute.
	 * @see #setTpname(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Tpname()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getTpname();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTpname <em>Tpname</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Tpname</em>' attribute.
	 * @see #getTpname()
	 * @generated
	 */
	void setTpname(String value);

	/**
	 * Returns the value of the '<em><b>Trace</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Trace</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Trace</em>' attribute.
	 * @see #setTrace(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Trace()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getTrace();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getTrace <em>Trace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Trace</em>' attribute.
	 * @see #getTrace()
	 * @generated
	 */
	void setTrace(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Type</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #setType(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_Type()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Returns the value of the '<em><b>User Name</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>User Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>User Name</em>' attribute.
	 * @see #setUserName(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_UserName()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getUserName();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserName <em>User Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>User Name</em>' attribute.
	 * @see #getUserName()
	 * @generated
	 */
	void setUserName(String value);

	/**
	 * Returns the value of the '<em><b>User</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>User</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>User</em>' attribute.
	 * @see #setUser(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_User()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getUser();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUser <em>User</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>User</em>' attribute.
	 * @see #getUser()
	 * @generated
	 */
	void setUser(String value);

	/**
	 * Returns the value of the '<em><b>User Id</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>User Id</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>User Id</em>' attribute.
	 * @see #setUserId(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_UserId()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getUserId();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUserId <em>User Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>User Id</em>' attribute.
	 * @see #getUserId()
	 * @generated
	 */
	void setUserId(String value);

	/**
	 * Returns the value of the '<em><b>Use Sapgui</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Use Sapgui</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Use Sapgui</em>' attribute.
	 * @see #setUseSapgui(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_UseSapgui()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getUseSapgui();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getUseSapgui <em>Use Sapgui</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Use Sapgui</em>' attribute.
	 * @see #getUseSapgui()
	 * @generated
	 */
	void setUseSapgui(String value);

	/**
	 * Returns the value of the '<em><b>X50 9cert</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>X50 9cert</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>X50 9cert</em>' attribute.
	 * @see #setX509cert(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getDestinationData_X509cert()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getX509cert();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.DestinationData#getX509cert <em>X50 9cert</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>X50 9cert</em>' attribute.
	 * @see #getX509cert()
	 * @generated
	 */
	void setX509cert(String value);

} // DestinationData
