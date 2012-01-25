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

import java.util.concurrent.{TimeUnit, CountDownLatch}
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.api._
import org.fusesource.hawtdispatch._
import org.scalatest.matchers.ShouldMatchers
import scala.math._
import org.fusesource.hawtbuf.Buffer
import collection.mutable.{HashMap, ListBuffer}


class BasicUseCasesTest extends BrokerTestSupport with ShouldMatchers {

  val test_address = "test"
  val queue_prefix = "queue:"
  val topic_prefix = "topic:"

  def create_receiver(number:Int, prefix: String, recv: ListBuffer[String], receiver_close_latch: CountDownLatch) = {

    var receiver:Receiver = null

    val latch = new CountDownLatch(1)
    val receiver_connection = AmqpConnectionFactory.create
    receiver_connection.setOnClose(^{
      debug("Receiver disconnected")
      receiver_close_latch.countDown
    })
    receiver_connection.connect(getConnectionUri, ^ {
      val session = receiver_connection.createSession
      session.begin(^ {
        receiver = session.createReceiver
        receiver.setName("receiver" + number)
        receiver.setAddress(prefix + test_address)
        receiver.setOnDetach(^{
          session.end(^ {
            receiver_connection.close
          })
        })
        receiver.setListener(new MessageListener with Logging {

          val queue = Dispatch.createQueue
          var count = 0

          def watchdog(last_count:Int): Unit = {
            queue.executeAfter(500, TimeUnit.MILLISECONDS, ^{
              if (count == last_count) {
                receiver.detach
              } else {
                watchdog(count)
              }
            })
          }

          def needLinkCredit(available: Long) = max(available, 1)

          def refiller(refiller: Runnable) = {}

          def offer(receiver: Receiver, message: Message) = {
            info("\"%s\" received \"%s\"", receiver.getName, message)
            if ( message.getBodyPart(0) == null ) {
              throw new RuntimeException("Message is null")
            }
            if ( !message.getSettled ) {
              receiver.settle(message, Outcome.ACCEPTED)
            }
            val msg = message.getBodyPart(0).asInstanceOf[Buffer].ascii.toString
            recv.append(msg)
            if (count == 0) {
              watchdog(count)
            }
            count += 1

            true
          }

          def full = false

        })
        receiver.attach(^ {
          info("Attached receiver %s", receiver.getName)
          latch.countDown
        })
      })
    })
    latch.await(30, TimeUnit.SECONDS) should be (true)
    receiver
  }

  def create_sender(number:Int, prefix: String, sent: ListBuffer[Int], sent_latch: CountDownLatch, sender_close_latch: CountDownLatch, max_messages: Int, settled: Boolean, durable: Boolean) = {

    val latch = new CountDownLatch(1)

    var sender:Sender = null

    val sender_connection = AmqpConnectionFactory.create
    sender_connection.setOnClose(^{
      debug("Disconnected sender")
      sender_close_latch.countDown
    })
    sender_connection.connect(getConnectionUri, ^ {
      val session = sender_connection.createSession
      session.begin(^ {
        sender = session.createSender
        sender.setName("sender" + number)
        sender.setAddress(prefix + test_address)
        sender.setOnDetach(^ {
          session.end(^ {
            sender_connection.close
          })
        })
        sender.attach(^ {
          latch.countDown
          def put(x: Int, max: Int): Unit = {
            val message = sender.getSession.createMessage
            message.setSettled(settled)
            message.getHeader.setDurable(durable)

            message.addBodyPart((number + ":" + x).getBytes)

            message.onSend(^ {
              info("Sent message #" + x)
            })

            message.onAck(^ {
              message.getOutcome match {
                case Outcome.ACCEPTED =>
                  debug("Message %s accepted", x)
                  sent.append(x)
                  sent_latch.countDown
                  if ( sent_latch.getCount <= 0 ) {
                    sender.detach
                  } else {
                    put(x + 1, max_messages)
                  }
                // TODO - will change when MODIFIED/RELEASED are implemented
                case _ =>
                  debug("Message %s %s", x, message.getOutcome)
                  message.setSettled(false)
                  message.onSend(^ {
                    debug("Resent message %s", x)
                  })
                  debug("Resending message %s, failures %s", x, message.getHeader.getDeliveryFailures)
                  sender.put(message)
              }
            })
            debug("Sending message %s", x)
            sender.put(message)
          }

          put(1, max_messages)

        })
      })
    })

    latch.await(30, TimeUnit.SECONDS) should be (true)
    sender
  }

