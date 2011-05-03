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

/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fusemq.amqp.codec.AmqpDefinitions

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object AmqpConstants {

  val PROTOCOL = "amqp"
  val MAGIC = new Buffer(AmqpDefinitions.MAGIC)
  val MIN_MAX_FRAME_SIZE = AmqpDefinitions.MIN_MAX_FRAME_SIZE

}