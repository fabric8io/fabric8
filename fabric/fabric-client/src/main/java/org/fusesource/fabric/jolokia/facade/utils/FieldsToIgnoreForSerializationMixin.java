package org.fusesource.fabric.jolokia.facade.utils;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.fusesource.fabric.api.CreationStateListener;

/**
 * @author Stan Lewis
 */
public abstract class FieldsToIgnoreForSerializationMixin {

    @JsonIgnore
    public abstract CreationStateListener getCreationStateListener();

}
