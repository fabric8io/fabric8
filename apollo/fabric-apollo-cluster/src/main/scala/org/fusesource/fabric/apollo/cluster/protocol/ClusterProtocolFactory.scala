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

import org.apache.activemq.apollo.broker.store.MessageRecord
import org.apache.activemq.apollo.broker.Message

import org.fusesource.hawtbuf.Buffer
import ClusterProtocolConstants._
import org.apache.activemq.apollo.broker.protocol._

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterProtocolCodecFactory extends ProtocolCodecFactory.Provider {

  def id = PROTOCOL_NAME

  def maxIdentificaionLength = PROTOCOL_MAGIC.length

  def matchesIdentification(header: Buffer) = {
    if (header.length < PROTOCOL_MAGIC.length) {
      false
    } else {
      header.startsWith(PROTOCOL_MAGIC)
    }
  }

  def isIdentifiable = true

  def createProtocolCodec = new ClusterProtocolCodec
}


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ClusterProtocolFactory extends ProtocolFactory {
  def create(config: String) = if(config == PROTOCOL_NAME) {
    ClusterProtocol
  } else {
    null
  }
  def create() = ClusterProtocol
}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ClusterProtocol extends ClusterProtocolCodecFactory with Protocol {

  def encode(message: Message) = throw new UnsupportedOperationException

  def decode(message: MessageRecord) = throw new UnsupportedOperationException

  def createProtocolHandler = new ClusterProtocolHandler

}
