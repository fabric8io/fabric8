/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.protocol

import org.fusesource.fusemq.amqp.codec.AmqpProtocolHeader
import org.fusesource.fusemq.amqp.codec.types.AmqpOpen

/**
 *
 */
trait ConnectionHandler {
  def header(header: AmqpProtocolHeader): Unit

  def close: Unit

  def open(open: AmqpOpen): Unit
}