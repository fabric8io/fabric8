/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.io;


/**
 * The core lifecyle interface for ActiveMQ components.
 *
 * @version $Revision: 1.1 $
 */
public interface Service {

    /**
     * Starts the service.  No guarantee is given that the service has fully started
     * by the time this method returns.
     */
    void start() throws Exception;

    /**
     * Starts the service.  Executes the onComplete runnable once the service has fully started up.
     *
     * @param onComplete my be set to null if not interested in a callback.
     */
    void start(Runnable onComplete) throws Exception;

    /**
     * Stops the service.  No guarantee is given that the service has fully stopped
     * by the time this method returns.
     */
    void stop();

    /**
     * Stops the service.  Executes the onComplete runnable once the service has fully stopped.
     *
     * @param onComplete my be set to null if not interested in a callback.
     */
    void stop(Runnable onComplete);

}
