package io.fabric8.api;

public class EnsembleModificationFailed extends FabricException {

    private final Reason reason;

    public Reason getReason() {
        return reason;
    }

    public EnsembleModificationFailed(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public EnsembleModificationFailed(String message, Throwable cause, Reason reason) {
        super(message, cause);
        this.reason = reason;
    }

    public EnsembleModificationFailed(Throwable cause, Reason reason) {
        super(cause);
        this.reason = reason;
    }

    public static EnsembleModificationFailed launderThrowable(Throwable cause) {
        if (cause instanceof EnsembleModificationFailed) {
            return (EnsembleModificationFailed) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        } else {
            return new EnsembleModificationFailed(cause, Reason.UNKNOWN);
        }
    }

    public enum Reason {
        INVALID_ARGUMENTS,
        ILLEGAL_STATE,
        CONTAINERS_NOT_ALIVE,
        CONTAINERS_ALREADY_IN_ENSEMBLE,
        CONTAINERS_NOT_IN_ENSEMBLE,
        TIMEOUT,
        UNKNOWN
    }
}
