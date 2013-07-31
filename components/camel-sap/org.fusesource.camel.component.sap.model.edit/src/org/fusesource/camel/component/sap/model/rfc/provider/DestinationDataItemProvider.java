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
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;

/**
 * This is the item provider adapter for a {@link org.fusesource.camel.component.sap.model.rfc.DestinationData} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class DestinationDataItemProvider
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
	public DestinationDataItemProvider(AdapterFactory adapterFactory) {
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

			addAliasUserPropertyDescriptor(object);
			addAshostPropertyDescriptor(object);
			addAuthTypePropertyDescriptor(object);
			addClientPropertyDescriptor(object);
			addCodepagePropertyDescriptor(object);
			addCpicTracePropertyDescriptor(object);
			addDenyInitialPasswordPropertyDescriptor(object);
			addExpirationPeriodPropertyDescriptor(object);
			addExpirationTimePropertyDescriptor(object);
			addGetsso2PropertyDescriptor(object);
			addGroupPropertyDescriptor(object);
			addGwhostPropertyDescriptor(object);
			addGwservPropertyDescriptor(object);
			addLangPropertyDescriptor(object);
			addLcheckPropertyDescriptor(object);
			addMaxGetTimePropertyDescriptor(object);
			addMshostPropertyDescriptor(object);
			addMsservPropertyDescriptor(object);
			addMysapsso2PropertyDescriptor(object);
			addPasswdPropertyDescriptor(object);
			addPasswordPropertyDescriptor(object);
			addPcsPropertyDescriptor(object);
			addPeakLimitPropertyDescriptor(object);
			addPingOnCreatePropertyDescriptor(object);
			addPoolCapacityPropertyDescriptor(object);
			addR3namePropertyDescriptor(object);
			addRepositoryDestPropertyDescriptor(object);
			addRepositoryPasswdPropertyDescriptor(object);
			addRepositoryRoundtripOptimizationPropertyDescriptor(object);
			addRepositorySncPropertyDescriptor(object);
			addRepositoryUserPropertyDescriptor(object);
			addSaprouterPropertyDescriptor(object);
			addSncLibraryPropertyDescriptor(object);
			addSncModePropertyDescriptor(object);
			addSncMynamePropertyDescriptor(object);
			addSncPartnernamePropertyDescriptor(object);
			addSncQopPropertyDescriptor(object);
			addSysnrPropertyDescriptor(object);
			addTphostPropertyDescriptor(object);
			addTpnamePropertyDescriptor(object);
			addTracePropertyDescriptor(object);
			addTypePropertyDescriptor(object);
			addUserNamePropertyDescriptor(object);
			addUserPropertyDescriptor(object);
			addUserIdPropertyDescriptor(object);
			addUseSapguiPropertyDescriptor(object);
			addX509certPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Alias User feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addAliasUserPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_aliasUser_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_aliasUser_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__ALIAS_USER,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Ashost feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addAshostPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_ashost_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_ashost_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__ASHOST,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Auth Type feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addAuthTypePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_authType_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_authType_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__AUTH_TYPE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Client feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addClientPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_client_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_client_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__CLIENT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Codepage feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addCodepagePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_codepage_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_codepage_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__CODEPAGE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Cpic Trace feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addCpicTracePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_cpicTrace_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_cpicTrace_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__CPIC_TRACE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Deny Initial Password feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDenyInitialPasswordPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_denyInitialPassword_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_denyInitialPassword_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__DENY_INITIAL_PASSWORD,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Expiration Period feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addExpirationPeriodPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_expirationPeriod_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_expirationPeriod_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__EXPIRATION_PERIOD,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Expiration Time feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addExpirationTimePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_expirationTime_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_expirationTime_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__EXPIRATION_TIME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Getsso2 feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addGetsso2PropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_getsso2_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_getsso2_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__GETSSO2,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Group feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addGroupPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_group_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_group_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__GROUP,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Gwhost feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addGwhostPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_gwhost_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_gwhost_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__GWHOST,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Gwserv feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addGwservPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_gwserv_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_gwserv_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__GWSERV,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Lang feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addLangPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_lang_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_lang_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__LANG,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Lcheck feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addLcheckPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_lcheck_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_lcheck_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__LCHECK,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Max Get Time feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMaxGetTimePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_maxGetTime_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_maxGetTime_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__MAX_GET_TIME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Mshost feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMshostPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_mshost_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_mshost_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__MSHOST,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Msserv feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMsservPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_msserv_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_msserv_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__MSSERV,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Mysapsso2 feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMysapsso2PropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_mysapsso2_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_mysapsso2_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__MYSAPSSO2,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Passwd feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addPasswdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_passwd_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_passwd_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__PASSWD,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Password feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addPasswordPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_password_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_password_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__PASSWORD,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Pcs feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addPcsPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_pcs_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_pcs_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__PCS,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Peak Limit feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addPeakLimitPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_peakLimit_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_peakLimit_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__PEAK_LIMIT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Ping On Create feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addPingOnCreatePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_pingOnCreate_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_pingOnCreate_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__PING_ON_CREATE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Pool Capacity feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addPoolCapacityPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_poolCapacity_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_poolCapacity_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__POOL_CAPACITY,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the R3name feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addR3namePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_r3name_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_r3name_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__R3NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Repository Dest feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRepositoryDestPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_repositoryDest_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_repositoryDest_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__REPOSITORY_DEST,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Repository Passwd feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRepositoryPasswdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_repositoryPasswd_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_repositoryPasswd_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__REPOSITORY_PASSWD,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Repository Roundtrip Optimization feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRepositoryRoundtripOptimizationPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_repositoryRoundtripOptimization_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_repositoryRoundtripOptimization_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Repository Snc feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRepositorySncPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_repositorySnc_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_repositorySnc_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__REPOSITORY_SNC,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Repository User feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRepositoryUserPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_repositoryUser_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_repositoryUser_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__REPOSITORY_USER,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Saprouter feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSaprouterPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_saprouter_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_saprouter_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SAPROUTER,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Snc Library feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSncLibraryPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_sncLibrary_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_sncLibrary_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SNC_LIBRARY,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Snc Mode feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSncModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_sncMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_sncMode_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SNC_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Snc Myname feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSncMynamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_sncMyname_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_sncMyname_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SNC_MYNAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Snc Partnername feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSncPartnernamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_sncPartnername_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_sncPartnername_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SNC_PARTNERNAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Snc Qop feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSncQopPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_sncQop_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_sncQop_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SNC_QOP,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Sysnr feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSysnrPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_sysnr_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_sysnr_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__SYSNR,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Tphost feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTphostPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_tphost_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_tphost_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__TPHOST,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Tpname feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTpnamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_tpname_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_tpname_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__TPNAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Trace feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTracePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_trace_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_trace_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__TRACE,
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
				 getString("_UI_DestinationData_type_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_type_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__TYPE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the User Name feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUserNamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_userName_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_userName_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__USER_NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the User feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUserPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_user_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_user_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__USER,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the User Id feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUserIdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_userId_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_userId_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__USER_ID,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Use Sapgui feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUseSapguiPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_useSapgui_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_useSapgui_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__USE_SAPGUI,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the X50 9cert feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addX509certPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_DestinationData_x509cert_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_DestinationData_x509cert_feature", "_UI_DestinationData_type"),
				 RfcPackage.Literals.DESTINATION_DATA__X509CERT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
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
			childrenFeatures.add(RfcPackage.Literals.DESTINATION_DATA__ENTRIES);
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
	 * This returns DestinationData.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/DestinationData"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((DestinationData)object).getR3name();
		return label == null || label.length() == 0 ?
			getString("_UI_DestinationData_type") :
			getString("_UI_DestinationData_type") + " " + label;
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

		switch (notification.getFeatureID(DestinationData.class)) {
			case RfcPackage.DESTINATION_DATA__ALIAS_USER:
			case RfcPackage.DESTINATION_DATA__ASHOST:
			case RfcPackage.DESTINATION_DATA__AUTH_TYPE:
			case RfcPackage.DESTINATION_DATA__CLIENT:
			case RfcPackage.DESTINATION_DATA__CODEPAGE:
			case RfcPackage.DESTINATION_DATA__CPIC_TRACE:
			case RfcPackage.DESTINATION_DATA__DENY_INITIAL_PASSWORD:
			case RfcPackage.DESTINATION_DATA__EXPIRATION_PERIOD:
			case RfcPackage.DESTINATION_DATA__EXPIRATION_TIME:
			case RfcPackage.DESTINATION_DATA__GETSSO2:
			case RfcPackage.DESTINATION_DATA__GROUP:
			case RfcPackage.DESTINATION_DATA__GWHOST:
			case RfcPackage.DESTINATION_DATA__GWSERV:
			case RfcPackage.DESTINATION_DATA__LANG:
			case RfcPackage.DESTINATION_DATA__LCHECK:
			case RfcPackage.DESTINATION_DATA__MAX_GET_TIME:
			case RfcPackage.DESTINATION_DATA__MSHOST:
			case RfcPackage.DESTINATION_DATA__MSSERV:
			case RfcPackage.DESTINATION_DATA__MYSAPSSO2:
			case RfcPackage.DESTINATION_DATA__PASSWD:
			case RfcPackage.DESTINATION_DATA__PASSWORD:
			case RfcPackage.DESTINATION_DATA__PCS:
			case RfcPackage.DESTINATION_DATA__PEAK_LIMIT:
			case RfcPackage.DESTINATION_DATA__PING_ON_CREATE:
			case RfcPackage.DESTINATION_DATA__POOL_CAPACITY:
			case RfcPackage.DESTINATION_DATA__R3NAME:
			case RfcPackage.DESTINATION_DATA__REPOSITORY_DEST:
			case RfcPackage.DESTINATION_DATA__REPOSITORY_PASSWD:
			case RfcPackage.DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION:
			case RfcPackage.DESTINATION_DATA__REPOSITORY_SNC:
			case RfcPackage.DESTINATION_DATA__REPOSITORY_USER:
			case RfcPackage.DESTINATION_DATA__SAPROUTER:
			case RfcPackage.DESTINATION_DATA__SNC_LIBRARY:
			case RfcPackage.DESTINATION_DATA__SNC_MODE:
			case RfcPackage.DESTINATION_DATA__SNC_MYNAME:
			case RfcPackage.DESTINATION_DATA__SNC_PARTNERNAME:
			case RfcPackage.DESTINATION_DATA__SNC_QOP:
			case RfcPackage.DESTINATION_DATA__SYSNR:
			case RfcPackage.DESTINATION_DATA__TPHOST:
			case RfcPackage.DESTINATION_DATA__TPNAME:
			case RfcPackage.DESTINATION_DATA__TRACE:
			case RfcPackage.DESTINATION_DATA__TYPE:
			case RfcPackage.DESTINATION_DATA__USER_NAME:
			case RfcPackage.DESTINATION_DATA__USER:
			case RfcPackage.DESTINATION_DATA__USER_ID:
			case RfcPackage.DESTINATION_DATA__USE_SAPGUI:
			case RfcPackage.DESTINATION_DATA__X509CERT:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
			case RfcPackage.DESTINATION_DATA__ENTRIES:
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
				(RfcPackage.Literals.DESTINATION_DATA__ENTRIES,
				 RfcFactory.eINSTANCE.create(RfcPackage.Literals.DESTINATION_DATA_ENTRY)));
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
