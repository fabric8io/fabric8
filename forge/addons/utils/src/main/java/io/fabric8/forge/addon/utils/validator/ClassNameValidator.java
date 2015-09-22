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

public class ClassNameValidator extends AbstractJLSUIValidator {

    private boolean allowPackageName;

    public ClassNameValidator(boolean allowPackageName) {
        this.allowPackageName = allowPackageName;
    }

    @Override
    protected ValidationResult validate(String s) {
        if (s == null) {
            return new ValidationResult(ResultType.INFO);
        }

        // is it a valid class name
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (allowPackageName && ch == '.') {
                continue;
            }
            if (!Character.isJavaIdentifierPart(ch)) {
                return new ValidationResult(ResultType.ERROR, "The class name [" + s + "] is invalid at position " + (i + 1));
            }
        }

        int idx = 0;
        if (allowPackageName && s.lastIndexOf('.') != -1) {
            idx = s.lastIndexOf('.') + 1;
        }
        if (idx >= 0 && idx < s.length()) {
            char ch = s.charAt(idx);

            // first must be upper case alpha
            if (!Character.isUpperCase(ch)) {
                return new ValidationResult(ResultType.ERROR, "The class name [" + s + "] must start with an upper case alphabetic character at index " + idx);
            }
        }
        return new ValidationResult(ResultType.INFO);
    }
}
