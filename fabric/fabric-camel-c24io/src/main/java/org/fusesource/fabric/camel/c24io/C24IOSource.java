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
import biz.c24.io.api.data.Element;
import biz.c24.io.api.presentation.BinarySource;
import biz.c24.io.api.presentation.JavaClassSource;
import biz.c24.io.api.presentation.SAXSource;
import biz.c24.io.api.presentation.Source;
import biz.c24.io.api.presentation.TextualSource;
import biz.c24.io.api.presentation.XMLSource;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.ObjectHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * A parser of objects using the C24 IO
 *
 * @version $Revision$
 */
public class C24IOSource<T extends C24IOSource> implements Processor {
    private Element element;
    private Source source;

    public C24IOSource() {
    }

    public C24IOSource(Element element) {
        this.element = element;
    }

    @SuppressWarnings("unchecked")
    public static C24IOSource c24Source(String modelClassName) {
        try {
            Class<Element> elementType = (Class<Element>) ObjectHelper.loadClass(modelClassName);
            return c24Source(elementType);
        } catch (RuntimeCamelException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeCamelException(e);
        }
    }

    public static C24IOSource c24Source(Class<?> elementType) {
        Element element = C24IOHelper.getMandatoryElement(elementType);
        return c24Source(element);
    }

    public static C24IOSource c24Source(Element element) {
        return new C24IOSource(element);
    }

    public void process(Exchange exchange) throws Exception {
        ComplexDataObject object = parseDataObject(exchange);

        Message out = exchange.getOut();
        out.setHeader("c24io.element", element);
        out.setBody(object);
    }

    protected ComplexDataObject parseDataObject(Exchange exchange) throws InvalidPayloadException, IOException {
        Source source = getSource();

        // lets set the input stream
        Reader reader = exchange.getIn().getBody(Reader.class);
        if (reader != null) {
            source.setReader(reader);
        } else {
            // TODO have some SAXSource handling code here?

            InputStream inStream = ExchangeHelper.getMandatoryInBody(exchange, InputStream.class);
            source.setInputStream(inStream);
        }
        ComplexDataObject object = source.readObject(element);
        return object;
    }

    public static Element element(String modelClassName) {
        try {
            Class<?> elementType = Class.forName(modelClassName);
            return element(elementType);
        } catch (RuntimeCamelException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeCamelException(e);
        }
    }

    public static Element element(Class<?> elementType) {
        return C24IOHelper.getMandatoryElement(elementType);
    }

    // Properties
    //-------------------------------------------------------------------------
    public Element getElement() {
        return element;
    }

    public Source getSource() {
        if (source == null) {
            return getElement().getModel().source();
        }
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * Sets the source parser to text
     */
    @SuppressWarnings("unchecked")
    public T textSource() {
        setSource(new TextualSource());
        return (T) this;
    }

    /**
     * Sets the source parser to XML
     */
    @SuppressWarnings("unchecked")
    public T xmlSource() {
        setSource(new XMLSource());
        return (T) this;
    }

    /**
     * Sets the source parser to SAX
     */
    @SuppressWarnings("unchecked")
    public T saxSource() {
        setSource(new SAXSource());
        return (T) this;
    }

    /**
     * Sets the source parser to XML
     */
    @SuppressWarnings("unchecked")
    public T binarySource() {
        setSource(new BinarySource());
        return (T) this;
    }

    /**
     * Sets the source parser to XML
     */
    @SuppressWarnings("unchecked")
    public T javaSource() {
        setSource(new JavaClassSource());
        return (T) this;
    }
}
