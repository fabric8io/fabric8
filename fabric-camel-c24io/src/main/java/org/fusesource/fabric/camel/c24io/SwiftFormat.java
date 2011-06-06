/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.presentation.BinarySink;
import biz.c24.io.api.presentation.JavaClassSink;
import biz.c24.io.api.presentation.SAXSink;
import biz.c24.io.api.presentation.Sink;
import biz.c24.io.api.presentation.TagValuePairSink;
import biz.c24.io.api.presentation.TextualSink;
import biz.c24.io.api.presentation.XMLSink;
import biz.c24.io.api.presentation.swift.SwiftPreParser;

import org.apache.camel.Exchange;
import org.apache.camel.model.dataformat.C24IOContentType;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.util.ExchangeHelper;

import static org.apache.camel.util.ObjectHelper.notNull;

/**
 * A {@link DataFormat} using SWIFT with
 * <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a>
 *
 * @version $Revision$
 */
public class SwiftFormat implements DataFormat {
    private final SwiftPreParser swiftParser;
    private Sink sink;
    private C24IOContentType contentType;

    public SwiftFormat() {
        this(new SwiftPreParser());
    }

    public SwiftFormat(SwiftPreParser swiftParser) {
        this.swiftParser = swiftParser;
    }

    public SwiftFormat(SwiftPreParser swiftParser, Sink sink) {
        this(swiftParser);
        this.sink = sink;
    }

    public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        ComplexDataObject dataObject = ExchangeHelper.convertToMandatoryType(exchange, ComplexDataObject.class, graph);
        Sink s = getSink(exchange);
        notNull(s, "sink or element");
        s.setOutputStream(stream);
        s.writeObject(dataObject);
    }

    public Object unmarshal(Exchange exchange, InputStream stream) throws IOException {
        swiftParser.setInputStream(stream);
        return swiftParser.readObject();
    }

    public Sink getSink(Exchange exchange) {
        if (sink == null) {
            C24IOContentType content = getContentType();
            if (content != null) {
                sink = createSink(content);
            }
            if (sink == null) {
                // lets default to the one from the element
                sink = createDefaultSink();
            }
        }
        return sink;
    }

    // Properties
    //-------------------------------------------------------------------------
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    public C24IOContentType getContentType() {
        return contentType;
    }

    public void setContentType(C24IOContentType contentType) {
        this.contentType = contentType;
    }


    // Implementation methods
    //-------------------------------------------------------------------------
    protected Sink createSink(C24IOContentType content) {
        switch (content) {
        case Default:
            return createDefaultSink();
        case Binary:
            return new BinarySink();
        case Java:
            return new JavaClassSink();
        case Sax:
            return new SAXSink();
        case Text:
            return new TextualSink();
        case Xml:
            return new XMLSink();
        case TagValuePair:
            return new TagValuePairSink();
        default:
            throw new IllegalArgumentException("Unknown format type: " + content);
        }
    }

    protected Sink createDefaultSink() {
        return new TextualSink();
    }
}