  test("Connect sender to a queue with no receiver attached and send a few messages") {
    val max_messages = 50
    val wait_time = max_messages
    val sent_latch = new CountDownLatch(max_messages)
    val sender_close_latch = new CountDownLatch(1)
    val sent = new ListBuffer[Int]
    create_sender(1, queue_prefix, sent, sent_latch, sender_close_latch, max_messages, false, true)
    sent_latch.await(wait_time, TimeUnit.SECONDS) should be (true)
    sender_close_latch.await(5, TimeUnit.SECONDS) should be (true)
  }

  // Test several combinations of message amounts, number of senders/receivers, etc.
  for (
    max_messages <- List(50);
    num_senders <- List(1, 10);
    num_receivers <- List(1, 10);
    prefix:String <- List(queue_prefix, topic_prefix);
    settled <- List(false);
    durable <- List(false)
  ) {

    val name = "Connect " + num_senders +
      " sender(s) to a " + prefix.stripSuffix(":") +
      ", send " + max_messages +
      " messages with settled=" + settled +
      " durable=" + durable +
      " and receive the messages with " + num_receivers +
      " receivers"

    // These fail intermittently
    ignore(name) {
      println(_testName.get)
      info("Starting %s", _testName.get)
      val wait_time = max_messages

      val expected_sent = max_messages * num_senders
      var expected_received = max_messages * num_senders

      if (prefix == topic_prefix) {
        expected_received = max_messages * num_senders * num_receivers
      }

      val senders = ListBuffer[Tuple4[Sender, ListBuffer[Int], CountDownLatch, CountDownLatch]]()
      val receivers = ListBuffer[Tuple3[Receiver, ListBuffer[String], CountDownLatch]]()

      (1 to num_receivers).toList.foreach((x) => {
        val receiver_close_latch = new CountDownLatch(1)
        val recv = new ListBuffer[String]
        info("Creating receiver %s", x)
        val receiver = create_receiver(x, prefix, recv, receiver_close_latch)
        receivers.append((receiver, recv, receiver_close_latch))
      })

      (1 to num_senders).toList.foreach((x) => {
        val sent_latch = new CountDownLatch(max_messages)
        val sender_close_latch = new CountDownLatch(1)
        val sent = new ListBuffer[Int]
        info("Creating sender %s", x)
        val sender = create_sender(x, prefix, sent, sent_latch, sender_close_latch, max_messages, settled, durable)
        senders.append((sender, sent, sent_latch, sender_close_latch))
      })

      var sent = 0
      var received = 0

      try {
        senders.foreach {
          case (sender, sent_list, sent_latch, sender_close_latch) =>
            sent_latch.await(wait_time, TimeUnit.SECONDS) should be (true)
            sender_close_latch.await(5, TimeUnit.SECONDS) should be (true)
            sent += sent_list.size
        }
        receivers.foreach {
          case (receiver, recv, receiver_close_latch) =>
            receiver_close_latch.await(5, TimeUnit.SECONDS) should be (true)
            received += recv.size
        }
        sent should be (expected_sent)
        received should be (expected_received)
        println(_testName.get + " finished successfully")
        info("%s finished successfully", _testName.get)
      } catch {
        case t:Throwable =>
          warn("%s did not finish successfully with %s", _testName.get, t.getLocalizedMessage)
          warn("sent=%s received=%s expected_sent=%s expected_received=%s", sent, received, expected_sent, expected_received)
          warn("Failed Senders : ")
          var i = 1
          senders.foreach {
            case (sender, sent, sent_latch, sender_close_latch) =>
              if (sent.size != expected_sent / num_senders) {
                warn("%s : to send=%s sent=%s", sender.getName, sent_latch.getCount, sent)
              }
              if (sender_close_latch.getCount > 0) {
                warn("%s did not disconnect", sender)
              }
              i = i + 1
          }
          i = 1
          warn ("Receivers : ")
          receivers.foreach {
            case (receiver, recv, receiver_close_latch) =>
            val messages = new HashMap[Int, ListBuffer[Int]]()
            var total = 0
            recv.foreach((x) => {
              val v = x.split(":")
              val sender = Integer.parseInt(v(0)).intValue
              val msg_num = Integer.parseInt(v(1)).intValue
              total = total + 1
              messages.get(sender) match {
                case Some(list) =>
                  list.append(msg_num)
                case None =>
                  val list = ListBuffer[Int]()
                  list.append(msg_num)
                  messages.put(sender, list)
              }
            })
            val builder = new StringBuilder
            messages.foreach {
              case (sender, list) =>
                builder.append(String.format("sender%s : %s\n", sender.asInstanceOf[AnyRef], list))
            }
            warn("%s : received : \n\n%stotal : %s", receiver.getName, builder.toString, total)
            if (receiver_close_latch.getCount > 0) {
              warn("%s did not disconnect", receiver)
            }
            i = i + 1
          }
          throw t
      }
    }
  }

}
