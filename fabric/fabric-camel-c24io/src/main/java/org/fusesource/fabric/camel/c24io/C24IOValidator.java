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
package io.fabric8.camel.c24io;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.data.ValidationException;
import biz.c24.io.api.data.ValidationManager;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.ExchangeHelper;

/**
 * Performs validation using <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a>
 * of a {@link ComplexDataObject}
 * 
 * @version $Revision$
 */
public class C24IOValidator implements Processor {
    private ValidationManager validationManager;

    public void process(Exchange exchange) throws Exception {
        ComplexDataObject object = ExchangeHelper.getMandatoryInBody(exchange, ComplexDataObject.class);
        try {
            // TODO should we use the listener and then throw an exception with all of the failures in it?
            getValidationManager().validateByException(object);
        } catch (ValidationException e) {
            throw new C24IOValidationException(exchange, object, e);
        }
    }

    public ValidationManager getValidationManager() {
        if (validationManager == null) {
            validationManager = createValidationManager();
        }
        return validationManager;
    }

    public void setValidationManager(ValidationManager validationManager) {
        this.validationManager = validationManager;
    }

    protected ValidationManager createValidationManager() {
        return new ValidationManager();
    }
}
