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

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;
import org.fusesource.camel.component.sap.model.rfc.DataType;
import org.fusesource.camel.component.sap.model.rfc.FieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RecordMetaData;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Field Meta Data</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getFieldMetaData <em>Field Meta Data</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getType <em>Type</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getByteLength <em>Byte Length</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getByteOffset <em>Byte Offset</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getUnicodeByteLength <em>Unicode Byte Length</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getUnicodeByteOffset <em>Unicode Byte Offset</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getDecimals <em>Decimals</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl#getRecordMetaData <em>Record Meta Data</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FieldMetaDataImpl extends EObjectImpl implements FieldMetaData {
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
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final DataType TYPE_EDEFAULT = DataType.CHAR;

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected DataType type = TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getByteLength() <em>Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getByteLength()
	 * @generated
	 * @ordered
	 */
	protected static final int BYTE_LENGTH_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getByteLength() <em>Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getByteLength()
	 * @generated
	 * @ordered
	 */
	protected int byteLength = BYTE_LENGTH_EDEFAULT;

	/**
	 * The default value of the '{@link #getByteOffset() <em>Byte Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getByteOffset()
	 * @generated
	 * @ordered
	 */
	protected static final int BYTE_OFFSET_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getByteOffset() <em>Byte Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getByteOffset()
	 * @generated
	 * @ordered
	 */
	protected int byteOffset = BYTE_OFFSET_EDEFAULT;

	/**
	 * The default value of the '{@link #getUnicodeByteLength() <em>Unicode Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUnicodeByteLength()
	 * @generated
	 * @ordered
	 */
	protected static final int UNICODE_BYTE_LENGTH_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getUnicodeByteLength() <em>Unicode Byte Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUnicodeByteLength()
	 * @generated
	 * @ordered
	 */
	protected int unicodeByteLength = UNICODE_BYTE_LENGTH_EDEFAULT;

	/**
	 * The default value of the '{@link #getUnicodeByteOffset() <em>Unicode Byte Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUnicodeByteOffset()
	 * @generated
	 * @ordered
	 */
	protected static final int UNICODE_BYTE_OFFSET_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getUnicodeByteOffset() <em>Unicode Byte Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUnicodeByteOffset()
	 * @generated
	 * @ordered
	 */
	protected int unicodeByteOffset = UNICODE_BYTE_OFFSET_EDEFAULT;

	/**
	 * The default value of the '{@link #getDecimals() <em>Decimals</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDecimals()
	 * @generated
	 * @ordered
	 */
	protected static final int DECIMALS_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getDecimals() <em>Decimals</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDecimals()
	 * @generated
	 * @ordered
	 */
	protected int decimals = DECIMALS_EDEFAULT;

	/**
	 * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String DESCRIPTION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected String description = DESCRIPTION_EDEFAULT;

	/**
	 * The cached value of the '{@link #getRecordMetaData() <em>Record Meta Data</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRecordMetaData()
	 * @generated
	 * @ordered
	 */
	protected RecordMetaData recordMetaData;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FieldMetaDataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return RfcPackage.Literals.FIELD_META_DATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<FieldMetaData> getFieldMetaData() {
		if (fieldMetaData == null) {
			fieldMetaData = new EObjectContainmentEList<FieldMetaData>(FieldMetaData.class, this, RfcPackage.FIELD_META_DATA__FIELD_META_DATA);
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
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setType(DataType newType) {
		DataType oldType = type;
		type = newType == null ? TYPE_EDEFAULT : newType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__TYPE, oldType, type));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getByteLength() {
		return byteLength;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setByteLength(int newByteLength) {
		int oldByteLength = byteLength;
		byteLength = newByteLength;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__BYTE_LENGTH, oldByteLength, byteLength));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getByteOffset() {
		return byteOffset;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setByteOffset(int newByteOffset) {
		int oldByteOffset = byteOffset;
		byteOffset = newByteOffset;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__BYTE_OFFSET, oldByteOffset, byteOffset));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getUnicodeByteLength() {
		return unicodeByteLength;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUnicodeByteLength(int newUnicodeByteLength) {
		int oldUnicodeByteLength = unicodeByteLength;
		unicodeByteLength = newUnicodeByteLength;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__UNICODE_BYTE_LENGTH, oldUnicodeByteLength, unicodeByteLength));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getUnicodeByteOffset() {
		return unicodeByteOffset;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUnicodeByteOffset(int newUnicodeByteOffset) {
		int oldUnicodeByteOffset = unicodeByteOffset;
		unicodeByteOffset = newUnicodeByteOffset;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__UNICODE_BYTE_OFFSET, oldUnicodeByteOffset, unicodeByteOffset));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getDecimals() {
		return decimals;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDecimals(int newDecimals) {
		int oldDecimals = decimals;
		decimals = newDecimals;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__DECIMALS, oldDecimals, decimals));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDescription(String newDescription) {
		String oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RecordMetaData getRecordMetaData() {
		if (recordMetaData != null && recordMetaData.eIsProxy()) {
			InternalEObject oldRecordMetaData = (InternalEObject)recordMetaData;
			recordMetaData = (RecordMetaData)eResolveProxy(oldRecordMetaData);
			if (recordMetaData != oldRecordMetaData) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, RfcPackage.FIELD_META_DATA__RECORD_META_DATA, oldRecordMetaData, recordMetaData));
			}
		}
		return recordMetaData;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RecordMetaData basicGetRecordMetaData() {
		return recordMetaData;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRecordMetaData(RecordMetaData newRecordMetaData) {
		RecordMetaData oldRecordMetaData = recordMetaData;
		recordMetaData = newRecordMetaData;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, RfcPackage.FIELD_META_DATA__RECORD_META_DATA, oldRecordMetaData, recordMetaData));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case RfcPackage.FIELD_META_DATA__FIELD_META_DATA:
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
			case RfcPackage.FIELD_META_DATA__FIELD_META_DATA:
				return getFieldMetaData();
			case RfcPackage.FIELD_META_DATA__NAME:
				return getName();
			case RfcPackage.FIELD_META_DATA__TYPE:
				return getType();
			case RfcPackage.FIELD_META_DATA__BYTE_LENGTH:
				return getByteLength();
			case RfcPackage.FIELD_META_DATA__BYTE_OFFSET:
				return getByteOffset();
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_LENGTH:
				return getUnicodeByteLength();
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_OFFSET:
				return getUnicodeByteOffset();
			case RfcPackage.FIELD_META_DATA__DECIMALS:
				return getDecimals();
			case RfcPackage.FIELD_META_DATA__DESCRIPTION:
				return getDescription();
			case RfcPackage.FIELD_META_DATA__RECORD_META_DATA:
				if (resolve) return getRecordMetaData();
				return basicGetRecordMetaData();
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
			case RfcPackage.FIELD_META_DATA__FIELD_META_DATA:
				getFieldMetaData().clear();
				getFieldMetaData().addAll((Collection<? extends FieldMetaData>)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__NAME:
				setName((String)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__TYPE:
				setType((DataType)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__BYTE_LENGTH:
				setByteLength((Integer)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__BYTE_OFFSET:
				setByteOffset((Integer)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_LENGTH:
				setUnicodeByteLength((Integer)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_OFFSET:
				setUnicodeByteOffset((Integer)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__DECIMALS:
				setDecimals((Integer)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case RfcPackage.FIELD_META_DATA__RECORD_META_DATA:
				setRecordMetaData((RecordMetaData)newValue);
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
			case RfcPackage.FIELD_META_DATA__FIELD_META_DATA:
				getFieldMetaData().clear();
				return;
			case RfcPackage.FIELD_META_DATA__NAME:
				setName(NAME_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__TYPE:
				setType(TYPE_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__BYTE_LENGTH:
				setByteLength(BYTE_LENGTH_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__BYTE_OFFSET:
				setByteOffset(BYTE_OFFSET_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_LENGTH:
				setUnicodeByteLength(UNICODE_BYTE_LENGTH_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_OFFSET:
				setUnicodeByteOffset(UNICODE_BYTE_OFFSET_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__DECIMALS:
				setDecimals(DECIMALS_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case RfcPackage.FIELD_META_DATA__RECORD_META_DATA:
				setRecordMetaData((RecordMetaData)null);
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
			case RfcPackage.FIELD_META_DATA__FIELD_META_DATA:
				return fieldMetaData != null && !fieldMetaData.isEmpty();
			case RfcPackage.FIELD_META_DATA__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case RfcPackage.FIELD_META_DATA__TYPE:
				return type != TYPE_EDEFAULT;
			case RfcPackage.FIELD_META_DATA__BYTE_LENGTH:
				return byteLength != BYTE_LENGTH_EDEFAULT;
			case RfcPackage.FIELD_META_DATA__BYTE_OFFSET:
				return byteOffset != BYTE_OFFSET_EDEFAULT;
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_LENGTH:
				return unicodeByteLength != UNICODE_BYTE_LENGTH_EDEFAULT;
			case RfcPackage.FIELD_META_DATA__UNICODE_BYTE_OFFSET:
				return unicodeByteOffset != UNICODE_BYTE_OFFSET_EDEFAULT;
			case RfcPackage.FIELD_META_DATA__DECIMALS:
				return decimals != DECIMALS_EDEFAULT;
			case RfcPackage.FIELD_META_DATA__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case RfcPackage.FIELD_META_DATA__RECORD_META_DATA:
				return recordMetaData != null;
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
		result.append(", type: ");
		result.append(type);
		result.append(", byteLength: ");
		result.append(byteLength);
		result.append(", byteOffset: ");
		result.append(byteOffset);
		result.append(", unicodeByteLength: ");
		result.append(unicodeByteLength);
		result.append(", unicodeByteOffset: ");
		result.append(unicodeByteOffset);
		result.append(", decimals: ");
		result.append(decimals);
		result.append(", description: ");
		result.append(description);
		result.append(')');
		return result.toString();
	}

} //FieldMetaDataImpl
