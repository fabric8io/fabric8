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
package org.fusesource.camel.component.sap.model.rfc.provider;


import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.emf.edit.provider.ViewerNotification;
import org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;

/**
 * This is the item provider adapter for a {@link org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class ListFieldMetaDataItemProvider
	extends ItemProviderAdapter
	implements
		IEditingDomainItemProvider,
		IStructuredItemContentProvider,
		ITreeItemContentProvider,
		IItemLabelProvider,
		IItemPropertySource {
	/**
	 * This constructs an instance from a factory and a notifier.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ListFieldMetaDataItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addNamePropertyDescriptor(object);
			addTypePropertyDescriptor(object);
			addByteLengthPropertyDescriptor(object);
			addUnicodeByteLengthPropertyDescriptor(object);
			addDecimalsPropertyDescriptor(object);
			addDefaultsPropertyDescriptor(object);
			addDescriptionPropertyDescriptor(object);
			addImportPropertyDescriptor(object);
			addChangingPropertyDescriptor(object);
			addExportPropertyDescriptor(object);
			addExceptionPropertyDescriptor(object);
			addOptionalPropertyDescriptor(object);
			addRecordMetaDataPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Name feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addNamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_name_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_name_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Type feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTypePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_type_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_type_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__TYPE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Byte Length feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addByteLengthPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_byteLength_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_byteLength_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__BYTE_LENGTH,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Unicode Byte Length feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUnicodeByteLengthPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_unicodeByteLength_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_unicodeByteLength_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__UNICODE_BYTE_LENGTH,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Decimals feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDecimalsPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_decimals_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_decimals_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__DECIMALS,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Defaults feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDefaultsPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_defaults_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_defaults_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__DEFAULTS,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Description feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDescriptionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_description_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_description_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__DESCRIPTION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Import feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addImportPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_import_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_import_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__IMPORT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Changing feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addChangingPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_changing_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_changing_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__CHANGING,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Export feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addExportPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_export_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_export_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__EXPORT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Exception feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addExceptionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_exception_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_exception_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__EXCEPTION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Optional feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addOptionalPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_optional_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_optional_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__OPTIONAL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Record Meta Data feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRecordMetaDataPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ListFieldMetaData_recordMetaData_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ListFieldMetaData_recordMetaData_feature", "_UI_ListFieldMetaData_type"),
				 RfcPackage.Literals.LIST_FIELD_META_DATA__RECORD_META_DATA,
				 true,
				 false,
				 true,
				 null,
				 null,
				 null));
	}

	/**
	 * This specifies how to implement {@link #getChildren} and is used to deduce an appropriate feature for an
	 * {@link org.eclipse.emf.edit.command.AddCommand}, {@link org.eclipse.emf.edit.command.RemoveCommand} or
	 * {@link org.eclipse.emf.edit.command.MoveCommand} in {@link #createCommand}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Collection<? extends EStructuralFeature> getChildrenFeatures(Object object) {
		if (childrenFeatures == null) {
			super.getChildrenFeatures(object);
			childrenFeatures.add(RfcPackage.Literals.LIST_FIELD_META_DATA__FIELD_META_DATA);
		}
		return childrenFeatures;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EStructuralFeature getChildFeature(Object object, Object child) {
		// Check the type of the specified child object and return the proper feature to use for
		// adding (see {@link AddCommand}) it as a child.

		return super.getChildFeature(object, child);
	}

	/**
	 * This returns ListFieldMetaData.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/ListFieldMetaData"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((ListFieldMetaData)object).getName();
		return label == null || label.length() == 0 ?
			getString("_UI_ListFieldMetaData_type") :
			getString("_UI_ListFieldMetaData_type") + " " + label;
	}

	/**
	 * This handles model notifications by calling {@link #updateChildren} to update any cached
	 * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void notifyChanged(Notification notification) {
		updateChildren(notification);

		switch (notification.getFeatureID(ListFieldMetaData.class)) {
			case RfcPackage.LIST_FIELD_META_DATA__NAME:
			case RfcPackage.LIST_FIELD_META_DATA__TYPE:
			case RfcPackage.LIST_FIELD_META_DATA__BYTE_LENGTH:
			case RfcPackage.LIST_FIELD_META_DATA__UNICODE_BYTE_LENGTH:
			case RfcPackage.LIST_FIELD_META_DATA__DECIMALS:
			case RfcPackage.LIST_FIELD_META_DATA__DEFAULTS:
			case RfcPackage.LIST_FIELD_META_DATA__DESCRIPTION:
			case RfcPackage.LIST_FIELD_META_DATA__IMPORT:
			case RfcPackage.LIST_FIELD_META_DATA__CHANGING:
			case RfcPackage.LIST_FIELD_META_DATA__EXPORT:
			case RfcPackage.LIST_FIELD_META_DATA__EXCEPTION:
			case RfcPackage.LIST_FIELD_META_DATA__OPTIONAL:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
			case RfcPackage.LIST_FIELD_META_DATA__FIELD_META_DATA:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), true, false));
				return;
		}
		super.notifyChanged(notification);
	}

	/**
	 * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children
	 * that can be created under this object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);

		newChildDescriptors.add
			(createChildParameter
				(RfcPackage.Literals.LIST_FIELD_META_DATA__FIELD_META_DATA,
				 RfcFactory.eINSTANCE.createFieldMetaData()));
	}

	/**
	 * Return the resource locator for this item provider's resources.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ResourceLocator getResourceLocator() {
		return SAPRFCEditPlugin.INSTANCE;
	}

}
