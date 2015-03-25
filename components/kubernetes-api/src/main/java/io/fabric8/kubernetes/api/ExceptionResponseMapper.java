package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.base.Status;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

public class ExceptionResponseMapper implements ResponseExceptionMapper<Exception> {
    private static final transient Logger LOG = LoggerFactory.getLogger(ExceptionResponseMapper.class);

    @Override
    public Exception fromResponse(Response response) {
        try {
            Object entity = response.getEntity();
            String message = extractErrorMessage(entity);
            message = "HTTP " + response.getStatus() + " " + message;
            return new WebApplicationException(message, response);
        } catch (Exception ex) {
            return new Exception(
                    "Could not deserialize server side exception: " + ex.getMessage());
        }
    }

    public static String extractErrorMessage(Object entity) {
        String message = "No message";
        ObjectMapper mapper = KubernetesFactory.createObjectMapper();
        try {
            String json = null;
            if (entity instanceof InputStream) {
                InputStream inputStream = (InputStream) entity;
                json = IOHelpers.readFully(inputStream);
            } else if (entity != null) {
                json = entity.toString();
            }
            if (Strings.isNotBlank(json)) {
                message = json;
                if (textLooksLikeJsonObject(json)) {
                    try {
                        Status error = mapper.readValue(json, Status.class);
                        if (error != null) {
                            message = error.getMessage();
                            if (Strings.isNullOrBlank(message)) {
                                message = error.getReason();
                            }
                        }
                    } catch (IOException e) {
                        LOG.warn("Failed to parse Status from JSON:" + json + ". Reason: " + e, e);
                    }
                }
            }
        } catch (Exception e) {
            message = "Failed to extract message from request: " + e;
        }
        return message;
    }

    protected static boolean textLooksLikeJsonObject(String text) {
        text = text.trim();
        return text.startsWith("{") && text.endsWith("}");
    }
}