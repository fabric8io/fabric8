/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import biz.c24.io.api.presentation.Sink;
import biz.c24.io.api.presentation.Source;
import biz.c24.io.api.presentation.XMLSink;
import biz.c24.testtransactions.Transactions;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.util.ExchangeHelper;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class DataFormatTest {

    @Test
    public void testParseDataObjectDirectly() throws Exception {
        Transactions transactions = new Transactions();
        Source source = transactions.getModel().source();
        source.setInputStream(createInputStream());
        source.readObject(transactions);

        System.out.println("Parsed: " + transactions);

        XMLSink sink = new XMLSink(System.out);
        sink.writeObject(transactions);
    }

    @Test
    public void testDataFormat() throws Exception {
        C24IOFormat format = new C24IOFormat();
        format.setElementType(Transactions.class);


        CamelContext context = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(context);
        InputStream in = createInputStream();
        Object answer = format.unmarshal(exchange, in);
        System.out.println("Unmarshalled data object: " + answer);
        assertNotNull(answer);
    }

    protected InputStream createInputStream() throws FileNotFoundException {
        return SampleDataFiles.sampleTransactionsFile();
    }
}
