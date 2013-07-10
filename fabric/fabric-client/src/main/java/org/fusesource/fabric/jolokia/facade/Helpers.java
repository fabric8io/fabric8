package org.fusesource.fabric.jolokia.facade;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.HasId;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector.createExecRequest;
import static org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector.createReadRequest;
import static org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector.createWriteRequest;

/**
 * @author Stan Lewis
 */
public class Helpers {

    static private ObjectMapper mapper = null;

    static List<Object> toList(Object... args) {
        List<Object> rc = new ArrayList<Object>();
        for (Object arg : args) {
            rc.add(arg);
        }
        return rc;
    }

    public static void doContainerAction(J4pClient j4p, String action, String id) {
        try {
            J4pExecRequest request = createExecRequest(action + "Container(java.lang.String)", id);
            J4pExecResponse response = j4p.execute(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to " + action + " container " + id, e);
        }
    }

    public static <T extends Object> T getFieldValue(J4pClient j4p, String operation, String id, String field) {
        T rc = null;
        Map<String, Object> value = exec(j4p, operation, id, toList(field));
        rc = (T)value.get(field);
        return rc;
    }

    public static <T extends Object> T exec(J4pClient j4p, String operation, Object ... args) {
        try {
            J4pExecRequest request = createExecRequest(operation, args);
            J4pExecResponse response = j4p.execute(request);
            return response.getValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call " + operation + " with args: " + args, e);
        }
    }

    public static <T extends Object> T write(J4pClient j4p, String attribute, Object value) {
        try {
            J4pWriteRequest request = createWriteRequest(attribute, value);
            J4pWriteResponse response = j4p.execute(request);
            return response.getValue();

        } catch (Exception e) {
            throw new RuntimeException("Failed to write " + attribute + " using new value " + value, e);
        }
    }

    public static <T extends Object> T read(J4pClient j4p, String attribute) {
        try {
            J4pReadRequest request = createReadRequest(attribute);
            J4pReadResponse response = j4p.execute(request);
            return response.getValue();

        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + attribute, e);
        }
    }

    public static List<String> extractIds(HasId[] things) {
        return extractIds(Arrays.asList(things));
    }

    public static List<String> extractIds(List<HasId> things) {
        List<String> answer = new ArrayList<String>();
        for (HasId thing : things) {
            answer.add(thing.getId());
        }
        return answer;
    }

    public static ObjectMapper getObjectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.getDeserializationConfig().addMixInAnnotations(CreateContainerMetadata.class, FieldsToIgnoreForDeserializationMixin.class);
            mapper.getDeserializationConfig().addMixInAnnotations(CreateContainerOptions.class, FieldsToIgnoreForDeserializationMixin.class);
            mapper.getSerializationConfig().addMixInAnnotations(CreateContainerOptions.class, FieldsToIgnoreForSerializationMixin.class);
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return mapper;
    }
}
