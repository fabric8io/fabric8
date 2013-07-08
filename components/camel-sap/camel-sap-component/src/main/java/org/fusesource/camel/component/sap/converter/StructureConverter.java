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
package org.fusesource.camel.component.sap.converter;

import java.io.IOException;

import org.apache.camel.Converter;
import org.eclipse.emf.ecore.EObject;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.util.RfcUtil;

@Converter
public enum StructureConverter {
	INSTANCE;
	
	@Converter
	public static String toString(Structure structure) {
		try {
			return RfcUtil.marshal(structure);
		} catch (IOException e) {
			return null;
		}
	}
	
	@Converter
	public static Structure toStructure(String string) {
		try {
			EObject eObject = RfcUtil.unmarshal(string);
			
			if (Structure.class.isInstance(eObject)) {
				return (Structure) eObject;
			}
		} catch (IOException e) {
			// Ignore
		} 
		return null; 
	}

}
