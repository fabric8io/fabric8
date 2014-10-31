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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.Generated;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = IntOrString.Deserializer.class)
@JsonSerialize(using = IntOrString.Serializer.class)
@JsonPropertyOrder({
    "stringValue",
    "intValue"
})
public class IntOrString {

    /**
     * 
     */
    @JsonProperty("stringValue")
    private String stringValue;
    /**
     * 
     */
    @JsonProperty("intValue")
    private Integer intValue;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The stringValue
     */
    @JsonProperty("stringValue")
    public String getStringValue() {
        return stringValue;
    }

    /**
     * 
     * @param stringValue
     *     The stringValue
     */
    @JsonProperty("stringValue")
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * 
     * @return
     *     The intValue
     */
    @JsonProperty("intValue")
    public Integer getIntValue() {
        return intValue;
    }

    /**
     * 
     * @param intValue
     *     The intValue
     */
    @JsonProperty("intValue")
    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
    
           
    public static class Serializer extends JsonSerializer<IntOrString> {

        @Override
        public void serialize(IntOrString value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            if (value != null) {
                Integer intValue = value.getIntValue();
                if (intValue != null) {
                    jgen.writeNumber(intValue);
                } else {
                    String stringValue = value.getStringValue();
                    if (stringValue != null) {
                        jgen.writeString(stringValue);
                    } else {
                        jgen.writeNull();
                    }
                }
            } else {
                jgen.writeNull();
            }
        }

    }

    public static class Deserializer extends JsonDeserializer<IntOrString> {

        @Override
        public IntOrString deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            IntOrString intOrString = new IntOrString();
            int asInt = node.asInt();
            if (asInt != 0) {
                intOrString.setIntValue(asInt);
            } else {
                intOrString.setStringValue(node.asText());
            }
            return intOrString;
        }

    }

}
