package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.ErrorSchema;

import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ExceptionResponseMapper implements ResponseExceptionMapper<Exception> {
	@Override
	public Exception fromResponse(Response response) {
		try {
			
			ObjectMapper mapper = new ObjectMapper(); 
			ErrorSchema error = mapper.readValue((InputStream)response.getEntity(), ErrorSchema.class);
			
			return new KubernetesApiException(error.getMessage());
		} catch (Exception ex) {
			return new Exception(
					"Could not deserialize server side exception: "+ ex.getMessage());
		}
	}
}