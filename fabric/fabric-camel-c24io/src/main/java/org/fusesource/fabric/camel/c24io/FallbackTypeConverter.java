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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.data.DataModel;
import biz.c24.io.api.data.Element;
import biz.c24.io.api.presentation.Sink;
import biz.c24.io.api.presentation.Source;

import org.apache.camel.Exchange;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.TypeConverter;
import org.apache.camel.spi.TypeConverterAware;
import org.apache.camel.util.ObjectHelper;

/**
 * Auto-detect {@link ComplexDataObject} instances from the
 * <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a> and allow them to be
 * transformed to and from sources and sinks.
 *
 * @version $Revision$
 */
public class FallbackTypeConverter implements TypeConverter, TypeConverterAware {
    private TypeConverter parentTypeConverter;
    private boolean prettyPrint = true;
    private Sink sink;

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public void setTypeConverter(TypeConverter parentTypeConverter) {
        this.parentTypeConverter = parentTypeConverter;
    }

    public <T> T convertTo(Class<T> type, Object value) {
        return convertTo(type, null, value);
    }

    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
        try {
            if (isComplexDataObject(type)) {
                return unmarshall(type, value, exchange);
            }
            if (value instanceof ComplexDataObject) {
                marshall(type, (ComplexDataObject) value, exchange);
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeCamelException(e);
        }
    }

    protected <T> boolean isComplexDataObject(Class<T> type) {
        return ComplexDataObject.class.isAssignableFrom(type);
    }

    /**
     * Lets try parse via JAXB
     */
    protected <T> T unmarshall(Class<T> type, Object value, Exchange exchange) throws IOException {
        Element element = getElementForType(type, exchange);
        if (element == null) {
            return null;
        }

        Source source = getSource(type, element, exchange);

        boolean configured = false;
        if (parentTypeConverter != null) {
            try {
                InputStream inputStream = parentTypeConverter.mandatoryConvertTo(InputStream.class, value);
                source.setInputStream(inputStream);
                configured = true;
            } catch (NoTypeConversionAvailableException ex1) {
                try {
                    Reader reader = parentTypeConverter.mandatoryConvertTo(Reader.class, value);
                    source.setReader(reader);
                    configured = true;
                } catch (NoTypeConversionAvailableException ex2) {
                    // do nothing here
                }
            }
 
            if (!configured) {
                if (value instanceof String) {
                    value = new StringReader((String) value);
                }
                if (value instanceof InputStream) {
                    source.setInputStream((InputStream) value);
                    configured = true;
                }
                if (value instanceof Reader) {
                    source.setReader((Reader) value);
                    configured = true;
                }
            }
        }
        if (configured) {
            ComplexDataObject object = source.readObject(element);
            return ObjectHelper.cast(type, object);
        } else {
            return null;
        }
    }

    protected Element getElementForType(Class<?> type, Exchange exchange) {
        return C24IOHelper.getElement(type);
    }

    protected <T> T marshall(Class<T> type, ComplexDataObject dataObject, Exchange exchange) throws IOException {
        if (parentTypeConverter != null) {
            // TODO allow configuration to determine the sink from the Exchange

            Sink sink = getSink(dataObject, exchange);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            sink.setOutputStream(buffer);
            sink.writeObject(dataObject);

            byte[] data = buffer.toByteArray();

            try {
                return parentTypeConverter.mandatoryConvertTo(type, data);
            } catch (NoTypeConversionAvailableException e) {
                return null;
            }
        }

        return null;
    }

    protected Source getSource(Class<?> type, Element element, Exchange exchange) {
        Source answer = null;
        if (exchange != null) {
            answer = exchange.getProperty("c24io.source", Source.class);
        }
        if (answer == null) {
            DataModel model = element.getModel();
            answer = model.source();
        }
        return answer;
    }

    protected Sink getSink(ComplexDataObject dataObject, Exchange exchange) {
        Sink answer = null;
        if (exchange != null) {
            answer = exchange.getProperty("c24io.sink", Sink.class);
        }
        if (answer == null) {
            answer = dataObject.getModel().sink();
        }
        return answer;
    }

    public <T> T mandatoryConvertTo(Class<T> type, Exchange exchange, Object value) throws NoTypeConversionAvailableException {
        T answer = convertTo(type, exchange, value);
        if (answer == null) {
            throw new NoTypeConversionAvailableException(value, type);
        }
        return answer;
    }
    
    public <T> T mandatoryConvertTo(Class<T> type, Object value) throws NoTypeConversionAvailableException {
        T answer = convertTo(type, value);
        if (answer == null) {
            throw new NoTypeConversionAvailableException(value, type);
        }
        return answer;
    }

    // TODO @Override
    public <T> T tryConvertTo(Class<T> type, Exchange exchange, Object value) {
        try {
            return convertTo(type, value);
        } catch (Exception e) {
            return null;
        }
    }

    // TODO @Override
    public <T> T tryConvertTo(Class<T> type, Object value) {
        try {
            return convertTo(type, value);
        } catch (Exception e) {
            return null;
        }
    }
}
