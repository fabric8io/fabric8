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

public class MaxLengthValidator extends AbstractJLSUIValidator {

    private int maxLength;

    public MaxLengthValidator(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    protected ValidationResult validate(String s) {
        if (s == null) {
            return new ValidationResult(ResultType.INFO);
        }

        if (maxLength > 0 && s.length() > maxLength) {
            return new ValidationResult(ResultType.ERROR, "Maximum length is " + maxLength + " characters");
        }

        return new ValidationResult(ResultType.INFO);
    }

}
