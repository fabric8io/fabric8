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

import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.hawtdispatch._
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.fabric.apollo.amqp.api._
import java.util.concurrent._

/**
 *
 */

class ConnectionTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create server connection with TCP and connect to it") {
    val server = new BaseTestServer
    server.bind("tcp://localhost:0")

    val latch = new CountDownLatch(1)
    val client = AmqpConnectionFactory.create()
    client.setOnClose(^{
      debug("Connection closed")
      latch.countDown
    })
    client.connect(server.getConnectionUri, ^{
      debug("Client connected")
      val session = client.createSession
      debug("Created session %s", session)
      session.begin(^{
        debug("Session started")
        session.end(^{
          debug("Session ended")
          client.close
        })
      })
    })

    latch.await(10, TimeUnit.SECONDS) should be (true)
  }
/*
  test("Create server connection, connect to it, stop heartbeat thread and check that the connection is disconnected") {
    val server = new BaseTestServer
    server.bind("tcp://localhost:0")

    val latch = new CountDownLatch(1)
    val client = AmqpConnectionFactory.create
    client.setOnClose(^{
      debug("Connection closed")
      latch.countDown
    })
    client.connect(server.getConnectionUri, ^{
      debug("Client connected")
      val session = client.createSession
      session.begin(^{
        debug("Session started")
        client.getDispatchQueue.after(5, TimeUnit.SECONDS) {
          client.asInstanceOf[AmqpConnection].heartbeat_monitor.stop
        }
      })
    })

    latch.await(60, TimeUnit.SECONDS) should be (true)

  }

  test("Create server connection with TCP, connect, create a session and link, then disconnect") {
    val server = new TestReceiver

    val latch = new CountDownLatch(1)
    val detach_latch = new CountDownLatch(1)
    val receiver_detach_latch = new CountDownLatch(1)

    server._onDetach = ^{
      receiver_detach_latch.countDown
    }

    server.bind("tcp://localhost:0")

    val client = AmqpConnectionFactory.create()
    client.setOnClose(^{
      latch.countDown
    })

    client.connect(server.getConnectionUri, ^{
      val session = client.createSession
      session.begin(^{
        val sender = session.createSender
        sender.setOnDetach(^{
          detach_latch.countDown
        })
        sender.attach(^{
          client.getDispatchQueue.after(5, TimeUnit.SECONDS) {
            client.close
          }
        })
      })
    })

    receiver_detach_latch.await(10, TimeUnit.SECONDS) should be (true)
    detach_latch.await(10, TimeUnit.SECONDS) should be (true)
    latch.await(10, TimeUnit.SECONDS) should be (true)

  }


  test("Create server, connect sender, send some messages") {
    val max_messages = 1000
    val latch = new CountDownLatch(1)
    val recv_latch = new CountDownLatch(max_messages)

    info("Creating server")
    val server = new TestReceiver
    server._offer = (receiver:Receiver, message:Message) => {
      info("received message %s", message)
      recv_latch.countDown
      true
    }
    server.bind("tcp://localhost:0")

    info ("Connecting sender")

    val client = AmqpConnectionFactory.create()
    client.setOnClose(^{
      debug("Sender closed connection")
      latch.countDown
    })
    client.connect(server.getConnectionUri, ^{
      debug("Sender client connected")
      val session = client.createSession
      session.begin(^{
        debug("Sender created session")
        val sender = session.createSender
        sender.setOnDetach(^{
          debug("Sender detached")
          session.end(^{
            debug("Sender closed session")
            client.close
          })
        })
        sender.attach(^{
          debug("Sender attached")
          def put(x:Int):Unit = {
            val msg = sender.getSession.createMessage
            msg.addBodyPart(new Buffer(("message #" + x).getBytes))
            if (x < max_messages) {
              msg.onSend( ^{
                debug("Sent message %s : %s", x, msg)
              })
              sender.put(msg)
              put(x + 1)
            } else {
              msg.onSend(^{
                sender.detach()
              })
              sender.put(msg)
            }
          }
          debug("Sending messages")
          put(1)
        })
      })
    })

    latch.await(10, TimeUnit.SECONDS) should be (true)
    recv_latch.await(10, TimeUnit.SECONDS) should be (true)

  }

    test("Create server, connect receiver, send some messages") {
      val max_messages = 1000
      val latch = new CountDownLatch(max_messages * 2)
      val finished = new CountDownLatch(1)

      val executor = Executors.newSingleThreadExecutor

      info("Creating server")
      val server = new DefaultLinkListener
      server.receiver_attaching = (session:Session, sender:Sender) => {
        executor.execute(^{
          debug("Waiting to send messages")
          Thread.sleep(3000)
          val i = (1 to max_messages).toList
          i.foreach((x) => {
            val msg = sender.getSession.createMessage
            msg.addBodyPart(("message #" + x).getBytes)
            msg.onSend( ^{
              debug("Sent message %s", i)
              latch.countDown
            })
            sender.put(msg)
          })
        })
      }

      server.bind("tcp://localhost:0")

      var received = 0

      debug("Connecting receiver")
      val client = AmqpConnectionFactory.create
      client.setOnClose(^{
        debug("Connection closed")
        finished.countDown
      })
      client.connect(server.getConnectionUri, ^{
        debug("Receiver client connected")
        val session = client.createSession
        session.begin(^{
          debug("Receiver created session")
          val receiver = session.createReceiver
          receiver.setAddress("foo")
          receiver.setOnDetach(^{
            debug("Receiver detached")
            session.end(^{
              debug("Session closed")
              client.close
            })
          })
          receiver.setListener(new MessageListener {
            def needLinkCredit(available: Long) = 1L
            def refiller(refiller: Runnable) = {}
            def offer(receiver: Receiver, message: Message) = {
              debug("Received message %s", message)
              latch.countDown
              received = received + 1
              if (received >= max_messages) {
                receiver.detach
              }
              true
            }
            def full = false
          })

          receiver.attach(^{
            debug("Receiver attached")
          })
        })

      })


      latch.await(30, TimeUnit.SECONDS) should be (true)
      finished.await(30, TimeUnit.SECONDS) should be (true)

    }
    */
}
