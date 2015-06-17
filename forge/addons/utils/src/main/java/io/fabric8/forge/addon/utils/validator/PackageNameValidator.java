/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.addon.utils.validator;

import org.jboss.forge.addon.parser.java.ui.validators.AbstractJLSUIValidator;
import org.jboss.forge.addon.parser.java.utils.ResultType;
import org.jboss.forge.addon.parser.java.utils.ValidationResult;

public class PackageNameValidator extends AbstractJLSUIValidator {

    @Override
    protected ValidationResult validate(String s) {
        if (s == null) {
            return new ValidationResult(ResultType.INFO);
        }

        // is it a valid class name
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];

            if (i > 0 && ch == '.') {
                // a dot is valid as its a package name separator
                continue;
            }
            if (!Character.isJavaIdentifierPart(ch)) {
                return new ValidationResult(ResultType.ERROR, "The package name [" + s + "] is invalid at position " + (i + 1));
            }
            // they must all be lower case for alphas
            if (Character.isAlphabetic(ch) && !Character.isLowerCase(ch)) {
                return new ValidationResult(ResultType.ERROR, "The package name [" + s + "] must be lower case alphabetic character");
            }
        }

        return new ValidationResult(ResultType.INFO);
    }

}
