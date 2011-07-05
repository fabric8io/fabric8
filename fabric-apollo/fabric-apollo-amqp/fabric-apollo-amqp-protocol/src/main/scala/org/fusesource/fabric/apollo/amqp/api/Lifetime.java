/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.api;

/**
 *
 */
public enum Lifetime {
    UNDEFINED,
    DELETE_ON_CLOSE,
    DELETE_ON_NO_LINKS,
    DELETE_ON_NO_MESSAGES,
    DELETE_ON_NO_LINKS_OR_MESSAGES
}
