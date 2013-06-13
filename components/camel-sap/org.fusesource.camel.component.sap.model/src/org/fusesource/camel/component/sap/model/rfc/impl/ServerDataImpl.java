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

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EcoreEMap;
import org.eclipse.emf.ecore.util.InternalEList;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.ServerData;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Server Data</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getEntries <em>Entries</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getGwhost <em>Gwhost</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getGwserv <em>Gwserv</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getProgid <em>Progid</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getConnectionCount <em>Connection Count</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getSaprouter <em>Saprouter</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getMaxStartUpDelay <em>Max Start Up Delay</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getRepositoryDestination <em>Repository Destination</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getRepositoryMap <em>Repository Map</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getTrace <em>Trace</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getWorkerThreadCount <em>Worker Thread Count</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getWorkerThreadMinCount <em>Worker Thread Min Count</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getSncMode <em>Snc Mode</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getSncQop <em>Snc Qop</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getSncMyname <em>Snc Myname</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl#getSncLib <em>Snc Lib</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ServerDataImpl extends EObjectImpl implements ServerData {
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
	 * The default value of the '{@link #getProgid() <em>Progid</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProgid()
	 * @generated
	 * @ordered
	 */
	protected static final String PROGID_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getConnectionCount() <em>Connection Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConnectionCount()
	 * @generated
	 * @ordered
	 */
	protected static final String CONNECTION_COUNT_EDEFAULT = "";

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
	 * The default value of the '{@link #getMaxStartUpDelay() <em>Max Start Up Delay</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxStartUpDelay()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_START_UP_DELAY_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositoryDestination() <em>Repository Destination</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryDestination()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_DESTINATION_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getRepositoryMap() <em>Repository Map</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryMap()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_MAP_EDEFAULT = "";

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
	 * The default value of the '{@link #getWorkerThreadCount() <em>Worker Thread Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWorkerThreadCount()
	 * @generated
	 * @ordered
	 */
	protected static final String WORKER_THREAD_COUNT_EDEFAULT = "";

	/**
	 * The default value of the '{@link #getWorkerThreadMinCount() <em>Worker Thread Min Count</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWorkerThreadMinCount()
	 * @generated
	 * @ordered
	 */
	protected static final String WORKER_THREAD_MIN_COUNT_EDEFAULT = "";

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
	 * The default value of the '{@link #getSncQop() <em>Snc Qop</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncQop()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_QOP_EDEFAULT = "";

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
	 * The default value of the '{@link #getSncLib() <em>Snc Lib</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSncLib()
	 * @generated
	 * @ordered
	 */
	protected static final String SNC_LIB_EDEFAULT = "";

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerDataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return RfcPackage.Literals.SERVER_DATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EMap<String, String> getEntries() {
		if (entries == null) {
			entries = new EcoreEMap<String,String>(RfcPackage.Literals.SERVER_DATA_ENTRY, ServerDataEntryImpl.class, this, RfcPackage.SERVER_DATA__ENTRIES);
		}
		return entries;
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
	public String getProgid() {
		// TODO: implement this method to return the 'Progid' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProgid(String newProgid) {
		// TODO: implement this method to set the 'Progid' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getConnectionCount() {
		// TODO: implement this method to return the 'Connection Count' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setConnectionCount(String newConnectionCount) {
		// TODO: implement this method to set the 'Connection Count' attribute
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
	public String getMaxStartUpDelay() {
		// TODO: implement this method to return the 'Max Start Up Delay' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxStartUpDelay(String newMaxStartUpDelay) {
		// TODO: implement this method to set the 'Max Start Up Delay' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositoryDestination() {
		// TODO: implement this method to return the 'Repository Destination' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositoryDestination(String newRepositoryDestination) {
		// TODO: implement this method to set the 'Repository Destination' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRepositoryMap() {
		// TODO: implement this method to return the 'Repository Map' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRepositoryMap(String newRepositoryMap) {
		// TODO: implement this method to set the 'Repository Map' attribute
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
	public String getWorkerThreadCount() {
		// TODO: implement this method to return the 'Worker Thread Count' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setWorkerThreadCount(String newWorkerThreadCount) {
		// TODO: implement this method to set the 'Worker Thread Count' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getWorkerThreadMinCount() {
		// TODO: implement this method to return the 'Worker Thread Min Count' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setWorkerThreadMinCount(String newWorkerThreadMinCount) {
		// TODO: implement this method to set the 'Worker Thread Min Count' attribute
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
	public String getSncLib() {
		// TODO: implement this method to return the 'Snc Lib' attribute
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSncLib(String newSncLib) {
		// TODO: implement this method to set the 'Snc Lib' attribute
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
			case RfcPackage.SERVER_DATA__ENTRIES:
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
			case RfcPackage.SERVER_DATA__ENTRIES:
				if (coreType) return getEntries();
				else return getEntries().map();
			case RfcPackage.SERVER_DATA__GWHOST:
				return getGwhost();
			case RfcPackage.SERVER_DATA__GWSERV:
				return getGwserv();
			case RfcPackage.SERVER_DATA__PROGID:
				return getProgid();
			case RfcPackage.SERVER_DATA__CONNECTION_COUNT:
				return getConnectionCount();
			case RfcPackage.SERVER_DATA__SAPROUTER:
				return getSaprouter();
			case RfcPackage.SERVER_DATA__MAX_START_UP_DELAY:
				return getMaxStartUpDelay();
			case RfcPackage.SERVER_DATA__REPOSITORY_DESTINATION:
				return getRepositoryDestination();
			case RfcPackage.SERVER_DATA__REPOSITORY_MAP:
				return getRepositoryMap();
			case RfcPackage.SERVER_DATA__TRACE:
				return getTrace();
			case RfcPackage.SERVER_DATA__WORKER_THREAD_COUNT:
				return getWorkerThreadCount();
			case RfcPackage.SERVER_DATA__WORKER_THREAD_MIN_COUNT:
				return getWorkerThreadMinCount();
			case RfcPackage.SERVER_DATA__SNC_MODE:
				return getSncMode();
			case RfcPackage.SERVER_DATA__SNC_QOP:
				return getSncQop();
			case RfcPackage.SERVER_DATA__SNC_MYNAME:
				return getSncMyname();
			case RfcPackage.SERVER_DATA__SNC_LIB:
				return getSncLib();
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
			case RfcPackage.SERVER_DATA__ENTRIES:
				((EStructuralFeature.Setting)getEntries()).set(newValue);
				return;
			case RfcPackage.SERVER_DATA__GWHOST:
				setGwhost((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__GWSERV:
				setGwserv((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__PROGID:
				setProgid((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__CONNECTION_COUNT:
				setConnectionCount((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__SAPROUTER:
				setSaprouter((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__MAX_START_UP_DELAY:
				setMaxStartUpDelay((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__REPOSITORY_DESTINATION:
				setRepositoryDestination((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__REPOSITORY_MAP:
				setRepositoryMap((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__TRACE:
				setTrace((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__WORKER_THREAD_COUNT:
				setWorkerThreadCount((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__WORKER_THREAD_MIN_COUNT:
				setWorkerThreadMinCount((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__SNC_MODE:
				setSncMode((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__SNC_QOP:
				setSncQop((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__SNC_MYNAME:
				setSncMyname((String)newValue);
				return;
			case RfcPackage.SERVER_DATA__SNC_LIB:
				setSncLib((String)newValue);
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
			case RfcPackage.SERVER_DATA__ENTRIES:
				getEntries().clear();
				return;
			case RfcPackage.SERVER_DATA__GWHOST:
				setGwhost(GWHOST_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__GWSERV:
				setGwserv(GWSERV_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__PROGID:
				setProgid(PROGID_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__CONNECTION_COUNT:
				setConnectionCount(CONNECTION_COUNT_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__SAPROUTER:
				setSaprouter(SAPROUTER_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__MAX_START_UP_DELAY:
				setMaxStartUpDelay(MAX_START_UP_DELAY_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__REPOSITORY_DESTINATION:
				setRepositoryDestination(REPOSITORY_DESTINATION_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__REPOSITORY_MAP:
				setRepositoryMap(REPOSITORY_MAP_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__TRACE:
				setTrace(TRACE_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__WORKER_THREAD_COUNT:
				setWorkerThreadCount(WORKER_THREAD_COUNT_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__WORKER_THREAD_MIN_COUNT:
				setWorkerThreadMinCount(WORKER_THREAD_MIN_COUNT_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__SNC_MODE:
				setSncMode(SNC_MODE_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__SNC_QOP:
				setSncQop(SNC_QOP_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__SNC_MYNAME:
				setSncMyname(SNC_MYNAME_EDEFAULT);
				return;
			case RfcPackage.SERVER_DATA__SNC_LIB:
				setSncLib(SNC_LIB_EDEFAULT);
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
			case RfcPackage.SERVER_DATA__ENTRIES:
				return entries != null && !entries.isEmpty();
			case RfcPackage.SERVER_DATA__GWHOST:
				return GWHOST_EDEFAULT == null ? getGwhost() != null : !GWHOST_EDEFAULT.equals(getGwhost());
			case RfcPackage.SERVER_DATA__GWSERV:
				return GWSERV_EDEFAULT == null ? getGwserv() != null : !GWSERV_EDEFAULT.equals(getGwserv());
			case RfcPackage.SERVER_DATA__PROGID:
				return PROGID_EDEFAULT == null ? getProgid() != null : !PROGID_EDEFAULT.equals(getProgid());
			case RfcPackage.SERVER_DATA__CONNECTION_COUNT:
				return CONNECTION_COUNT_EDEFAULT == null ? getConnectionCount() != null : !CONNECTION_COUNT_EDEFAULT.equals(getConnectionCount());
			case RfcPackage.SERVER_DATA__SAPROUTER:
				return SAPROUTER_EDEFAULT == null ? getSaprouter() != null : !SAPROUTER_EDEFAULT.equals(getSaprouter());
			case RfcPackage.SERVER_DATA__MAX_START_UP_DELAY:
				return MAX_START_UP_DELAY_EDEFAULT == null ? getMaxStartUpDelay() != null : !MAX_START_UP_DELAY_EDEFAULT.equals(getMaxStartUpDelay());
			case RfcPackage.SERVER_DATA__REPOSITORY_DESTINATION:
				return REPOSITORY_DESTINATION_EDEFAULT == null ? getRepositoryDestination() != null : !REPOSITORY_DESTINATION_EDEFAULT.equals(getRepositoryDestination());
			case RfcPackage.SERVER_DATA__REPOSITORY_MAP:
				return REPOSITORY_MAP_EDEFAULT == null ? getRepositoryMap() != null : !REPOSITORY_MAP_EDEFAULT.equals(getRepositoryMap());
			case RfcPackage.SERVER_DATA__TRACE:
				return TRACE_EDEFAULT == null ? getTrace() != null : !TRACE_EDEFAULT.equals(getTrace());
			case RfcPackage.SERVER_DATA__WORKER_THREAD_COUNT:
				return WORKER_THREAD_COUNT_EDEFAULT == null ? getWorkerThreadCount() != null : !WORKER_THREAD_COUNT_EDEFAULT.equals(getWorkerThreadCount());
			case RfcPackage.SERVER_DATA__WORKER_THREAD_MIN_COUNT:
				return WORKER_THREAD_MIN_COUNT_EDEFAULT == null ? getWorkerThreadMinCount() != null : !WORKER_THREAD_MIN_COUNT_EDEFAULT.equals(getWorkerThreadMinCount());
			case RfcPackage.SERVER_DATA__SNC_MODE:
				return SNC_MODE_EDEFAULT == null ? getSncMode() != null : !SNC_MODE_EDEFAULT.equals(getSncMode());
			case RfcPackage.SERVER_DATA__SNC_QOP:
				return SNC_QOP_EDEFAULT == null ? getSncQop() != null : !SNC_QOP_EDEFAULT.equals(getSncQop());
			case RfcPackage.SERVER_DATA__SNC_MYNAME:
				return SNC_MYNAME_EDEFAULT == null ? getSncMyname() != null : !SNC_MYNAME_EDEFAULT.equals(getSncMyname());
			case RfcPackage.SERVER_DATA__SNC_LIB:
				return SNC_LIB_EDEFAULT == null ? getSncLib() != null : !SNC_LIB_EDEFAULT.equals(getSncLib());
		}
		return super.eIsSet(featureID);
	}

} //ServerDataImpl
