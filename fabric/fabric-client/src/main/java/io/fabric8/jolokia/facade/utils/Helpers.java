package io.fabric8.jolokia.facade.utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.HasId;
import io.fabric8.jolokia.facade.mbeans.MBeans;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.*;

import javax.management.MalformedObjectNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public class Helpers {

    private static ObjectMapper mapper = null;

    public static List<Object> toList(Object... args) {
        List<Object> rc = new ArrayList<Object>();
        for (Object arg : args) {
            rc.add(arg);
        }
        return rc;
    }

    public static J4pExecRequest createExecRequest(String operation, Object... args) throws MalformedObjectNameException {
        J4pExecRequest rc = new J4pExecRequest(MBeans.FABRIC.getUrl(), operation, args);
        rc.setPreferredHttpMethod("POST");
        return rc;
    }

    public static J4pExecRequest createCustomExecRequest(String mbeanUrl, String operation, Object... args) throws MalformedObjectNameException {
        J4pExecRequest rc = new J4pExecRequest(mbeanUrl, operation, args);
        rc.setPreferredHttpMethod("POST");
        return rc;
    }

    public static J4pWriteRequest createWriteRequest(String attribute, Object value) throws MalformedObjectNameException {
        J4pWriteRequest answer = new J4pWriteRequest(MBeans.FABRIC.getUrl(), attribute, value);
        answer.setPreferredHttpMethod("POST");
        return answer;
    }

    public static J4pReadRequest createReadRequest(String attribute) throws MalformedObjectNameException {
        J4pReadRequest answer = null;
        if (attribute == null || attribute.toString().length() < 1) {
            answer = new J4pReadRequest(MBeans.FABRIC.getUrl());
        } else {
            answer = new J4pReadRequest(MBeans.FABRIC.getUrl(), attribute);
        }
        answer.setPreferredHttpMethod("POST");
        return answer;
    }

    public static J4pReadRequest createCustomReadRequest(String mbeanUrl, String attribute) throws MalformedObjectNameException {
        J4pReadRequest answer = null;
        if (attribute == null || attribute.toString().length() < 1) {
            answer = new J4pReadRequest(mbeanUrl);
        } else {
            answer = new J4pReadRequest(mbeanUrl, attribute);
        }
        answer.setPreferredHttpMethod("POST");
        return answer;
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
            //System.out.println(response.getValue().toString());
            return response.getValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call " + operation + " with args: " + args, e);
        }
    }

    /**
     * executes an operation and returns the json result value
     *
     * @param j4p
     * @param operation
     * @param args
     * @return
     */
    public static String execToJSON(J4pClient j4p, String operation, Object ... args) {
        try {
            J4pExecRequest request = createExecRequest(operation, args);
            J4pExecResponse response = j4p.execute(request);
            return response.getValue().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call " + operation + " with args: " + args, e);
        }
    }

    public static String execCustomToJSON(J4pClient j4p, String mbeanUrl, String operation, Object ... args) {
        try {
            J4pExecRequest request = createCustomExecRequest(mbeanUrl, operation, args);
            J4pExecResponse response = j4p.execute(request);
            return response.getValue().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call " + operation + " with args: " + args, e);
        }
    }

    public static String readToJSON(J4pClient j4p, String attribute) {
        try {
            J4pReadRequest request = createReadRequest(attribute);
            J4pReadResponse response = j4p.execute(request);
            return response.getValue().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + attribute, e);
        }
    }

    public static String readCustomToJSON(J4pClient j4p, String mbeanUrl, String attribute) {
        try {
            J4pReadRequest request = createCustomReadRequest(mbeanUrl, attribute);
            J4pReadResponse response = j4p.execute(request);
            return response.getValue().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + attribute, e);
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
