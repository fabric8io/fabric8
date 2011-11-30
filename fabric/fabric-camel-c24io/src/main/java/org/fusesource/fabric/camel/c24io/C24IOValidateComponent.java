/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.util.Map;

import biz.c24.io.api.data.ValidationConstraints;
import biz.c24.io.api.data.ValidationManager;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.ProcessorEndpoint;

/**
 * @version $Revision$
 */
public class C24IOValidateComponent extends DefaultComponent {
    protected Endpoint createEndpoint(String uri, String remaining, Map parameters) throws Exception {

        C24IOValidator validator = new C24IOValidator();
        Object bean = getCamelContext().getRegistry().lookup(remaining);
        if (bean instanceof ValidationManager) {
            ValidationManager validationManager = (ValidationManager)bean;
            validator.setValidationManager(validationManager);
        } else if (bean instanceof ValidationConstraints) {
            ValidationConstraints validationConstraints = (ValidationConstraints)bean;
            ValidationManager validationManager = new ValidationManager(validationConstraints);
            validator.setValidationManager(validationManager);
        } else if (bean != null) {
            throw new IllegalArgumentException("Bean " + remaining + " not an instance of ValidationManager or ValidationContraints");
        }
        return new ProcessorEndpoint(uri, this, validator);
    }
}
