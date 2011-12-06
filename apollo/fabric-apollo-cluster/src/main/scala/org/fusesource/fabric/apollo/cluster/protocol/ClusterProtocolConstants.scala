/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.cluster.protocol
import org.fusesource.hawtbuf.Buffer._
import org.fusesource.hawtbuf.Buffer

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ClusterProtocolConstants {

  val PROTOCOL_NAME = "cluster"
  val PROTOCOL_VERSION = "1.0"

  val PROTOCOL_MAGIC = ascii("fusesource:cluster:")

  //
  // Hello is the first frame exchanged
  val COMMAND_HELLO = 0

  // Channels are used to send messages from producers to destinations and
  // from destinations to consumers.
  val COMMAND_CHANNEL_OPEN = 1
  val COMMAND_CHANNEL_SEND = 2
  val COMMAND_CHANNEL_ACK = 3
  val COMMAND_CHANNEL_CLOSE = 4

  // Consumer add/removes are used to notify destination masters about
  // active consumers that are available to receive messages.
  val COMMAND_ADD_CONSUMER = 5
  val COMMAND_REMOVE_CONSUMER = 6

  val EMPTY_BUFFER = new Buffer(0)

}
