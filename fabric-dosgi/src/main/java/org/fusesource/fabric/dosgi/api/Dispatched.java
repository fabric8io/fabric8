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

    DispatchQueue getDispatchQueue();

}
