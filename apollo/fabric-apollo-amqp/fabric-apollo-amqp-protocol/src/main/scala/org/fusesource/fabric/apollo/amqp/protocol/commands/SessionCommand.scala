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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame

/**
 *
 */

class SessionCommand extends Command

object BeginSession {
  private val INSTANCE = new BeginSession
  def apply() = INSTANCE
}
class BeginSession extends SessionCommand

object EndSession {
  private val INSTANCE = new EndSession

  def apply() = INSTANCE

  def apply(reason:String) = {
    val rc = new EndSession
    rc.reason = Option(reason)
    rc
  }

  def apply(reason:Throwable) = {
    val rc = new EndSession
    rc.exception = Option(reason)
    rc
  }
}

class EndSession extends SessionCommand {
  var reason:Option[String] = None
  var exception:Option[Throwable] = None
}

object BeginSent {
  private val INSTANCE = new BeginSent
  def apply() = INSTANCE
}
class BeginSent extends SessionCommand

object BeginReceived {
  private val INSTANCE = new BeginReceived
  def apply() = INSTANCE
}
class BeginReceived extends SessionCommand

object EndSent {
  private val INSTANCE = new EndSent
  def apply() = INSTANCE
}
class EndSent extends SessionCommand

object EndReceived {
  private val INSTANCE = new EndReceived
  def apply() = INSTANCE
}
class EndReceived extends SessionCommand