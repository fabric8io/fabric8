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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.data.DataModel;
import biz.c24.io.api.data.DocumentRoot;
import biz.c24.io.api.data.Element;
import biz.c24.io.api.presentation.BinarySink;
import biz.c24.io.api.presentation.BinarySource;
import biz.c24.io.api.presentation.JavaClassSink;
import biz.c24.io.api.presentation.JavaClassSource;
import biz.c24.io.api.presentation.SAXSink;
import biz.c24.io.api.presentation.SAXSource;
import biz.c24.io.api.presentation.Sink;
import biz.c24.io.api.presentation.Source;
import biz.c24.io.api.presentation.TagValuePairSink;
import biz.c24.io.api.presentation.TextualSink;
import biz.c24.io.api.presentation.TextualSource;
import biz.c24.io.api.presentation.XMLSink;
import biz.c24.io.api.presentation.XMLSource;

import org.apache.camel.Exchange;
import org.apache.camel.model.dataformat.C24IOContentType;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.ObjectHelper;

import static org.apache.camel.util.ObjectHelper.notNull;

/**
 * A {@link DataFormat} for working with
 * <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a>
 *
 * @version $Revision$
 */
public class C24IOFormat implements DataFormat {
    private Sink sink;
    private Source source;
    private Element element;
    private Class elementType;
    private C24IOContentType contentType;

    public C24IOFormat() {
    }

    public C24IOFormat(Element element) {
        this.element = element;
    }

    public C24IOFormat(Element element, Source source, Sink sink) {
        this.element = element;
        this.source = source;
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
        Source s = getSource(exchange);
        Element e = getElement();
        notNull(s, "source");
        notNull(e, "element");

        s.setInputStream(stream);
        return s.readObject(e);
    }

    public Source getSource(Exchange exchange) {
        if (source == null) {
            C24IOContentType content = getContentType();
            if (content != null) {
                if (content == C24IOContentType.Auto) {
                    return discoverSource(exchange);
                }
                source = createSource(content);
            }
            if (source == null) {
                // lets default to the one from the element
                source = getDefaultSource();
            }
        }
        return source;
    }

    public Sink getSink(Exchange exchange) {
        if (sink == null) {
            C24IOContentType content = getContentType();
            if (content != null) {
                if (content == C24IOContentType.Auto) {
                    return discoverSink(exchange);
                }
                sink = createSink(content);
            }
            if (sink == null) {
                // lets default to the one from the element
                sink = getDefaultSink();
            }
        }
        return sink;
    }

    // Properties
    //-------------------------------------------------------------------------

    public Element getElement() {
        if (element == null) {
            Class type = getElementType();
            if (type != null) {
                element = C24IOHelper.getMandatoryElement(type);
            }
        }
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public Class getElementType() {
        return elementType;
    }

    public void setElementType(Class elementType) {
        this.elementType = elementType;
    }

    public void setSink(Sink sink) {
        this.sink = sink;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public C24IOContentType getContentType() {
        return contentType;
    }

    public void setContentType(C24IOContentType contentType) {
        this.contentType = contentType;
    }

    public Source getDefaultSource() {
        Element e = getElement();
        if (e != null) {
            return e.getModel().source();
        }
        return null;
    }

    public Sink getDefaultSink() {
        Element e = getElement();
        if (e != null) {
            return e.getModel().sink();
        }
        return null;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected Source discoverSource(Exchange exchange) {
        String mime = ExchangeHelper.getContentType(exchange);
        if (mime != null) {
            if (isXmlMimeType(mime)) {
                return new XMLSource();
            } else if (isJavaMimeType(mime)) {
                return new JavaClassSource();
            } else if (isBinaryMimeType(mime)) {
                return new BinarySource();
            } else if (isTextMimeType(mime)) {
                return new TextualSource();
            }
        }
        return getDefaultSource();
    }

    protected Source createSource(C24IOContentType content) {
        switch (content) {
        case Default:
            return getDefaultSource();
        case Binary:
            return new BinarySource();
        case Java:
            return new JavaClassSource();
        case Sax:
            return new SAXSource();
        case Text:
            return new TextualSource();
        case Xml:
            return new XMLSource();
        default:
            throw new IllegalArgumentException("Unknown format type: " + content);
        }
    }

    protected Sink discoverSink(Exchange exchange) {
        String mime = ExchangeHelper.getContentType(exchange);
        if (mime != null) {
            if (isXmlMimeType(mime)) {
                return new XMLSink();
            } else if (isJavaMimeType(mime)) {
                return new JavaClassSink();
            } else if (isBinaryMimeType(mime)) {
                return new BinarySink();
            } else if (isTextMimeType(mime)) {
                return new TextualSink();
            }
        }
        return getDefaultSink();
    }

    protected Sink createSink(C24IOContentType content) {
        switch (content) {
        case Default:
            return getDefaultSink();
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

    protected boolean isXmlMimeType(String mime) {
        return mime.equals("application/xml") || mime.contains("xml");
    }

    protected boolean isJavaMimeType(String mime) {
        return mime.equals("application/x-java-serialized-object");
    }

    protected boolean isTextMimeType(String mime) {
        return mime.startsWith("text/");
    }

    protected boolean isBinaryMimeType(String mime) {
        return mime.equals("application/octet-stream");
    }
}