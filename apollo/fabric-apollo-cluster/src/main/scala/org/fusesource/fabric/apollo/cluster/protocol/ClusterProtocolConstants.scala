/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
