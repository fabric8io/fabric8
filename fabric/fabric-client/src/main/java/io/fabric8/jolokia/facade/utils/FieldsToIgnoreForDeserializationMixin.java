package io.fabric8.jolokia.facade.utils;

import org.codehaus.jackson.annotate.JsonIgnore;
import io.fabric8.api.Container;

import java.net.URI;

/**
 * @author Stan Lewis
 */
public abstract class FieldsToIgnoreForDeserializationMixin {

    @JsonIgnore
    public abstract Container getContainer();


    @JsonIgnore
    public abstract URI getProxyUri();

    @JsonIgnore
    public abstract void setProxyUri(URI proxyUri);
}

