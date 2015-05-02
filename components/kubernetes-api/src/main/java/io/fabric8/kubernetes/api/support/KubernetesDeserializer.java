/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.io.IOException;
import java.util.Map;

/**
 * A custom deserializer that uses the 'kind' attribute in the JSON to figure out which Kubernetes / OpenShift DTO class to use
 */
public class KubernetesDeserializer extends StdDeserializer<Object> {
    private final Map<String, Class<?>> kindToClasses;

    public KubernetesDeserializer(Map<String, Class<?>> kindToClasses) {
        super(Object.class);
        this.kindToClasses = kindToClasses;
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        TreeNode treeNode = mapper.readTree(jp);
        if (treeNode.isObject()) {
            ObjectNode root = (ObjectNode) treeNode;
            Class<?> realClass = null;

            JsonNode kind = root.get("kind");
            if (kind != null) {
                String kindText = kind.textValue();
                Class<?> classForKind = kindToClasses.get(kindText);
                if (classForKind != null) {
                    return mapper.treeToValue(root, classForKind);
                }
            }
        } else {
            ValueNode valueNode = (ValueNode) treeNode;
            if (valueNode.isTextual()) {
                return valueNode.textValue();
            } else if (valueNode.isNumber()) {
                return valueNode.numberValue();
            }
        }
        return null;
    }


}
