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

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpError
/**
 *
 */

class MessageAcknowledgementTest extends FunSuiteSupport with ShouldMatchers with Logging {
  /*

  def stop_client(sender:Sender, session:Session, client:Connection, latch:CountDownLatch) = {
    info("detaching...")
    sender.setOnDetach(^{
      info("Detached sender")
      session.end(^{
        info("Ended session")
        client.close
      })
    })
    sender.detach
  }

  test("Send unsettled messages, wait for ack before sending each message") {
    val max_messages = 10
    val recv_latch = new CountDownLatch(max_messages)
    val latch = new CountDownLatch(1)
    val server = new TestReceiver
    server._offer = (receiver:Receiver, message:Message) => {
      info("Received message %s", message)
      if (!message.getSettled) {
        receiver.settle(message, Outcome.ACCEPTED)
      }
      recv_latch.countDown
      true
    }

    server.bind("tcp://localhost:0")

    val client = AmqpConnectionFactory.create()
    client.setOnClose(^{
      info("Closed connection")
      latch.countDown
    })
    client.connect(server.getConnectionUri, ^{
      info("Connected sender")
      val session = client.createSession
      session.begin(^{
        info("Started session")
        val sender = session.createSender
        sender.setAddress("queue:foo")
        sender.setName("Test Link")
        sender.attach(^{
          def put(i:Int, max:Int):Unit = {
            val message = sender.getSession.createMessage
            message.addBodyPart(new Buffer(("Message #" + i).getBytes))
            message.setSettled(false)
            message.onAck(^{
              info("Message \"%s\" had outcome %s", message, message.getOutcome)
              if (message.getOutcome == Outcome.ACCEPTED) {
                if (i < max) {
                  info("Sending another message")
                  put(i + 1, max)
                } else {
                  stop_client(sender, session, client, latch)
                }
              } else {
                stop_client(sender, session, client, latch)
              }

            })
            sender.put(message)
          }
          put(0, max_messages)
        })
      })
    })

    latch.await(30, TimeUnit.SECONDS) should be (true)
    recv_latch.await(30, TimeUnit.SECONDS) should be (true)
  }

  test("Send unsettled message, throw exception in receiver which rejects the message, resend") {
    val recv_latch = new CountDownLatch(2)
    val latch = new CountDownLatch(1)
    val server = new TestReceiver
    var throw_exception = true
    server._offer = (receiver:Receiver, message:Message) => {
      info("Received message %s", message)
      recv_latch.countDown
      if (throw_exception) {
        info("Throwing exception on purpose")
        throw_exception = false
        throw new RuntimeException("Nope!")
      }

      if (!message.getSettled) {
        receiver.settle(message, Outcome.ACCEPTED)
      }
      true
    }

    server.bind("tcp://localhost:0")

    var failures = 0L
    var error:AmqpError = null
    var settled = false

    val client = AmqpConnectionFactory.create
    client.setOnClose(^{
      info("Closed connection")
      latch.countDown
    })
    client.connect(server.getConnectionUri, ^{
      val session = client.createSession
      session.begin(^{
        val sender = session.createSender
        sender.setAddress("queue:test")
        sender.setName("Some Test Link")
        sender.attach(^{
          val message = sender.getSession.createMessage
          message.setSettled(false)
          message.addBodyPart(new Buffer("Test".getBytes))
          message.onAck(^{
            message.getOutcome match {
              case Outcome.ACCEPTED =>
                debug("Message is accepted : %s", message)
                settled = message.getSettled
                stop_client(sender, session, client, latch)
              case Outcome.REJECTED =>
                message.setSettled(false)
                failures = message.getHeader.getDeliveryFailures.longValue
                error = message.getError
                debug("Resending message %s", message)
                sender.put(message)
            }
          })
          sender.put(message)
        })
      })
    })

    latch.await(30, TimeUnit.SECONDS) should be (true)
    failures should be (1)
    error should not (be (null))
    settled should be (true)
    recv_latch.await(30, TimeUnit.SECONDS) should be (true)
  }

  test("Send a bunch of messages with batchable=true, randomly reject and make sure they're eventually all sent successfully") {
    val max_messages = 10
    val recv_latch = new CountDownLatch(max_messages)
    val send_latch = new CountDownLatch(max_messages)
    val latch = new CountDownLatch(1)
    val server = new TestReceiver
    server._offer = (receiver:Receiver, message:Message) => {
      info("Received message %s", message)
      if (Random.nextBoolean) {
        info("Throwing exception on purpose")
        throw new RuntimeException("Nope!")
      }
      if (!message.getSettled) {
        receiver.settle(message, Outcome.ACCEPTED)
      }
      recv_latch.countDown
      true
    }

    server.bind("tcp://localhost:0")

    val client = AmqpConnectionFactory.create()
    client.setOnClose(^{
      info("Closed connection")
      latch.countDown
    })
    client.connect(server.getConnectionUri, ^{
      debug("Sender client connected")
      val session = client.createSession
      session.begin(^{
        debug("Sender created session")
        val sender = session.createSender
        sender.attach(^{
          debug("Sender attached")
          def put(x:Int):Unit = {
            val msg = sender.getSession.createMessage
            msg.addBodyPart(("message #" + x).getBytes)
            msg.setSettled(false)
            msg.setBatchable(true)
            msg.onAck(^{
              msg.getOutcome match {
                case Outcome.REJECTED =>
                  debug("Resending message %s : failure count %s", msg, msg.getHeader.getDeliveryFailures)
                  msg.setSettled(false)
                  sender.put(msg)
                case _ =>
                    if (msg.getSettled) {
                      debug("Message settled : %s", msg)
                      send_latch.countDown
                      if (send_latch.getCount == 0) {
                        stop_client(sender, session, client, latch)
                      }
                    }
              }
            })
            if (x < max_messages) {
              msg.onSend( ^{
                debug("Sent message %s : %s", x, msg)
              })
              sender.put(msg)
              put(x + 1)
            } else {
              sender.put(msg)
            }
          }
          debug("Sending messages")
          put(1)
        })
      })
    })

    send_latch.await(1, TimeUnit.MINUTES) should be (true)
    latch.await(30, TimeUnit.SECONDS) should be (true)
    recv_latch.await(1, TimeUnit.MINUTES) should be (true)

  }
*/
}
