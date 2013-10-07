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

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.fusesource.camel.component.sap.model.rfc.AbapException;
import org.fusesource.camel.component.sap.model.rfc.DataType;
import org.fusesource.camel.component.sap.model.rfc.Destination;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.FieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.FunctionTemplate;
import org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RecordMetaData;
import org.fusesource.camel.component.sap.model.rfc.RepositoryData;
import org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.Server;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.ServerDataStore;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class RfcPackageImpl extends EPackageImpl implements RfcPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass destinationEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass rfcEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass tableEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass structureEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass destinationDataEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass destinationDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass destinationDataStoreEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass destinationDataStoreEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverDataEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverDataStoreEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverDataStoreEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass functionTemplateEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass recordMetaDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass fieldMetaDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass listFieldMetaDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass abapExceptionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass respositoryDataEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass repositoryDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass repositoryDataStoreEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass respositoryDataStoreEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum dataTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType parameterListEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType fieldListEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType abapExceptionListEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType functionTemplateMapEDataType = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private RfcPackageImpl() {
		super(eNS_URI, RfcFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 * 
	 * <p>This method is used to initialize {@link RfcPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static RfcPackage init() {
		if (isInited) return (RfcPackage)EPackage.Registry.INSTANCE.getEPackage(RfcPackage.eNS_URI);

		// Obtain or create and register package
		RfcPackageImpl theRfcPackage = (RfcPackageImpl)(EPackage.Registry.INSTANCE.get(eNS_URI) instanceof RfcPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new RfcPackageImpl());

		isInited = true;

		// Create package meta-data objects
		theRfcPackage.createPackageContents();

		// Initialize created meta-data
		theRfcPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theRfcPackage.freeze();

  
		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(RfcPackage.eNS_URI, theRfcPackage);
		return theRfcPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDestination() {
		return destinationEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestination_Name() {
		return (EAttribute)destinationEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestination_RepositoryName() {
		return (EAttribute)destinationEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDestination_Rfcs() {
		return (EReference)destinationEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRFC() {
		return rfcEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRFC_Name() {
		return (EAttribute)rfcEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRFC_Group() {
		return (EAttribute)rfcEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRFC_Description() {
		return (EAttribute)rfcEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRFC_Request() {
		return (EReference)rfcEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRFC_Response() {
		return (EReference)rfcEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRFC_Destination() {
		return (EReference)rfcEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getTable() {
		return tableEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getStructure() {
		return structureEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDestinationDataEntry() {
		return destinationDataEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationDataEntry_Key() {
		return (EAttribute)destinationDataEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationDataEntry_Value() {
		return (EAttribute)destinationDataEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDestinationData() {
		return destinationDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDestinationData_Entries() {
		return (EReference)destinationDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_AliasUser() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Ashost() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_AuthType() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Client() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Codepage() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_CpicTrace() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_DenyInitialPassword() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_ExpirationPeriod() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_ExpirationTime() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Getsso2() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Group() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Gwhost() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Gwserv() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Lang() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Lcheck() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_MaxGetTime() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(16);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Mshost() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(17);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Msserv() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(18);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Mysapsso2() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(19);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Passwd() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(20);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Password() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(21);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Pcs() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(22);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_PeakLimit() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(23);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_PingOnCreate() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(24);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_PoolCapacity() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(25);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_R3name() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(26);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_RepositoryDest() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(27);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_RepositoryPasswd() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(28);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_RepositoryRoundtripOptimization() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(29);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_RepositorySnc() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(30);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_RepositoryUser() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(31);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Saprouter() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(32);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_SncLibrary() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(33);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_SncMode() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(34);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_SncMyname() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(35);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_SncPartnername() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(36);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_SncQop() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(37);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Sysnr() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(38);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Tphost() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(39);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Tpname() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(40);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Trace() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(41);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_Type() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(42);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_UserName() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(43);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_User() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(44);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_UserId() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(45);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_UseSapgui() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(46);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationData_X509cert() {
		return (EAttribute)destinationDataEClass.getEStructuralFeatures().get(47);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDestinationDataStoreEntry() {
		return destinationDataStoreEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDestinationDataStoreEntry_Key() {
		return (EAttribute)destinationDataStoreEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDestinationDataStoreEntry_Value() {
		return (EReference)destinationDataStoreEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDestinationDataStore() {
		return destinationDataStoreEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDestinationDataStore_Entries() {
		return (EReference)destinationDataStoreEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getServer() {
		return serverEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServer_Name() {
		return (EAttribute)serverEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getServerDataEntry() {
		return serverDataEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerDataEntry_Key() {
		return (EAttribute)serverDataEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerDataEntry_Value() {
		return (EAttribute)serverDataEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getServerData() {
		return serverDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getServerData_Entries() {
		return (EReference)serverDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_Gwhost() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_Gwserv() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_Progid() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_ConnectionCount() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_Saprouter() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_MaxStartUpDelay() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_RepositoryDestination() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_RepositoryMap() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_Trace() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_WorkerThreadCount() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_WorkerThreadMinCount() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_SncMode() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_SncQop() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_SncMyname() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerData_SncLib() {
		return (EAttribute)serverDataEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getServerDataStoreEntry() {
		return serverDataStoreEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getServerDataStoreEntry_Key() {
		return (EAttribute)serverDataStoreEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getServerDataStoreEntry_Value() {
		return (EReference)serverDataStoreEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getServerDataStore() {
		return serverDataStoreEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getServerDataStore_Entries() {
		return (EReference)serverDataStoreEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getFunctionTemplate() {
		return functionTemplateEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFunctionTemplate_Imports() {
		return (EReference)functionTemplateEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFunctionTemplate_Exports() {
		return (EReference)functionTemplateEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFunctionTemplate_Changing() {
		return (EReference)functionTemplateEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFunctionTemplate_Tables() {
		return (EReference)functionTemplateEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFunctionTemplate_Exceptions() {
		return (EReference)functionTemplateEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFunctionTemplate_ImportParameterList() {
		return (EAttribute)functionTemplateEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFunctionTemplate_ExportParameterList() {
		return (EAttribute)functionTemplateEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFunctionTemplate_ChangingParameterList() {
		return (EAttribute)functionTemplateEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFunctionTemplate_TableParameterList() {
		return (EAttribute)functionTemplateEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFunctionTemplate_ExceptionList() {
		return (EAttribute)functionTemplateEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRecordMetaData() {
		return recordMetaDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRecordMetaData_FieldMetaData() {
		return (EReference)recordMetaDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRecordMetaData_Name() {
		return (EAttribute)recordMetaDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRecordMetaData_RecordFieldMetaData() {
		return (EAttribute)recordMetaDataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getFieldMetaData() {
		return fieldMetaDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFieldMetaData_FieldMetaData() {
		return (EReference)fieldMetaDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_Name() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_Type() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_ByteLength() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_ByteOffset() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_UnicodeByteLength() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_UnicodeByteOffset() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_Decimals() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFieldMetaData_Description() {
		return (EAttribute)fieldMetaDataEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFieldMetaData_RecordMetaData() {
		return (EReference)fieldMetaDataEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getListFieldMetaData() {
		return listFieldMetaDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getListFieldMetaData_FieldMetaData() {
		return (EReference)listFieldMetaDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Name() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Type() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_ByteLength() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_UnicodeByteLength() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Decimals() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Defaults() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Description() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Import() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Changing() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Export() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Exception() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListFieldMetaData_Optional() {
		return (EAttribute)listFieldMetaDataEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getListFieldMetaData_RecordMetaData() {
		return (EReference)listFieldMetaDataEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getAbapException() {
		return abapExceptionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAbapException_Key() {
		return (EAttribute)abapExceptionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAbapException_Message() {
		return (EAttribute)abapExceptionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRespositoryDataEntry() {
		return respositoryDataEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRespositoryDataEntry_Key() {
		return (EAttribute)respositoryDataEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRespositoryDataEntry_Value() {
		return (EReference)respositoryDataEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRepositoryData() {
		return repositoryDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRepositoryData_Entries() {
		return (EReference)repositoryDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRepositoryData_FunctionTemplates() {
		return (EAttribute)repositoryDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRepositoryDataStore() {
		return repositoryDataStoreEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRepositoryDataStore_Entries() {
		return (EReference)repositoryDataStoreEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRespositoryDataStoreEntry() {
		return respositoryDataStoreEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRespositoryDataStoreEntry_Key() {
		return (EAttribute)respositoryDataStoreEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRespositoryDataStoreEntry_Value() {
		return (EReference)respositoryDataStoreEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getDataType() {
		return dataTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getParameterList() {
		return parameterListEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getFieldList() {
		return fieldListEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getAbapExceptionList() {
		return abapExceptionListEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getFunctionTemplateMap() {
		return functionTemplateMapEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RfcFactory getRfcFactory() {
		return (RfcFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		destinationEClass = createEClass(DESTINATION);
		createEAttribute(destinationEClass, DESTINATION__NAME);
		createEAttribute(destinationEClass, DESTINATION__REPOSITORY_NAME);
		createEReference(destinationEClass, DESTINATION__RFCS);

		rfcEClass = createEClass(RFC);
		createEAttribute(rfcEClass, RFC__NAME);
		createEAttribute(rfcEClass, RFC__GROUP);
		createEAttribute(rfcEClass, RFC__DESCRIPTION);
		createEReference(rfcEClass, RFC__REQUEST);
		createEReference(rfcEClass, RFC__RESPONSE);
		createEReference(rfcEClass, RFC__DESTINATION);

		tableEClass = createEClass(TABLE);

		structureEClass = createEClass(STRUCTURE);

		destinationDataEntryEClass = createEClass(DESTINATION_DATA_ENTRY);
		createEAttribute(destinationDataEntryEClass, DESTINATION_DATA_ENTRY__KEY);
		createEAttribute(destinationDataEntryEClass, DESTINATION_DATA_ENTRY__VALUE);

		destinationDataEClass = createEClass(DESTINATION_DATA);
		createEReference(destinationDataEClass, DESTINATION_DATA__ENTRIES);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__ALIAS_USER);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__ASHOST);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__AUTH_TYPE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__CLIENT);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__CODEPAGE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__CPIC_TRACE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__DENY_INITIAL_PASSWORD);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__EXPIRATION_PERIOD);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__EXPIRATION_TIME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__GETSSO2);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__GROUP);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__GWHOST);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__GWSERV);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__LANG);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__LCHECK);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__MAX_GET_TIME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__MSHOST);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__MSSERV);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__MYSAPSSO2);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__PASSWD);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__PASSWORD);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__PCS);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__PEAK_LIMIT);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__PING_ON_CREATE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__POOL_CAPACITY);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__R3NAME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__REPOSITORY_DEST);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__REPOSITORY_PASSWD);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__REPOSITORY_ROUNDTRIP_OPTIMIZATION);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__REPOSITORY_SNC);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__REPOSITORY_USER);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SAPROUTER);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SNC_LIBRARY);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SNC_MODE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SNC_MYNAME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SNC_PARTNERNAME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SNC_QOP);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__SYSNR);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__TPHOST);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__TPNAME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__TRACE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__TYPE);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__USER_NAME);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__USER);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__USER_ID);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__USE_SAPGUI);
		createEAttribute(destinationDataEClass, DESTINATION_DATA__X509CERT);

		destinationDataStoreEntryEClass = createEClass(DESTINATION_DATA_STORE_ENTRY);
		createEAttribute(destinationDataStoreEntryEClass, DESTINATION_DATA_STORE_ENTRY__KEY);
		createEReference(destinationDataStoreEntryEClass, DESTINATION_DATA_STORE_ENTRY__VALUE);

		destinationDataStoreEClass = createEClass(DESTINATION_DATA_STORE);
		createEReference(destinationDataStoreEClass, DESTINATION_DATA_STORE__ENTRIES);

		serverEClass = createEClass(SERVER);
		createEAttribute(serverEClass, SERVER__NAME);

		serverDataEntryEClass = createEClass(SERVER_DATA_ENTRY);
		createEAttribute(serverDataEntryEClass, SERVER_DATA_ENTRY__KEY);
		createEAttribute(serverDataEntryEClass, SERVER_DATA_ENTRY__VALUE);

		serverDataEClass = createEClass(SERVER_DATA);
		createEReference(serverDataEClass, SERVER_DATA__ENTRIES);
		createEAttribute(serverDataEClass, SERVER_DATA__GWHOST);
		createEAttribute(serverDataEClass, SERVER_DATA__GWSERV);
		createEAttribute(serverDataEClass, SERVER_DATA__PROGID);
		createEAttribute(serverDataEClass, SERVER_DATA__CONNECTION_COUNT);
		createEAttribute(serverDataEClass, SERVER_DATA__SAPROUTER);
		createEAttribute(serverDataEClass, SERVER_DATA__MAX_START_UP_DELAY);
		createEAttribute(serverDataEClass, SERVER_DATA__REPOSITORY_DESTINATION);
		createEAttribute(serverDataEClass, SERVER_DATA__REPOSITORY_MAP);
		createEAttribute(serverDataEClass, SERVER_DATA__TRACE);
		createEAttribute(serverDataEClass, SERVER_DATA__WORKER_THREAD_COUNT);
		createEAttribute(serverDataEClass, SERVER_DATA__WORKER_THREAD_MIN_COUNT);
		createEAttribute(serverDataEClass, SERVER_DATA__SNC_MODE);
		createEAttribute(serverDataEClass, SERVER_DATA__SNC_QOP);
		createEAttribute(serverDataEClass, SERVER_DATA__SNC_MYNAME);
		createEAttribute(serverDataEClass, SERVER_DATA__SNC_LIB);

		serverDataStoreEntryEClass = createEClass(SERVER_DATA_STORE_ENTRY);
		createEAttribute(serverDataStoreEntryEClass, SERVER_DATA_STORE_ENTRY__KEY);
		createEReference(serverDataStoreEntryEClass, SERVER_DATA_STORE_ENTRY__VALUE);

		serverDataStoreEClass = createEClass(SERVER_DATA_STORE);
		createEReference(serverDataStoreEClass, SERVER_DATA_STORE__ENTRIES);

		functionTemplateEClass = createEClass(FUNCTION_TEMPLATE);
		createEReference(functionTemplateEClass, FUNCTION_TEMPLATE__IMPORTS);
		createEReference(functionTemplateEClass, FUNCTION_TEMPLATE__EXPORTS);
		createEReference(functionTemplateEClass, FUNCTION_TEMPLATE__CHANGING);
		createEReference(functionTemplateEClass, FUNCTION_TEMPLATE__TABLES);
		createEReference(functionTemplateEClass, FUNCTION_TEMPLATE__EXCEPTIONS);
		createEAttribute(functionTemplateEClass, FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST);
		createEAttribute(functionTemplateEClass, FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST);
		createEAttribute(functionTemplateEClass, FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST);
		createEAttribute(functionTemplateEClass, FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST);
		createEAttribute(functionTemplateEClass, FUNCTION_TEMPLATE__EXCEPTION_LIST);

		recordMetaDataEClass = createEClass(RECORD_META_DATA);
		createEReference(recordMetaDataEClass, RECORD_META_DATA__FIELD_META_DATA);
		createEAttribute(recordMetaDataEClass, RECORD_META_DATA__NAME);
		createEAttribute(recordMetaDataEClass, RECORD_META_DATA__RECORD_FIELD_META_DATA);

		fieldMetaDataEClass = createEClass(FIELD_META_DATA);
		createEReference(fieldMetaDataEClass, FIELD_META_DATA__FIELD_META_DATA);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__NAME);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__TYPE);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__BYTE_LENGTH);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__BYTE_OFFSET);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__UNICODE_BYTE_LENGTH);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__UNICODE_BYTE_OFFSET);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__DECIMALS);
		createEAttribute(fieldMetaDataEClass, FIELD_META_DATA__DESCRIPTION);
		createEReference(fieldMetaDataEClass, FIELD_META_DATA__RECORD_META_DATA);

		listFieldMetaDataEClass = createEClass(LIST_FIELD_META_DATA);
		createEReference(listFieldMetaDataEClass, LIST_FIELD_META_DATA__FIELD_META_DATA);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__NAME);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__TYPE);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__BYTE_LENGTH);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__UNICODE_BYTE_LENGTH);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__DECIMALS);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__DEFAULTS);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__DESCRIPTION);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__IMPORT);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__CHANGING);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__EXPORT);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__EXCEPTION);
		createEAttribute(listFieldMetaDataEClass, LIST_FIELD_META_DATA__OPTIONAL);
		createEReference(listFieldMetaDataEClass, LIST_FIELD_META_DATA__RECORD_META_DATA);

		abapExceptionEClass = createEClass(ABAP_EXCEPTION);
		createEAttribute(abapExceptionEClass, ABAP_EXCEPTION__KEY);
		createEAttribute(abapExceptionEClass, ABAP_EXCEPTION__MESSAGE);

		respositoryDataEntryEClass = createEClass(RESPOSITORY_DATA_ENTRY);
		createEAttribute(respositoryDataEntryEClass, RESPOSITORY_DATA_ENTRY__KEY);
		createEReference(respositoryDataEntryEClass, RESPOSITORY_DATA_ENTRY__VALUE);

		repositoryDataEClass = createEClass(REPOSITORY_DATA);
		createEReference(repositoryDataEClass, REPOSITORY_DATA__ENTRIES);
		createEAttribute(repositoryDataEClass, REPOSITORY_DATA__FUNCTION_TEMPLATES);

		repositoryDataStoreEClass = createEClass(REPOSITORY_DATA_STORE);
		createEReference(repositoryDataStoreEClass, REPOSITORY_DATA_STORE__ENTRIES);

		respositoryDataStoreEntryEClass = createEClass(RESPOSITORY_DATA_STORE_ENTRY);
		createEAttribute(respositoryDataStoreEntryEClass, RESPOSITORY_DATA_STORE_ENTRY__KEY);
		createEReference(respositoryDataStoreEntryEClass, RESPOSITORY_DATA_STORE_ENTRY__VALUE);

		// Create enums
		dataTypeEEnum = createEEnum(DATA_TYPE);

		// Create data types
		parameterListEDataType = createEDataType(PARAMETER_LIST);
		fieldListEDataType = createEDataType(FIELD_LIST);
		abapExceptionListEDataType = createEDataType(ABAP_EXCEPTION_LIST);
		functionTemplateMapEDataType = createEDataType(FUNCTION_TEMPLATE_MAP);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters
		ETypeParameter tableEClass_S = addETypeParameter(tableEClass, "S");

		// Set bounds for type parameters
		EGenericType g1 = createEGenericType(this.getStructure());
		tableEClass_S.getEBounds().add(g1);

		// Add supertypes to classes

		// Initialize classes and features; add operations and parameters
		initEClass(destinationEClass, Destination.class, "Destination", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDestination_Name(), ecorePackage.getEString(), "name", null, 0, 1, Destination.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestination_RepositoryName(), ecorePackage.getEString(), "repositoryName", null, 0, 1, Destination.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDestination_Rfcs(), this.getRFC(), this.getRFC_Destination(), "rfcs", null, 0, -1, Destination.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(rfcEClass, org.fusesource.camel.component.sap.model.rfc.RFC.class, "RFC", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRFC_Name(), ecorePackage.getEString(), "name", null, 0, 1, org.fusesource.camel.component.sap.model.rfc.RFC.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRFC_Group(), ecorePackage.getEString(), "group", null, 0, 1, org.fusesource.camel.component.sap.model.rfc.RFC.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRFC_Description(), ecorePackage.getEString(), "description", null, 0, 1, org.fusesource.camel.component.sap.model.rfc.RFC.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRFC_Request(), this.getStructure(), null, "request", null, 0, 1, org.fusesource.camel.component.sap.model.rfc.RFC.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRFC_Response(), this.getStructure(), null, "response", null, 0, 1, org.fusesource.camel.component.sap.model.rfc.RFC.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRFC_Destination(), this.getDestination(), this.getDestination_Rfcs(), "destination", null, 0, 1, org.fusesource.camel.component.sap.model.rfc.RFC.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(tableEClass, Table.class, "Table", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		EOperation op = addEOperation(tableEClass, null, "getRows", 0, -1, IS_UNIQUE, IS_ORDERED);
		g1 = createEGenericType(tableEClass_S);
		initEOperation(op, g1);

		op = addEOperation(tableEClass, null, "add", 0, 1, IS_UNIQUE, IS_ORDERED);
		g1 = createEGenericType(tableEClass_S);
		initEOperation(op, g1);

		op = addEOperation(tableEClass, null, "add", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEInt(), "index", 0, 1, IS_UNIQUE, IS_ORDERED);
		g1 = createEGenericType(tableEClass_S);
		initEOperation(op, g1);

		initEClass(structureEClass, Structure.class, "Structure", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		op = addEOperation(structureEClass, null, "get", 0, 1, IS_UNIQUE, IS_ORDERED);
		ETypeParameter t1 = addETypeParameter(op, "T");
		addEParameter(op, ecorePackage.getEJavaObject(), "key", 0, 1, IS_UNIQUE, IS_ORDERED);
		g1 = createEGenericType(ecorePackage.getEJavaClass());
		EGenericType g2 = createEGenericType(t1);
		g1.getETypeArguments().add(g2);
		addEParameter(op, g1, "type", 0, 1, IS_UNIQUE, IS_ORDERED);
		g1 = createEGenericType(t1);
		initEOperation(op, g1);

		initEClass(destinationDataEntryEClass, Map.Entry.class, "DestinationDataEntry", !IS_ABSTRACT, !IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDestinationDataEntry_Key(), ecorePackage.getEString(), "key", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationDataEntry_Value(), ecorePackage.getEString(), "value", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(destinationDataEClass, DestinationData.class, "DestinationData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getDestinationData_Entries(), this.getDestinationDataEntry(), null, "entries", null, 0, -1, DestinationData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_AliasUser(), ecorePackage.getEString(), "aliasUser", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Ashost(), ecorePackage.getEString(), "ashost", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_AuthType(), ecorePackage.getEString(), "authType", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Client(), ecorePackage.getEString(), "client", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Codepage(), ecorePackage.getEString(), "codepage", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_CpicTrace(), ecorePackage.getEString(), "cpicTrace", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_DenyInitialPassword(), ecorePackage.getEString(), "denyInitialPassword", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_ExpirationPeriod(), ecorePackage.getEString(), "expirationPeriod", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_ExpirationTime(), ecorePackage.getEString(), "expirationTime", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Getsso2(), ecorePackage.getEString(), "getsso2", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Group(), ecorePackage.getEString(), "group", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Gwhost(), ecorePackage.getEString(), "gwhost", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Gwserv(), ecorePackage.getEString(), "gwserv", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Lang(), ecorePackage.getEString(), "lang", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Lcheck(), ecorePackage.getEString(), "lcheck", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_MaxGetTime(), ecorePackage.getEString(), "maxGetTime", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Mshost(), ecorePackage.getEString(), "mshost", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Msserv(), ecorePackage.getEString(), "msserv", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Mysapsso2(), ecorePackage.getEString(), "mysapsso2", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Passwd(), ecorePackage.getEString(), "passwd", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Password(), ecorePackage.getEString(), "password", null, 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Pcs(), ecorePackage.getEString(), "pcs", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_PeakLimit(), ecorePackage.getEString(), "peakLimit", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_PingOnCreate(), ecorePackage.getEString(), "pingOnCreate", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_PoolCapacity(), ecorePackage.getEString(), "poolCapacity", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_R3name(), ecorePackage.getEString(), "r3name", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_RepositoryDest(), ecorePackage.getEString(), "repositoryDest", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_RepositoryPasswd(), ecorePackage.getEString(), "repositoryPasswd", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_RepositoryRoundtripOptimization(), ecorePackage.getEString(), "repositoryRoundtripOptimization", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_RepositorySnc(), ecorePackage.getEString(), "repositorySnc", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_RepositoryUser(), ecorePackage.getEString(), "repositoryUser", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Saprouter(), ecorePackage.getEString(), "saprouter", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_SncLibrary(), ecorePackage.getEString(), "sncLibrary", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_SncMode(), ecorePackage.getEString(), "sncMode", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_SncMyname(), ecorePackage.getEString(), "sncMyname", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_SncPartnername(), ecorePackage.getEString(), "sncPartnername", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_SncQop(), ecorePackage.getEString(), "sncQop", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Sysnr(), ecorePackage.getEString(), "sysnr", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Tphost(), ecorePackage.getEString(), "tphost", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Tpname(), ecorePackage.getEString(), "tpname", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Trace(), ecorePackage.getEString(), "trace", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_Type(), ecorePackage.getEString(), "type", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_UserName(), ecorePackage.getEString(), "userName", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_User(), ecorePackage.getEString(), "user", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_UserId(), ecorePackage.getEString(), "userId", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_UseSapgui(), ecorePackage.getEString(), "useSapgui", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getDestinationData_X509cert(), ecorePackage.getEString(), "x509cert", "", 0, 1, DestinationData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(destinationDataStoreEntryEClass, Map.Entry.class, "DestinationDataStoreEntry", !IS_ABSTRACT, !IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDestinationDataStoreEntry_Key(), ecorePackage.getEString(), "key", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDestinationDataStoreEntry_Value(), this.getDestinationData(), null, "value", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(destinationDataStoreEClass, DestinationDataStore.class, "DestinationDataStore", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getDestinationDataStore_Entries(), this.getDestinationDataStoreEntry(), null, "entries", null, 0, -1, DestinationDataStore.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serverEClass, Server.class, "Server", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServer_Name(), ecorePackage.getEString(), "name", null, 0, 1, Server.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serverDataEntryEClass, Map.Entry.class, "ServerDataEntry", !IS_ABSTRACT, !IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServerDataEntry_Key(), ecorePackage.getEString(), "key", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerDataEntry_Value(), ecorePackage.getEString(), "value", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serverDataEClass, ServerData.class, "ServerData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getServerData_Entries(), this.getServerDataEntry(), null, "entries", null, 0, -1, ServerData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_Gwhost(), ecorePackage.getEString(), "gwhost", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_Gwserv(), ecorePackage.getEString(), "gwserv", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_Progid(), ecorePackage.getEString(), "progid", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_ConnectionCount(), ecorePackage.getEString(), "connectionCount", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_Saprouter(), ecorePackage.getEString(), "saprouter", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_MaxStartUpDelay(), ecorePackage.getEString(), "maxStartUpDelay", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_RepositoryDestination(), ecorePackage.getEString(), "repositoryDestination", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_RepositoryMap(), ecorePackage.getEString(), "repositoryMap", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_Trace(), ecorePackage.getEString(), "trace", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_WorkerThreadCount(), ecorePackage.getEString(), "workerThreadCount", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_WorkerThreadMinCount(), ecorePackage.getEString(), "workerThreadMinCount", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_SncMode(), ecorePackage.getEString(), "sncMode", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_SncQop(), ecorePackage.getEString(), "sncQop", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_SncMyname(), ecorePackage.getEString(), "sncMyname", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerData_SncLib(), ecorePackage.getEString(), "sncLib", "", 0, 1, ServerData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(serverDataStoreEntryEClass, Map.Entry.class, "ServerDataStoreEntry", !IS_ABSTRACT, !IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServerDataStoreEntry_Key(), ecorePackage.getEString(), "key", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getServerDataStoreEntry_Value(), this.getServerData(), null, "value", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serverDataStoreEClass, ServerDataStore.class, "ServerDataStore", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getServerDataStore_Entries(), this.getServerDataStoreEntry(), null, "entries", null, 0, -1, ServerDataStore.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(functionTemplateEClass, FunctionTemplate.class, "FunctionTemplate", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getFunctionTemplate_Imports(), this.getListFieldMetaData(), null, "imports", null, 0, -1, FunctionTemplate.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFunctionTemplate_Exports(), this.getListFieldMetaData(), null, "exports", null, 0, -1, FunctionTemplate.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFunctionTemplate_Changing(), this.getListFieldMetaData(), null, "changing", null, 0, -1, FunctionTemplate.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFunctionTemplate_Tables(), this.getListFieldMetaData(), null, "tables", null, 0, -1, FunctionTemplate.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFunctionTemplate_Exceptions(), this.getAbapException(), null, "exceptions", null, 0, -1, FunctionTemplate.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFunctionTemplate_ImportParameterList(), this.getParameterList(), "importParameterList", null, 0, 1, FunctionTemplate.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getFunctionTemplate_ExportParameterList(), this.getParameterList(), "exportParameterList", null, 0, 1, FunctionTemplate.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getFunctionTemplate_ChangingParameterList(), this.getParameterList(), "changingParameterList", null, 0, 1, FunctionTemplate.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getFunctionTemplate_TableParameterList(), this.getParameterList(), "tableParameterList", null, 0, 1, FunctionTemplate.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getFunctionTemplate_ExceptionList(), this.getAbapExceptionList(), "exceptionList", null, 0, 1, FunctionTemplate.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(recordMetaDataEClass, RecordMetaData.class, "RecordMetaData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getRecordMetaData_FieldMetaData(), this.getFieldMetaData(), null, "fieldMetaData", null, 0, -1, RecordMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRecordMetaData_Name(), ecorePackage.getEString(), "name", null, 1, 1, RecordMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRecordMetaData_RecordFieldMetaData(), this.getFieldList(), "recordFieldMetaData", null, 0, 1, RecordMetaData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(fieldMetaDataEClass, FieldMetaData.class, "FieldMetaData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getFieldMetaData_FieldMetaData(), this.getFieldMetaData(), null, "fieldMetaData", null, 0, -1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_Name(), ecorePackage.getEString(), "name", null, 1, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_Type(), this.getDataType(), "type", null, 1, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_ByteLength(), ecorePackage.getEInt(), "byteLength", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_ByteOffset(), ecorePackage.getEInt(), "byteOffset", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_UnicodeByteLength(), ecorePackage.getEInt(), "unicodeByteLength", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_UnicodeByteOffset(), ecorePackage.getEInt(), "unicodeByteOffset", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_Decimals(), ecorePackage.getEInt(), "decimals", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFieldMetaData_Description(), ecorePackage.getEString(), "description", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFieldMetaData_RecordMetaData(), this.getRecordMetaData(), null, "recordMetaData", null, 0, 1, FieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(listFieldMetaDataEClass, ListFieldMetaData.class, "ListFieldMetaData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getListFieldMetaData_FieldMetaData(), this.getFieldMetaData(), null, "fieldMetaData", null, 0, -1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Name(), ecorePackage.getEString(), "name", null, 1, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Type(), this.getDataType(), "type", null, 1, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_ByteLength(), ecorePackage.getEInt(), "byteLength", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_UnicodeByteLength(), ecorePackage.getEInt(), "unicodeByteLength", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Decimals(), ecorePackage.getEInt(), "decimals", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Defaults(), ecorePackage.getEString(), "defaults", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Description(), ecorePackage.getEString(), "description", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Import(), ecorePackage.getEBoolean(), "import", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Changing(), ecorePackage.getEBoolean(), "changing", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Export(), ecorePackage.getEBoolean(), "export", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Exception(), ecorePackage.getEBoolean(), "exception", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListFieldMetaData_Optional(), ecorePackage.getEBoolean(), "optional", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getListFieldMetaData_RecordMetaData(), this.getRecordMetaData(), null, "recordMetaData", null, 0, 1, ListFieldMetaData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(abapExceptionEClass, AbapException.class, "AbapException", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAbapException_Key(), ecorePackage.getEString(), "key", null, 0, 1, AbapException.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAbapException_Message(), ecorePackage.getEString(), "message", null, 0, 1, AbapException.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(respositoryDataEntryEClass, Map.Entry.class, "RespositoryDataEntry", !IS_ABSTRACT, !IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRespositoryDataEntry_Key(), ecorePackage.getEString(), "key", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRespositoryDataEntry_Value(), this.getFunctionTemplate(), null, "value", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(repositoryDataEClass, RepositoryData.class, "RepositoryData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getRepositoryData_Entries(), this.getRespositoryDataEntry(), null, "entries", null, 0, -1, RepositoryData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRepositoryData_FunctionTemplates(), this.getFunctionTemplateMap(), "functionTemplates", null, 0, 1, RepositoryData.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(repositoryDataStoreEClass, RepositoryDataStore.class, "RepositoryDataStore", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getRepositoryDataStore_Entries(), this.getRespositoryDataStoreEntry(), null, "entries", null, 0, -1, RepositoryDataStore.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(respositoryDataStoreEntryEClass, Map.Entry.class, "RespositoryDataStoreEntry", !IS_ABSTRACT, !IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRespositoryDataStoreEntry_Key(), ecorePackage.getEString(), "key", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRespositoryDataStoreEntry_Value(), this.getRepositoryData(), null, "value", null, 1, 1, Map.Entry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(dataTypeEEnum, DataType.class, "DataType");
		addEEnumLiteral(dataTypeEEnum, DataType.CHAR);
		addEEnumLiteral(dataTypeEEnum, DataType.DATE);
		addEEnumLiteral(dataTypeEEnum, DataType.BCD);
		addEEnumLiteral(dataTypeEEnum, DataType.TIME);
		addEEnumLiteral(dataTypeEEnum, DataType.BYTE);
		addEEnumLiteral(dataTypeEEnum, DataType.NUM);
		addEEnumLiteral(dataTypeEEnum, DataType.FLOAT);
		addEEnumLiteral(dataTypeEEnum, DataType.INT);
		addEEnumLiteral(dataTypeEEnum, DataType.INT2);
		addEEnumLiteral(dataTypeEEnum, DataType.INT1);
		addEEnumLiteral(dataTypeEEnum, DataType.STRUCTURE);
		addEEnumLiteral(dataTypeEEnum, DataType.DECF16);
		addEEnumLiteral(dataTypeEEnum, DataType.DECF34);
		addEEnumLiteral(dataTypeEEnum, DataType.STRING);
		addEEnumLiteral(dataTypeEEnum, DataType.XSTRING);
		addEEnumLiteral(dataTypeEEnum, DataType.TABLE);

		// Initialize data types
		initEDataType(parameterListEDataType, List.class, "ParameterList", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS, "java.util.List<org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData>");
		initEDataType(fieldListEDataType, List.class, "FieldList", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS, "java.util.List<org.fusesource.camel.component.sap.model.rfc.FieldMetaData>");
		initEDataType(abapExceptionListEDataType, List.class, "AbapExceptionList", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS, "java.util.List<org.fusesource.camel.component.sap.model.rfc.AbapException>");
		initEDataType(functionTemplateMapEDataType, Map.class, "FunctionTemplateMap", !IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS, "java.util.Map<java.lang.String, org.fusesource.camel.component.sap.model.rfc.FunctionTemplate>");

		// Create resource
		createResource(eNS_URI);
	}

} //RfcPackageImpl
