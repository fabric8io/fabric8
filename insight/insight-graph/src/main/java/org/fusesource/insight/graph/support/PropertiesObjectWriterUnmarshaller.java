/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.graph.support;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 */
public class PropertiesObjectWriterUnmarshaller implements Unmarshaller<OutputWriter> {
    @Override
    public OutputWriter unmarshal(String path, byte[] data) throws IOException {
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(data));

        // TODO
        // lets figure out the class
        OutputWriter answer = new GraphiteWriter();

        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            if (key != null) {
                map.put(key.toString(), entry.getValue());
            }
        }
        answer.setSettings(map);
        return answer;
    }
}
