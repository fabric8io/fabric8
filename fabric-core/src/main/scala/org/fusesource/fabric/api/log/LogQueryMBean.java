/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log;

import org.codehaus.jackson.JsonGenerationException;

import java.io.IOException;

/**
 * An MBean for querying log events
 */
public interface LogQueryMBean {

    /**
     * Returns the recent log events as JSON
     *
     * @param count maximum number to return o <0 for all of them
     * @return the log events as a blob of JSON
     */
    public String getLogEvents(int count) throws IOException;

}
