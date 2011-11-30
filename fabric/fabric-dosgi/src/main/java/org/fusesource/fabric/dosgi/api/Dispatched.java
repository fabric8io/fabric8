/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.api;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * <p>
 * Implemented by object which expect to be call from the execution context
 * of a dispatch queue.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface Dispatched {

    DispatchQueue queue();

}
