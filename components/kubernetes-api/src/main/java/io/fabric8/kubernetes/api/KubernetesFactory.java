/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.kubernetes.api.model.IntOrString;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple helper class for creating instances of Kubernetes
 */
public class KubernetesFactory {

    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080";

    private String address;

    public KubernetesFactory() {
        findKubernetesMaster();
        init();
    }

    public KubernetesFactory(String address) {
        this.address = address;
        if (isEmpty(address)) {
            findKubernetesMaster();
        }
        init();
    }

    protected void findKubernetesMaster() {
        this.address = resolveHttpKubernetesMaster();
    }

    private void init() {
    }

    @Override
    public String toString() {
        return "KubernetesFactory{" + address + '}';
    }

    public Kubernetes createKubernetes() {
        List<Object> providers = createProviders();
        return JAXRSClientFactory.create(address, Kubernetes.class, providers);
    }

    public KubernetesExtensions createKubernetesExtensions() {
        List<Object> providers = createProviders();
        return JAXRSClientFactory.create(address, KubernetesExtensions.class, providers);
    }

    protected List<Object> createProviders() {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJaxbJsonProvider());
        providers.add(new PlainTextJacksonProvider());
        providers.add(new JacksonIntOrStringConfig());
        return providers;
    }

    /**
     * Lets accept plain text too as if its JSON to work around some issues with the REST API and remote kube....
     */
    @javax.ws.rs.ext.Provider
    @javax.ws.rs.Consumes({"text/plain"})
    @javax.ws.rs.Produces({"text/plain"})
    public static class PlainTextJacksonProvider extends JacksonJaxbJsonProvider {
        public PlainTextJacksonProvider() {
        }

        @Override
        protected boolean hasMatchingMediaType(MediaType mediaType) {
            boolean answer = super.hasMatchingMediaType(mediaType);
            String type = mediaType.getType();
            String subtype = mediaType.getSubtype();
            if (!answer && type.equals("text")) {
                answer = super.hasMatchingMediaType(MediaType.APPLICATION_JSON_TYPE);
            }
            return answer;
        }
    }

    public String getKubernetesMaster() {
        String answer = address;
        int idx = answer.lastIndexOf(":");
        if (idx > 0) {
            answer = answer.substring(0, idx);
        }
        idx = answer.lastIndexOf(":");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        idx = answer.lastIndexOf("/");
        if (idx > 0) {
            answer = answer.substring(idx + 1);
        }
        return answer;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (isEmpty(address)) {
            findKubernetesMaster();
        }
    }

    protected static boolean isEmpty(String text) {
        return text == null || text.length() == 0;
    }

    // Helpers

    public static String resolveHttpKubernetesMaster() {
        String dockerHost = resolveKubernetesMaster();
        if (dockerHost.startsWith("tcp:")) {
            return "http:" + dockerHost.substring(4);
        }
        return dockerHost;
    }

    public static String resolveKubernetesMaster() {
        String dockerHost = System.getenv("KUBERNETES_MASTER");
        if (isEmpty(dockerHost)) {
            dockerHost = System.getProperty("kubernetes.master");
        }
        if (!isEmpty(dockerHost)) {
            return dockerHost;
        }
        return DEFAULT_KUBERNETES_MASTER;
    }

    /**
     * Creates a configured Jackson object mapper for parsing JSON
     */
    public static ObjectMapper createObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(IntOrString.class, new IntOrStringSerializer());
        module.addDeserializer(IntOrString.class, new IntOrStringDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        return mapper;
    }

    static class IntOrStringSerializer extends JsonSerializer<IntOrString> {

        @Override
        public void serialize(IntOrString value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            if (value != null) {
                Object intValue = value.getAdditionalProperties().get("intValue");
                if (intValue != null) {
                    jgen.writeNumber((Integer) intValue);
                } else {
                    Object stringValue = value.getAdditionalProperties().get("stringValue");
                    if (stringValue != null) {
                        jgen.writeString((String) stringValue);
                    } else {
                        jgen.writeNull();
                    }
                }
            } else {
                jgen.writeNull();
            }
        }

    }

    static class IntOrStringDeserializer extends JsonDeserializer<IntOrString> {

        @Override
        public IntOrString deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);

            IntOrString intOrString = new IntOrString();

            int asInt = node.asInt();
            if (asInt != 0) {
                intOrString.setAdditionalProperty("intValue", asInt);
            } else {
                intOrString.setAdditionalProperty("stringValue", node.asText());
            }

            return intOrString;
        }

    }

    public static class JacksonIntOrStringConfig implements ContextResolver<ObjectMapper> {

        public JacksonIntOrStringConfig() {

        }

        @Override
        public ObjectMapper getContext(Class<?> aClass) {
            SimpleModule module = new SimpleModule();
            module.addSerializer(IntOrString.class, new KubernetesFactory.IntOrStringSerializer());
            module.addDeserializer(IntOrString.class, new KubernetesFactory.IntOrStringDeserializer());

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(module);

            return mapper;
        }
    }

}
