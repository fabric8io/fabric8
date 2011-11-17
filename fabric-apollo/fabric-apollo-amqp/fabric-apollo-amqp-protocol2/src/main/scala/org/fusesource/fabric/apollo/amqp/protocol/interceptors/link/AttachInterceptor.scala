package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link

/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

import org.fusesource.fabric.apollo.amqp.codec.types.Attach
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.PerformativeInterceptor
import org.apache.activemq.apollo.util.Logging

/**
 *
 */
class AttachInterceptor extends PerformativeInterceptor[Attach] with Logging {

}