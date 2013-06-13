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
 * A representation of the model object '<em><b>Server Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getEntries <em>Entries</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getGwhost <em>Gwhost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getGwserv <em>Gwserv</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getProgid <em>Progid</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getConnectionCount <em>Connection Count</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSaprouter <em>Saprouter</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getMaxStartUpDelay <em>Max Start Up Delay</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryDestination <em>Repository Destination</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryMap <em>Repository Map</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getTrace <em>Trace</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadCount <em>Worker Thread Count</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadMinCount <em>Worker Thread Min Count</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMode <em>Snc Mode</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncQop <em>Snc Qop</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMyname <em>Snc Myname</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncLib <em>Snc Lib</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData()
 * @model
 * @generated
 */
public interface ServerData extends EObject {
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_Entries()
	 * @model mapType="org.fusesource.camel.component.sap.model.rfc.ServerDataEntry<org.eclipse.emf.ecore.EString, org.eclipse.emf.ecore.EString>"
	 * @generated
	 */
	EMap<String, String> getEntries();

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_Gwhost()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getGwhost();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getGwhost <em>Gwhost</em>}' attribute.
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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_Gwserv()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getGwserv();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getGwserv <em>Gwserv</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Gwserv</em>' attribute.
	 * @see #getGwserv()
	 * @generated
	 */
	void setGwserv(String value);

	/**
	 * Returns the value of the '<em><b>Progid</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Progid</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Progid</em>' attribute.
	 * @see #setProgid(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_Progid()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getProgid();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getProgid <em>Progid</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Progid</em>' attribute.
	 * @see #getProgid()
	 * @generated
	 */
	void setProgid(String value);

	/**
	 * Returns the value of the '<em><b>Connection Count</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Connection Count</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Connection Count</em>' attribute.
	 * @see #setConnectionCount(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_ConnectionCount()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getConnectionCount();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getConnectionCount <em>Connection Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Connection Count</em>' attribute.
	 * @see #getConnectionCount()
	 * @generated
	 */
	void setConnectionCount(String value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_Saprouter()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSaprouter();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSaprouter <em>Saprouter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Saprouter</em>' attribute.
	 * @see #getSaprouter()
	 * @generated
	 */
	void setSaprouter(String value);

	/**
	 * Returns the value of the '<em><b>Max Start Up Delay</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Start Up Delay</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Start Up Delay</em>' attribute.
	 * @see #setMaxStartUpDelay(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_MaxStartUpDelay()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getMaxStartUpDelay();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getMaxStartUpDelay <em>Max Start Up Delay</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Start Up Delay</em>' attribute.
	 * @see #getMaxStartUpDelay()
	 * @generated
	 */
	void setMaxStartUpDelay(String value);

	/**
	 * Returns the value of the '<em><b>Repository Destination</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository Destination</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Destination</em>' attribute.
	 * @see #setRepositoryDestination(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_RepositoryDestination()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositoryDestination();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryDestination <em>Repository Destination</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Destination</em>' attribute.
	 * @see #getRepositoryDestination()
	 * @generated
	 */
	void setRepositoryDestination(String value);

	/**
	 * Returns the value of the '<em><b>Repository Map</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Repository Map</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Map</em>' attribute.
	 * @see #setRepositoryMap(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_RepositoryMap()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getRepositoryMap();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getRepositoryMap <em>Repository Map</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Map</em>' attribute.
	 * @see #getRepositoryMap()
	 * @generated
	 */
	void setRepositoryMap(String value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_Trace()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getTrace();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getTrace <em>Trace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Trace</em>' attribute.
	 * @see #getTrace()
	 * @generated
	 */
	void setTrace(String value);

	/**
	 * Returns the value of the '<em><b>Worker Thread Count</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Worker Thread Count</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Worker Thread Count</em>' attribute.
	 * @see #setWorkerThreadCount(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_WorkerThreadCount()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getWorkerThreadCount();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadCount <em>Worker Thread Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Worker Thread Count</em>' attribute.
	 * @see #getWorkerThreadCount()
	 * @generated
	 */
	void setWorkerThreadCount(String value);

	/**
	 * Returns the value of the '<em><b>Worker Thread Min Count</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Worker Thread Min Count</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Worker Thread Min Count</em>' attribute.
	 * @see #setWorkerThreadMinCount(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_WorkerThreadMinCount()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getWorkerThreadMinCount();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getWorkerThreadMinCount <em>Worker Thread Min Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Worker Thread Min Count</em>' attribute.
	 * @see #getWorkerThreadMinCount()
	 * @generated
	 */
	void setWorkerThreadMinCount(String value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_SncMode()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncMode();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMode <em>Snc Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Mode</em>' attribute.
	 * @see #getSncMode()
	 * @generated
	 */
	void setSncMode(String value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_SncQop()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncQop();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncQop <em>Snc Qop</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Qop</em>' attribute.
	 * @see #getSncQop()
	 * @generated
	 */
	void setSncQop(String value);

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
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_SncMyname()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncMyname();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncMyname <em>Snc Myname</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Myname</em>' attribute.
	 * @see #getSncMyname()
	 * @generated
	 */
	void setSncMyname(String value);

	/**
	 * Returns the value of the '<em><b>Snc Lib</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Snc Lib</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snc Lib</em>' attribute.
	 * @see #setSncLib(String)
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getServerData_SncLib()
	 * @model default="" transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getSncLib();

	/**
	 * Sets the value of the '{@link org.fusesource.camel.component.sap.model.rfc.ServerData#getSncLib <em>Snc Lib</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Snc Lib</em>' attribute.
	 * @see #getSncLib()
	 * @generated
	 */
	void setSncLib(String value);

} // ServerData
