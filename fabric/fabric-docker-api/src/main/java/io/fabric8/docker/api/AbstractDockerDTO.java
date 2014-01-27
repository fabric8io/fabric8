package io.fabric8.docker.api;

import io.fabric8.docker.api.support.DockerPropertyNamingStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Abstract class to be extended by every Docker REST DTO. (de)serialization.
 */
@JsonNaming(DockerPropertyNamingStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractDockerDTO {

}