package io.fabric8.gateway.apiman;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.overlord.apiman.rt.engine.beans.Application;
import org.overlord.apiman.rt.engine.beans.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RegistryInfo {

	
	public RegistryInfo() {
		super();
	}
	
	public RegistryInfo(Collection<Service> services, Collection<Application> applications) {
		super();
		this.services = services;
		this.applications = applications;
	}

	private Collection<Service> services;
	private Collection<Application> applications;

	public Collection<Service> getServices() {
		return services;
	}
	public void setServices(List<Service> services) {
		this.services = services;
	}
	public Collection<Application> getApplications() {
		return applications;
	}
	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}
	
	public String toJSON() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}
	
	public static RegistryInfo fromJSON(String json) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, RegistryInfo.class); 
	}
}
