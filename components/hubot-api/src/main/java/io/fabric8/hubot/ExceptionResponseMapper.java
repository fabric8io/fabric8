package io.fabric8.hubot;

import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ExceptionResponseMapper implements ResponseExceptionMapper<Exception> {
    @Override
    public Exception fromResponse(Response response) {
        try {
            Object entity = response.getEntity();
            String message = "No message";
            if (entity != null) {
                message = entity.toString();
            }
            message = "HTTP " + response.getStatus() + " " + message;
            return new WebApplicationException(message, response);
        } catch (Exception ex) {
            return new Exception(
                    "Could not deserialize server side exception: " + ex.getMessage());
        }
    }
}