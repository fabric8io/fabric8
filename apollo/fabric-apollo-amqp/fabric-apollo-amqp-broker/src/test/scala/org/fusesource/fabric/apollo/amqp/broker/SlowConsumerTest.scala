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

package org.fusesource.fabric.apollo.amqp.broker

import org.scalatest.matchers.ShouldMatchers
import org.fusesource.hawtdispatch._
import scala.math.max
import org.fusesource.fabric.apollo.amqp.api._
import java.util.concurrent.{TimeUnit, CountDownLatch}
import java.util.concurrent.atomic.AtomicLong
import org.fusesource.hawtbuf.Buffer
import org.apache.activemq.apollo.util.Logging

/**
 *
 */

class SlowConsumerTest extends BrokerTestSupport with ShouldMatchers with Logging {

  for (destination <- List("queue:foo")) { //, "topic:foo")) {

    test("send a bunch of messages via " + destination + " to a slow consumer") {

      val expected_messages = 50
      val received_messages = new AtomicLong(0)

      def create_receiver(slow:Boolean) = {
        val rc = new CountDownLatch(1)
        val connection = AmqpConnectionFactory.create
        connection.setOnClose(^{
          rc.countDown
        })
        connection.connect(getConnectionUri, ^{
          val session = connection.createSession
          session.begin(^{
            val receiver = session.createReceiver
            receiver.setAddress(destination)
            receiver.setOnDetach(^ {
              session.end(^ {
                connection.close
              })
            })
            receiver.setListener(new MessageListener {

              var sleeping = false
              var _refiller = ^{}

              var count = 0

              def watchdog(last_count:Int): Unit = {
                connection.getDispatchQueue.executeAfter(3000, TimeUnit.MILLISECONDS, ^{
                  if (count == last_count) {
                    info("Haven't received messages in the past 3 seconds, timing out receiver")
                    receiver.detach
                  } else {
                    watchdog(count)
                  }
                })
              }

              def needLinkCredit(available: Long) = max(available, 10)

              def refiller(refiller: Runnable) = _refiller = refiller

              def offer(receiver: Receiver, message: Message) = {
                if (sleeping) {
                  false
                } else {
                  def settle = {
                    info("\"%s\" got message %s", receiver.getName, message)
                    if ( !message.getSettled ) {
                      receiver.settle(message, Outcome.ACCEPTED)
                    }
                    received_messages.incrementAndGet
                    if (count == 0) {
                      watchdog(count)
                    }
                    count = count + 1
                  }
                  if (slow) {
                    sleeping = true
                    connection.getDispatchQueue.after(200, TimeUnit.MILLISECONDS) {
                      sleeping = false
                      settle
                      _refiller.run
                    }
                  } else {
                    settle
                  }
                  true
                }
              }

              def full = sleeping
            })

            receiver.setName(slow + " test receiver link")
            receiver.attach(^{})

          })
        })

        rc
      }

      def create_sender = {
        val rc = new CountDownLatch(1)
        val connection = AmqpConnectionFactory.create
        connection.setOnClose(^{rc.countDown})
        connection.connect(getConnectionUri, ^{
          val session = connection.createSession
          session.begin(^{
            val sender = session.createSender()
            sender.setAddress(destination)
            sender.setName("Test Sender Link")
            sender.setOnDetach(^{
              session.end(^{
                connection.close
              })
            })
            sender.attach(^{
              debug("Sender attached")
              for (x <- (1 to expected_messages).toList) {
                debug("putting message %s", x)
                val message = sender.getSession.createMessage
                message.setSettled(false)
                message.getHeader.setDurable(true)
                message.addBodyPart(new Buffer(("message #" + x).getBytes))
                if (x >= expected_messages) {
                  message.onAck(^{
                    sender.detach
                  })
                }
                sender.put(message)
              }
            })
          })
        })
        rc
      }


      val receiver1 = create_receiver(true)
      val receiver2 = create_receiver(false)

      val sender = create_sender
      sender.await(expected_messages * 100, TimeUnit.MILLISECONDS) should be (true)


      receiver1.await(expected_messages * 5000, TimeUnit.MILLISECONDS) should be (true)
      receiver2.await(expected_messages * 1000, TimeUnit.MILLISECONDS) should be (true)
      if (destination.startsWith("topic")) {
        received_messages.get should be (expected_messages * 2)
      } else {
        received_messages.get should be (expected_messages)
      }



    }
  }

}
