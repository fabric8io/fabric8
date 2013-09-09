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

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Structure</b></em>'.
 * @extends java.util.Map<String,Object>
 * <!-- end-user-doc -->
 *
 *
 * @see org.fusesource.camel.component.sap.model.rfc.RfcPackage#getStructure()
 * @model
 * @generated
 */
public interface Structure extends EObject, java.util.Map<String, Object> {

	/**
	 * <!-- begin-user-doc -->
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this {@link Structure} contains no mapping for the key.
	 * @param key - the key whose associated value is to be returned
	 * @param type - the type of required value
	 * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
	 * <!-- end-user-doc -->
	 * @model
	 * @generated
	 */
	<T> T get(Object key, Class<T> type);
} // Structure
