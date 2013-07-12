package org.fusesource.fabric.jolokia.facade;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.fusesource.fabric.api.Container;

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

