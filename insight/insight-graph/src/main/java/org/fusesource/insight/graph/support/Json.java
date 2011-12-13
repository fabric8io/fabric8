/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.graph.support;

import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class Json {
    public static <T> T readJsonValue(String name, InputStream in, Class<T> aClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // lets tweak the class loader so we can find the classes
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = GraphiteWriter.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            return mapper.readValue(in, aClass);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
