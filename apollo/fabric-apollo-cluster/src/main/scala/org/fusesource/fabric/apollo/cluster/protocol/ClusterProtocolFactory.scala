/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
