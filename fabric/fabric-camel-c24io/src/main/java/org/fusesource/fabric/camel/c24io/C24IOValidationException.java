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
import org.apache.camel.Exchange;
import org.apache.camel.ValidationException;

/**
 * A Camel {@link ValidationException} thrown if validation fails using
 * <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a>.
 *
 * @version $Revision$
 */
public class C24IOValidationException extends ValidationException {
    private final ComplexDataObject dataObject;
    private final biz.c24.io.api.data.ValidationException validationException;

    public C24IOValidationException(Exchange exchange, ComplexDataObject dataObject, biz.c24.io.api.data.ValidationException validationException) {
        super(validationException.toString(), exchange, validationException);
        this.dataObject = dataObject;
        this.validationException = validationException;
    }

    public ComplexDataObject getDataObject() {
        return dataObject;
    }

    /**
     * The underlying <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a> exception
     *
     * @return
     */
    public biz.c24.io.api.data.ValidationException getValidationException() {
        return validationException;
    }
}
