/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
