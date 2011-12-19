/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.graph.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 */
public class JsonUnmarshaller<T> implements Unmarshaller<T> {
    private final Class<T> outputType;

    public JsonUnmarshaller(Class<T> outputType) {
        this.outputType = outputType;
    }

    @Override
    public T unmarshal(String path, byte[] data) throws IOException {
        return (T) Json.readJsonValue(path, new ByteArrayInputStream(data), outputType);
    }
}
