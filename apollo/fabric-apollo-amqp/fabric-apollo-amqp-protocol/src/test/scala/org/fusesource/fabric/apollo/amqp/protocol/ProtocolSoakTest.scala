/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.apache.activemq.apollo.util.{FunSuiteSupport, Logging}
import org.fusesource.fabric.apollo.amqp.api._
import org.fusesource.hawtdispatch._
import scala.math._
import collection.mutable.{ListBuffer, HashMap}
import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean}
import java.util.concurrent.{TimeUnit, Executors, CountDownLatch}
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.hawtbuf.Buffer
import Outcome._

/**
 *
 */

class ProtocolSoakTest extends FunSuiteSupport with ShouldMatchers with Logging {
/*
  for (
    num_sessions <- List(1);
    num_senders <- List(1, 5, 10);
    max_messages <- List(100000);
    settled <- List(true, false)
  ) {
    val name = "sessions=" + num_sessions + " senders=" + num_senders + " num_messages=" + max_messages + " settled=" + settled

    test (name) {
      info("Starting test \"%s\"", _testName.get)
      val latch = new CountDownLatch(1)
      var recv = 0;
      var sent = 0;

      val max_per_sender = max_messages / (num_sessions * num_senders)

      info("Creating server")
      val server = new TestReceiver
      server._offer = (receiver:Receiver, message:Message) => {
        //info("received message %s", message)
        if (!message.getSettled()) {
          receiver.settle(message, ACCEPTED)
        }
        val available = receiver.getAvailableLinkCredit
        if (available != null && available.longValue < 5) {
          receiver.addLinkCredit(20 - available.longValue)
        }
        recv += 1
        true
      }
      server.bind("tcp://localhost:0")

      info ("Connecting senders")
      val client = AmqpConnectionFactory.create
      client.setOnClose(^{
        latch.countDown
      })
      client.connect(server.getConnectionUri, ^{
        val session_count = new AtomicLong(0)

        (1 to num_sessions).toList.foreach((se) => {

          val session = client.createSession
          session.begin(^{

            session_count.incrementAndGet
            val sender_count = new AtomicLong(0)

            (1 to num_senders).toList.foreach((s) => {
              val sender = session.createSender
              sender.setOnDetach(^{
                if (sender_count.decrementAndGet <= 0) {
                  session.end(^{
                    if (session_count.decrementAndGet <= 0) {
                      client.close
                    }
                  })
                }
              })

              sender.attach(^{

                sender_count.incrementAndGet

                def put(count:Int):Unit = {
                  val msg = sender.getSession.createMessage
                  msg.setSettled(settled)
                  msg.addBodyPart(new Buffer((se + ":" + s + " message #" + count).getBytes))
                  msg.onAck(^{
                    msg.getOutcome() match {
                      case ACCEPTED =>
                        sent += 1
                        if (count >= max_per_sender) {
                          sender.detach
                        }
                        put(count + 1)
                      case REJECTED =>
                        msg.setSettled(settled)
                        sender.put(msg)
                    }
                  })
                  sender.put(msg)
                }
                put(1)
              })
            })
          })
        })
      })


      try {
        var last_recv = 0
        println("Test - " + name)
        val sample_rate = 1
        while (recv < max_messages) {
          latch.await(sample_rate, TimeUnit.SECONDS)
          val sample = recv - last_recv
          last_recv = last_recv + sample
          println("Sent : " + sent + " Received : " + recv + " Received / s : " + (sample / sample_rate))
        }
        sent should be (max_messages)
        recv should be (max_messages)
        info("Test \"%s\" completed successfully", _testName.get)
      } catch {
        case t:Throwable =>
          warn("Test \"%s\" failed", _testName.get)
          warn("sent=%s, received=%s, expected=%s", sent, recv, max_per_sender)
          throw t
      }

    }
  }
*/
}
