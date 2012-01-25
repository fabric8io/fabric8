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

package org.fusesource.fabric.apollo.amqp.protocol.utilities

import collection.mutable.Queue
import org.fusesource.hawtdispatch._

/**
 *
 */

object Tasks {
  def apply() = new Queue[() => Unit]

  def apply(args:(() => Unit)*) = {
    val rc = new Queue[() => Unit]
    args.foreach((x) => rc.enqueue(x))
    rc
  }
}

object OptionalTask {
  def apply(args:Option[() => Unit]*) = {
    val rc = new Queue[() => Unit]
    args.foreach((x) => x.foreach((x) => rc.enqueue(x)))
    rc
  }
}

object execute {
  def apply(q:Queue[() => Unit]) = {
    if (!q.isEmpty) {
      Option(Dispatch.getCurrentQueue) match {
        case Some(queue) =>
          q.dequeueAll((x) => {
            queue {
              x()
            }
            true
          })
        case None =>
          q.dequeueAll((x) => {x(); true})
      }
    }
  }
}