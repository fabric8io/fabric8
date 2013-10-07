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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.fusesource.camel.component.sap.model.rfc.AbapException;
import org.fusesource.camel.component.sap.model.rfc.FunctionTemplate;
import org.fusesource.camel.component.sap.model.rfc.ListFieldMetaData;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Function Template</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getImports <em>Imports</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getExports <em>Exports</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getChanging <em>Changing</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getTables <em>Tables</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getExceptions <em>Exceptions</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getImportParameterList <em>Import Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getExportParameterList <em>Export Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getChangingParameterList <em>Changing Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getTableParameterList <em>Table Parameter List</em>}</li>
 *   <li>{@link org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl#getExceptionList <em>Exception List</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FunctionTemplateImpl extends EObjectImpl implements FunctionTemplate {
	/**
	 * The cached value of the '{@link #getImports() <em>Imports</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImports()
	 * @generated
	 * @ordered
	 */
	protected EList<ListFieldMetaData> imports;

	/**
	 * The cached value of the '{@link #getExports() <em>Exports</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExports()
	 * @generated
	 * @ordered
	 */
	protected EList<ListFieldMetaData> exports;

	/**
	 * The cached value of the '{@link #getChanging() <em>Changing</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChanging()
	 * @generated
	 * @ordered
	 */
	protected EList<ListFieldMetaData> changing;

	/**
	 * The cached value of the '{@link #getTables() <em>Tables</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTables()
	 * @generated
	 * @ordered
	 */
	protected EList<ListFieldMetaData> tables;

	/**
	 * The cached value of the '{@link #getExceptions() <em>Exceptions</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExceptions()
	 * @generated
	 * @ordered
	 */
	protected EList<AbapException> exceptions;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FunctionTemplateImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return RfcPackage.Literals.FUNCTION_TEMPLATE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<ListFieldMetaData> getImports() {
		if (imports == null) {
			imports = new EObjectResolvingEList<ListFieldMetaData>(ListFieldMetaData.class, this, RfcPackage.FUNCTION_TEMPLATE__IMPORTS);
		}
		return imports;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<ListFieldMetaData> getExports() {
		if (exports == null) {
			exports = new EObjectResolvingEList<ListFieldMetaData>(ListFieldMetaData.class, this, RfcPackage.FUNCTION_TEMPLATE__EXPORTS);
		}
		return exports;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<ListFieldMetaData> getChanging() {
		if (changing == null) {
			changing = new EObjectResolvingEList<ListFieldMetaData>(ListFieldMetaData.class, this, RfcPackage.FUNCTION_TEMPLATE__CHANGING);
		}
		return changing;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<ListFieldMetaData> getTables() {
		if (tables == null) {
			tables = new EObjectResolvingEList<ListFieldMetaData>(ListFieldMetaData.class, this, RfcPackage.FUNCTION_TEMPLATE__TABLES);
		}
		return tables;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<AbapException> getExceptions() {
		if (exceptions == null) {
			exceptions = new EObjectResolvingEList<AbapException>(AbapException.class, this, RfcPackage.FUNCTION_TEMPLATE__EXCEPTIONS);
		}
		return exceptions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public List<ListFieldMetaData> getImportParameterList() {
		return getImports();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setImportParameterList(List<ListFieldMetaData> newImportParameterList) {
		getImports().clear();
		getImports().addAll(newImportParameterList);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public List<ListFieldMetaData> getExportParameterList() {
		return getExports();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setExportParameterList(List<ListFieldMetaData> newExportParameterList) {
		getExports().clear();
		getExports().addAll(newExportParameterList);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public List<ListFieldMetaData> getChangingParameterList() {
		return getChanging();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setChangingParameterList(List<ListFieldMetaData> newChangingParameterList) {
		getChanging().clear();
		getChanging().addAll(newChangingParameterList);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public List<ListFieldMetaData> getTableParameterList() {
		return getTables();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setTableParameterList(List<ListFieldMetaData> newTableParameterList) {
		getTables().clear();
		getTables().addAll(newTableParameterList);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public List<AbapException> getExceptionList() {
		return getExceptions();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setExceptionList(List<AbapException> newExceptionList) {
		getExceptions().clear();
		getExceptions().addAll(newExceptionList);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case RfcPackage.FUNCTION_TEMPLATE__IMPORTS:
				return getImports();
			case RfcPackage.FUNCTION_TEMPLATE__EXPORTS:
				return getExports();
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING:
				return getChanging();
			case RfcPackage.FUNCTION_TEMPLATE__TABLES:
				return getTables();
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTIONS:
				return getExceptions();
			case RfcPackage.FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST:
				return getImportParameterList();
			case RfcPackage.FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST:
				return getExportParameterList();
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST:
				return getChangingParameterList();
			case RfcPackage.FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST:
				return getTableParameterList();
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTION_LIST:
				return getExceptionList();
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
			case RfcPackage.FUNCTION_TEMPLATE__IMPORTS:
				getImports().clear();
				getImports().addAll((Collection<? extends ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXPORTS:
				getExports().clear();
				getExports().addAll((Collection<? extends ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING:
				getChanging().clear();
				getChanging().addAll((Collection<? extends ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__TABLES:
				getTables().clear();
				getTables().addAll((Collection<? extends ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTIONS:
				getExceptions().clear();
				getExceptions().addAll((Collection<? extends AbapException>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST:
				setImportParameterList((List<ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST:
				setExportParameterList((List<ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST:
				setChangingParameterList((List<ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST:
				setTableParameterList((List<ListFieldMetaData>)newValue);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTION_LIST:
				setExceptionList((List<AbapException>)newValue);
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
			case RfcPackage.FUNCTION_TEMPLATE__IMPORTS:
				getImports().clear();
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXPORTS:
				getExports().clear();
				return;
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING:
				getChanging().clear();
				return;
			case RfcPackage.FUNCTION_TEMPLATE__TABLES:
				getTables().clear();
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTIONS:
				getExceptions().clear();
				return;
			case RfcPackage.FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST:
				setImportParameterList((List<ListFieldMetaData>)null);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST:
				setExportParameterList((List<ListFieldMetaData>)null);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST:
				setChangingParameterList((List<ListFieldMetaData>)null);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST:
				setTableParameterList((List<ListFieldMetaData>)null);
				return;
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTION_LIST:
				setExceptionList((List<AbapException>)null);
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
			case RfcPackage.FUNCTION_TEMPLATE__IMPORTS:
				return imports != null && !imports.isEmpty();
			case RfcPackage.FUNCTION_TEMPLATE__EXPORTS:
				return exports != null && !exports.isEmpty();
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING:
				return changing != null && !changing.isEmpty();
			case RfcPackage.FUNCTION_TEMPLATE__TABLES:
				return tables != null && !tables.isEmpty();
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTIONS:
				return exceptions != null && !exceptions.isEmpty();
			case RfcPackage.FUNCTION_TEMPLATE__IMPORT_PARAMETER_LIST:
				return getImportParameterList() != null;
			case RfcPackage.FUNCTION_TEMPLATE__EXPORT_PARAMETER_LIST:
				return getExportParameterList() != null;
			case RfcPackage.FUNCTION_TEMPLATE__CHANGING_PARAMETER_LIST:
				return getChangingParameterList() != null;
			case RfcPackage.FUNCTION_TEMPLATE__TABLE_PARAMETER_LIST:
				return getTableParameterList() != null;
			case RfcPackage.FUNCTION_TEMPLATE__EXCEPTION_LIST:
				return getExceptionList() != null;
		}
		return super.eIsSet(featureID);
	}

} //FunctionTemplateImpl
