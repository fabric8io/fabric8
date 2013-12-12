package io.fabric8.api;

public class DynamicReferenceException extends FabricException {

    public DynamicReferenceException() {
    }

    public DynamicReferenceException(String message) {
        super(message);
    }

    public DynamicReferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicReferenceException(Throwable cause) {
        super(cause);
    }
}
