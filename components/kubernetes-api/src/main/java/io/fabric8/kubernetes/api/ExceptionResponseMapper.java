package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ErrorSchema;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;

import javax.ws.rs.core.Response;
import java.io.InputStream;

public class ExceptionResponseMapper implements ResponseExceptionMapper<Exception> {
    @Override
    public Exception fromResponse(Response response) {
        try {

            ObjectMapper mapper = new ObjectMapper();
            ErrorSchema error = mapper.readValue((InputStream) response.getEntity(), ErrorSchema.class);

            return new KubernetesApiException(error.getMessage());
        } catch (Exception ex) {
            return new Exception(
                    "Could not deserialize server side exception: " + ex.getMessage());
        }
    }
}