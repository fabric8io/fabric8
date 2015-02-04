package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Status;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.InputStream;

public class ExceptionResponseMapper implements ResponseExceptionMapper<Exception> {
    @Override
    public Exception fromResponse(Response response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object entity = response.getEntity();
            String message = "No message";
            if (entity instanceof InputStream) {
                Status error = mapper.readValue((InputStream) entity, Status.class);
                if (error != null) {
                    message = error.getMessage();
                }
            } else if (entity != null) {
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