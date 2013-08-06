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

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.fusesource.camel.component.sap.model.rfc.FieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RecordMetaData;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Record Meta Data</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl#getFieldMetaData <em>Field Meta Data</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl#getRecordFieldMetaData <em>Record Field Meta Data</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RecordMetaDataImpl extends EObjectImpl implements RecordMetaData {
	/**
	 * The cached value of the '{@link #getFieldMetaData() <em>Field Meta Data</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFieldMetaData()
	 * @generated
	 * @ordered
	 */
	protected EList<FieldMetaData> fieldMetaData;

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RecordMetaDataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return RfcPackage.Literals.RECORD_META_DATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<FieldMetaData> getFieldMetaData() {
		if (fieldMetaData == null) {
			fieldMetaData = new EObjectContainmentEList<FieldMetaData>(FieldMetaData.class, this, RfcPackage.RECORD_META_DATA__FIELD_META_DATA);
		}
		return fieldMetaData;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.RECORD_META_DATA__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public List<FieldMetaData> getRecordFieldMetaData() {
		return getFieldMetaData();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setRecordFieldMetaData(List<FieldMetaData> newRecordFieldMetaData) {
		getFieldMetaData().clear();
		getFieldMetaData().addAll(newRecordFieldMetaData);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case RfcPackage.RECORD_META_DATA__FIELD_META_DATA:
				return ((InternalEList<?>)getFieldMetaData()).basicRemove(otherEnd, msgs);
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
			case RfcPackage.RECORD_META_DATA__FIELD_META_DATA:
				return getFieldMetaData();
			case RfcPackage.RECORD_META_DATA__NAME:
				return getName();
			case RfcPackage.RECORD_META_DATA__RECORD_FIELD_META_DATA:
				return getRecordFieldMetaData();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case RfcPackage.RECORD_META_DATA__FIELD_META_DATA:
				getFieldMetaData().clear();
				getFieldMetaData().addAll((Collection<? extends FieldMetaData>)newValue);
				return;
			case RfcPackage.RECORD_META_DATA__NAME:
				setName((String)newValue);
				return;
			case RfcPackage.RECORD_META_DATA__RECORD_FIELD_META_DATA:
				setRecordFieldMetaData((List<FieldMetaData>)newValue);
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
			case RfcPackage.RECORD_META_DATA__FIELD_META_DATA:
				getFieldMetaData().clear();
				return;
			case RfcPackage.RECORD_META_DATA__NAME:
				setName(NAME_EDEFAULT);
				return;
			case RfcPackage.RECORD_META_DATA__RECORD_FIELD_META_DATA:
				setRecordFieldMetaData((List<FieldMetaData>)null);
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
			case RfcPackage.RECORD_META_DATA__FIELD_META_DATA:
				return fieldMetaData != null && !fieldMetaData.isEmpty();
			case RfcPackage.RECORD_META_DATA__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case RfcPackage.RECORD_META_DATA__RECORD_FIELD_META_DATA:
				return getRecordFieldMetaData() != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

} //RecordMetaDataImpl
