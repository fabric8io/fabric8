/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.camel.tooling.util;

import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.MarshalDefinition;
import org.apache.camel.model.PolicyDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.TransactedDefinition;
import org.apache.camel.model.UnmarshalDefinition;

public class CamelModelUtils {

    public static boolean canAcceptInput(String className) {
        Class<?> aClass;
        Object def;
        try {
            aClass = Class.forName(className);
            def = aClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + className + ". " + e, e);
        }
        if (def instanceof ProcessorDefinition) {
            return canAcceptOutput(aClass, (ProcessorDefinition) def);
        } else {
            throw new IllegalArgumentException("The class is not a ProcessorDefinition! " + aClass);
        }
    }

	public static boolean canAcceptOutput(Class<?> aClass, ProcessorDefinition def) {
		if (aClass == null) {
			return false;
		}

		// special for bean/marshal/unmarshal, until their isOutputSupport would return false
		if (BeanDefinition.class.isAssignableFrom(aClass)) {
			return false;
		}
		if (MarshalDefinition.class.isAssignableFrom(aClass) ||
				UnmarshalDefinition.class.isAssignableFrom(aClass) ||
				TransactedDefinition.class.isAssignableFrom(aClass)) {
			return false;
		}

		// use isOutputSupport on camel model
		if (ProcessorDefinition.class.isAssignableFrom(aClass)) {
			if (def != null) {
				boolean answer = def.isOutputSupported();
				return answer;
			}
		}

		// assume no output is supported
		return false;
	}

	public static boolean isNextSiblingStepAddedAsNodeChild(Class<?> aClass, ProcessorDefinition def) {
		boolean acceptOutput = canAcceptOutput(aClass, def);
		return !acceptOutput || PolicyDefinition.class.isAssignableFrom(aClass);
	}

}
