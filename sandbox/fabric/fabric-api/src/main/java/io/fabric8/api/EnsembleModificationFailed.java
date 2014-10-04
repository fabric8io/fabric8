/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
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
