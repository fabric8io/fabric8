/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.data.ValidationException;
import biz.c24.io.api.presentation.BinarySink;
import biz.c24.io.api.presentation.JavaClassSink;
import biz.c24.io.api.presentation.SAXSink;
import biz.c24.io.api.presentation.Sink;
import biz.c24.io.api.presentation.TagValuePairSink;
import biz.c24.io.api.presentation.TextualSink;
import biz.c24.io.api.presentation.XMLSink;

import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.util.ExchangeHelper;

/**
 * Transforms an C24 data object into some output format
 *
 * @version $Revision$
 */
public class C24IOSink extends C24IOSource {
    private Sink sink;

    public C24IOSink() {
    }

    public C24IOSink(Sink sink) {
        this.sink = sink;
    }
    
    public static C24IOSink c24Sink() {
        return new C24IOSink();
    }

    public static C24IOSink c24Sink(Sink sink) {
        return new C24IOSink(sink);
    }

    public void process(Exchange exchange) throws Exception {
        ComplexDataObject dataObject = unmarshalDataObject(exchange);

        dataObject = transformDataObject(exchange, dataObject);

        Sink s = getSink();
        if (s == null) {
            s = dataObject.getModel().sink();
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        s.setOutputStream(buffer);
        s.writeObject(dataObject);

        Message out = exchange.getOut();
        out.setHeader("c24io.sink", sink);
        out.setBody(buffer.toByteArray());
    }

    public Sink getSink() {
        return sink;
    }

    public void setSink(Sink sink) {
        this.sink = sink;
    }

    // Fluent API
    //-------------------------------------------------------------------------

    /**
     * Sets the output sink to be text
     */
    public C24IOSink text() {
        setSink(new TextualSink());
        return this;
    }

    /**
     * Sets the output sink to be binary
     */
    public C24IOSink binary() {
        setSink(new BinarySink());
        return this;
    }

    /**
     * Sets the output sink to be XML
     */
    public C24IOSink xml() {
        setSink(new XMLSink());
        return this;
    }

    /**
     * Sets the output sink to be SAX
     */
    public C24IOSink sax() {
        setSink(new SAXSink());
        return this;
    }

    /**
     * Sets the output sink to be tag value pair
     */
    public C24IOSink tagValuePair() {
        setSink(new TagValuePairSink());
        return this;
    }

    /**
     * Sets the output sink to be Java
     */
    public C24IOSink java() {
        setSink(new JavaClassSink());
        return this;
    }


    // Implementation methods
    //-------------------------------------------------------------------------
    protected ComplexDataObject unmarshalDataObject(Exchange exchange) throws InvalidPayloadException, IOException {
        return ExchangeHelper.getMandatoryInBody(exchange, ComplexDataObject.class);
    }

    protected ComplexDataObject transformDataObject(Exchange exchange, ComplexDataObject dataObject) throws ValidationException {
        return dataObject;
    }
}