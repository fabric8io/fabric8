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

package org.fusesource.fabric.apollo.amqp.protocol.commands

import org.fusesource.hawtbuf.Buffer

/**
 *
 */
class LinkCommand extends Command

object AttachSent {
  private val INSTANCE = new AttachSent
  def apply() = INSTANCE
}
class AttachSent extends LinkCommand

object AttachReceived {
  private val INSTANCE = new AttachReceived
  def apply() = INSTANCE
}
class AttachReceived extends LinkCommand

object DetachSent {
  private val INSTANCE = new DetachSent
  def apply() = INSTANCE
}
class DetachSent extends LinkCommand

object DetachReceived {
  private val INSTANCE = new DetachReceived
  def apply() = INSTANCE
}
class DetachReceived extends LinkCommand






