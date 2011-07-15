/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.generator;

/**
 *
 */
public class Log {

    public static org.apache.maven.plugin.logging.Log LOG = null;

    public static void debug(String msg, Object... args) {
        if ( LOG != null && LOG.isDebugEnabled() ) {
            LOG.debug(String.format(msg, args));
        }
    }

    public static void info(String msg, Object... args) {
        if ( LOG != null && LOG.isInfoEnabled() ) {
            LOG.info(String.format(msg, args));
        }
    }

    public static void warn(String msg, Object... args) {
        if ( LOG != null && LOG.isWarnEnabled() ) {
            LOG.warn(String.format(msg, args));
        }
    }

    public static void error(String msg, Object... args) {
        if ( LOG != null && LOG.isErrorEnabled() ) {
            LOG.error(String.format(msg, args));
        }
    }
}
