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

package org.fusesource.fabric.apollo.amqp.protocol.commands
import org.fusesource.hawtdispatch.transport._


/**
 *
 */
class ConnectionCommand extends Command

object CloseConnection {
  private val INSTANCE = new CloseConnection

  def apply() = INSTANCE

  def apply(reason:String) = {
    val rc = new CloseConnection
    rc.reason = Option(reason)
    rc
  }

  def apply(reason:Throwable) = {
    val rc = new CloseConnection
    rc.exception = Option(reason)
    rc
  }
}
class CloseConnection extends ConnectionCommand {
  var reason:Option[String] = None
  var exception:Option[Throwable] = None
}

object ConnectionClosed {
  private val INSTANCE = new ConnectionClosed
  def apply() = INSTANCE
}
class ConnectionClosed extends ConnectionCommand

object ConnectionCreated {
  def apply(transport:Transport) = new ConnectionCreated(transport)
}
class ConnectionCreated(val transport:Transport) extends ConnectionCommand

object HeaderSent {
  private val INSTANCE = new HeaderSent
  def apply() = INSTANCE
}
class HeaderSent extends ConnectionCommand

object HeaderReceived {
  private val INSTANCE = new HeaderReceived
  def apply() = INSTANCE
}
class HeaderReceived extends ConnectionCommand

object SendOpen {
  private val INSTANCE = new SendOpen
  def apply() = INSTANCE
}
class SendOpen extends ConnectionCommand

object OpenSent {
  private val INSTANCE = new OpenSent
  def apply() = INSTANCE
}
class OpenSent extends ConnectionCommand

object OpenReceived {
  private val INSTANCE = new OpenReceived
  def apply() = INSTANCE
}
class OpenReceived extends ConnectionCommand

object CloseSent {
  private val INSTANCE = new CloseSent
  def apply() = INSTANCE
}
class CloseSent extends ConnectionCommand

object CloseReceived {
  private val INSTANCE = new CloseSent
  def apply() = INSTANCE
}
class CloseReceived extends ConnectionCommand
