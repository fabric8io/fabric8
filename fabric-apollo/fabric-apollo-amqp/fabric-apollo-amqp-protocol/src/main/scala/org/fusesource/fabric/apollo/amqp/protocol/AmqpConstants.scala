/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object AmqpConstants {

  val PROTOCOL = "amqp"
  val MAGIC = new Buffer(AMQPDefinitions.MAGIC)
  val MIN_MAX_FRAME_SIZE = AMQPDefinitions.MIN_MAX_FRAME_SIZE

}
