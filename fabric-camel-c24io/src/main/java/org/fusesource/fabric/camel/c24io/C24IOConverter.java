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
import biz.c24.io.api.data.saxon.DocumentNode;
import biz.c24.io.api.transform.Transform;
import biz.c24.saxon.Configuration;
import net.sf.saxon.om.Item;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;

/**
 * Helper converters for <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a>
 * 
 * @version $Revision$
 */
@Converter
public final class C24IOConverter {
    
    private C24IOConverter() {
        // Helper class
    }


    /**
     * A converter to provide a Processor for invoking the given C24IO
     * transformation class
     *
     * @return a Processor capable of performing the transformation on a Message Exchange
     */
    @Converter
    public static C24IOTransform toProcessor(Transform transformer) {
        return new C24IOTransform(transformer);
    }

    /**
     * Converts a data object into a Saxon document info so that it can be used in Saxon's
     * XQuery processor
     */
    @Converter
    public static DocumentNode toDocumentNode(ComplexDataObject dataObject, Exchange exchange) {
        Configuration configuration = exchange.getProperty("CamelSaxonConfiguration", Configuration.class);
        if (configuration == null) {
            configuration = new Configuration();
        }

        return toDocumentNode(configuration, dataObject);
    }

    /**
     * Converts a data object into a Saxon document info so that it can be used in Saxon's
     * XQuery processor
     */

    /* TODO - convert from ComplexDataObject to Saxon Item is required for XQuery support!
    @Converter
    public static Item toItem(ComplexDataObject dataObject, Exchange exchange) {
        // TODO!!!
        return null;
    }
    */

    /**
     * Converts a data object into a Saxon document info so that it can be used in Saxon's
     * XQuery processor
     */
    public static DocumentNode toDocumentNode(Configuration config, ComplexDataObject dataObject) {
        return new DocumentNode(config, dataObject, true, true);
    }
}
