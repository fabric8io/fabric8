
package io.fabric8.kubernetes.api.model;

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
@Generated("org.jsonschema2pojo")
@JsonDeserialize(using = IntOrString.Deserializer.class)
@JsonSerialize(using = IntOrString.Serializer.class)
@JsonPropertyOrder({
    "IntVal",
    "Kind",
    "StrVal"
})
public class IntOrString {

    @JsonProperty("IntVal")
    private Integer IntVal;
    @JsonProperty("Kind")
    private Integer Kind;
    @JsonProperty("StrVal")
    private String StrVal;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();



    /**
     * 
     * @return
     *     The IntVal
     */
    @JsonProperty("IntVal")
    public Integer getIntVal() {
        return IntVal;
    }

    /**
     * 
     * @param IntVal
     *     The IntVal
     */
    @JsonProperty("IntVal")
    public void setIntVal(Integer IntVal) {
        this.IntVal = IntVal;
    }

    /**
     * 
     * @return
     *     The Kind
     */
    @JsonProperty("Kind")
    public Integer getKind() {
        return Kind;
    }

    /**
     * 
     * @param Kind
     *     The Kind
     */
    @JsonProperty("Kind")
    public void setKind(Integer Kind) {
        this.Kind = Kind;
    }

    /**
     * 
     * @return
     *     The StrVal
     */
    @JsonProperty("StrVal")
    public String getStrVal() {
        return StrVal;
    }

    /**
     * 
     * @param StrVal
     *     The StrVal
     */
    @JsonProperty("StrVal")
    public void setStrVal(String StrVal) {
        this.StrVal = StrVal;
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
                Integer intValue = value.getIntVal();
                if (intValue != null) {
                    jgen.writeNumber(intValue);
                } else {
                    String stringValue = value.getStrVal();
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
                intOrString.setIntVal(asInt);
            } else {
                intOrString.setStrVal(node.asText());
            }
            return intOrString;
        }

    }
}
