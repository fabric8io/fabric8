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

package org.fusesource.fabric.apollo.amqp.protocol.interfaces

import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.protocol.api.Connection

/**
 *
 */
abstract trait ProtocolConnection extends Connection {
  def send(data: Buffer): Unit

  def send(data: Buffer, channel: Int): Unit

  def release(session: ProtocolSession): Unit
}