/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

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
