/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

public class FabricException extends RuntimeException {

    public FabricException() {
    }

    public FabricException(String message) {
        super(message);
    }

    public FabricException(String message, Throwable cause) {
        super(message, cause);
    }

    public FabricException(Throwable cause) {
        super(cause);
    }
}
