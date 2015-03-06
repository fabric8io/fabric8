/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.forge.camel.commands.project;

import org.jboss.forge.addon.parser.java.ui.validators.AbstractJLSUIValidator;
import org.jboss.forge.addon.parser.java.utils.ResultType;
import org.jboss.forge.addon.parser.java.utils.ValidationResult;

public class ResourceNameValidator extends AbstractJLSUIValidator {

    private String extension;

    public ResourceNameValidator(String extension) {
        this.extension = extension;
    }

    @Override
    protected ValidationResult validate(String s) {
        if (s == null) {
            return new ValidationResult(ResultType.INFO);
        }

        if (extension != null && !s.endsWith("." + extension)) {
            return new ValidationResult(ResultType.ERROR, "The file name [" + s + "] must be a " + extension + " file");
        }

        // min length
        int min = extension != null ? extension.length() + 2 : 1;
        if (s.length() < min) {
            return new ValidationResult(ResultType.ERROR, "The file name [" + s + "] must be at least " + min + " characters");
        }

        return new ValidationResult(ResultType.INFO);
    }

}