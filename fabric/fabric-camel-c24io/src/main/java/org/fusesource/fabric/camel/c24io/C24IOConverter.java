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
import biz.c24.io.api.data.saxon.DocumentNode;
import biz.c24.io.api.transform.Transform;
import net.sf.saxon.Configuration;
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
    public static DocumentNode toDocumentNode(Configuration config, ComplexDataObject dataObject) {
        return new DocumentNode(config, dataObject, true, true);
    }
}
