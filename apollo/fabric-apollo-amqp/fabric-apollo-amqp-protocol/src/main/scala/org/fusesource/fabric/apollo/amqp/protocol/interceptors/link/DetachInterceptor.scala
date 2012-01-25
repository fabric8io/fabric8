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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link

import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types.Detach
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.PerformativeInterceptor
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.utilities.execute

/**
 *
 */
class DetachInterceptor extends PerformativeInterceptor[Detach] with Logging {

  var sent = false
  var received = false

  override protected def receive(performative: Detach, payload: Buffer, tasks: Queue[() => Unit]) = {
    execute(tasks)
    true
  }

}