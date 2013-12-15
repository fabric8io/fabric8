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

import biz.c24.io.api.presentation.Sink;
import biz.c24.io.api.presentation.Source;
import biz.c24.io.api.presentation.XMLSink;
import biz.c24.io.gettingstarted.transaction.Transactions;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.util.ExchangeHelper;
import org.junit.Test;

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
