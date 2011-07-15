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
/**
 *
 */
class SessionTest extends FunSuiteSupport with ShouldMatchers with Logging {

  /*

  def create_sessions = {
    val connection1 = new DummyConnection
    val connection2 = new DummyConnection

    connection1.peer = connection2

    val session1 = connection1.session
    val session2 = connection2.session

    (session1, session2)
  }

  for ( num_links <- List(1, 5, 10, 50); num_messages <- List(1, 5, 50); settled <- List(true, false) ) {

    val name = "Connect 2 sessions and send " + num_messages + " settled=" + settled + " message(s) across " + num_links + " links"

    test(name) {
      val expected = num_messages * num_links
      val count = new AtomicLong(0)
      val latch = new CountDownLatch(expected)

      val senders = ListBuffer[Sender]()
      val attach_latch = new CountDownLatch(num_links)

      val (session1, session2) = create_sessions
      session2.setLinkListener(new TestLinkListener(new TestMessageListener(count, latch)))
      session1.begin(^{
        val i = (1 to num_links).toList
        i.foreach((i) => {
          val sender = session1.createSender
          sender.setAddress("test" + i)
          sender.attach(^{
            debug("Sender attached")
            attach_latch.countDown
          })
          senders += sender
        })
      })

      attach_latch.await(30, TimeUnit.SECONDS) should be (true)

      val futures = ListBuffer[Future[_]]()
      val executor = Executors.newFixedThreadPool(num_links)

      senders.foreach((sender) => {
        futures.append(executor.submit(^{
          val sender_latch = new CountDownLatch(1)
          sender.setOnDetach(^{
            debug("Sender detached")
            sender_latch.countDown
          })
          def put(x:Int):Unit = {
            val msg = sender.getSession.createMessage
            msg.addBodyPart(new Buffer(("message #" + x).getBytes))
            if (x < num_messages) {
              msg.onSend( ^{
                debug("Sent message %s", msg)
              })
              sender.put(msg)
              put(x + 1)
            } else {
              msg.onSend( ^{
                debug("Sent message %s", msg)
                sender.detach
              })
              sender.put(msg)
            }
          }
          put(1)
          sender_latch.await(60, TimeUnit.SECONDS)
        }))
      })

      futures.foreach((x) => x.get(10, TimeUnit.SECONDS))
      latch.await(20, TimeUnit.SECONDS) should be (true)

      val total = count.get

      println("Got " + total + " messages, expected " + expected)
      total should be (expected)
    }
  }

  for (num_links <- List(1, 5, 10, 50)) {
    val name = "Attach " + num_links + " links across 2 sessions, check all links detached"
    test(name) {
      val (session1, session2) = create_sessions
      val detach_latch = new CountDownLatch(num_links)

      val receivers = ListBuffer[Receiver]()
      session2.setLinkListener(new LinkListener {
        def receiverDetaching(session: Session, sender: Sender) = {}

        def senderDetaching(session: Session, receiver: Receiver) = {
         debug("Detaching %s", receiver.getName)
         detach_latch.countDown
        }

        def receiverAttaching(session: Session, sender: Sender) = {}

        def senderAttaching(session: Session, receiver: Receiver) = {
          receiver.setListener(new MessageListener {
            def needLinkCredit(available: Long) = 0L
            def refiller(refiller: Runnable) = {}
            def offer(receiver: Receiver, message: Message) = false
            def full = false
          })
        }
      })

      val attach_latch = new CountDownLatch(num_links)

      session1.begin(^{
        val i = (1 to num_links).toList
        i.foreach((i) => {
          val sender = session1.createSender
          sender.setAddress("test" + i)
          sender.setName("test" + i)
          sender.attach(^{
            debug("Attached %s", sender.getName)
            attach_latch.countDown
            if (i == num_links) {
              sender.detach(new IllegalArgumentException("Test"))
            }
          })
        })
      })

      attach_latch.await(5, TimeUnit.SECONDS) should be (true)

      session1.end(AmqpAmqpError.INTERNAL_ERROR.getValue.getValue, "Test")
      detach_latch.await(5, TimeUnit.SECONDS) should be (true)

      receivers.foreach((x) => x.established should be (false))

    }
  }

  test("Create session and change window sizes") {
    val (session1, session2) = create_sessions.asInstanceOf[(AmqpSession, AmqpSession)]

    def display = {
      info("\nSession 1 : %s\n\nSession 2 : %s", session1, session2)
    }

    session1.begin(NOOP)

    session1.setOutgoingWindow(20L)

    display

    session1.outgoing_window should be (20L)
    session2.remote_outgoing_window should be (20L)

    session2.setIncomingWindow(20L)

    display

    session2.incoming_window should be (20L)
    session1.remote_incoming_window should be (20L)

  }

  /*
    test("Attach, detach and re-attach") {

      val expected = 2
      val count = new AtomicLong(0)
      val latch = new CountDownLatch(expected)

      val (session1, session2) = create_sessions
      session2.setLinkListener(new TestLinkListener(new TestMessageListener(count, latch)))
      session1.begin(false)

      val sender = session1.sender
      sender.setAddress("queue:test")
      sender.attach

      sender.established should be (true)

      val msg1 = sender.getSession.createMessage
      msg1.setBody("Message 1")
      val send_latch = new CountDownLatch(1)
      sender.setOnSendCallback(new OnSendCallback {
        def onDelivered(message: Message) = {
          send_latch.countDown
        }
      })
      sender.put(msg1)
      send_latch.await
      sender.detach

      Thread.sleep(100)
      count.get should be (1)
      sender.established should be (false)

      sender.attach
      sender.established should be (true)
      val msg2 = sender.getSession.createMessage
      msg2.setBody("Message 2")
      val send_latch_2 = new CountDownLatch(1)
      sender.setOnSendCallback(new OnSendCallback {
        def onDelivered(message: Message) = {
          send_latch_2.countDown
        }
      })
      sender.put(msg2)
      send_latch_2.await

      sender.detach

      latch.await(10, TimeUnit.SECONDS) should be (true)
      count.get should be (2)
      sender.established should be (false)
    }
  */
}
class TestMessageListener(count:AtomicLong, latch:CountDownLatch) extends MessageListener with Logging {
  def offer(r:Receiver, m:Message) = {
    debug("receiver : %s => message : %s", r.getName, m)
    count.incrementAndGet
    latch.countDown
    true
  }

  def needLinkCredit(a:Long) = max(a, 1L)
  def refiller(r:Runnable) = {}
  def full = false
}

class TestLinkListener(msgListener:MessageListener) extends LinkListener {

  def receiverDetaching(session: Session, sender: Sender) = {}

  def senderDetaching(session: Session, receiver: Receiver) = {}

  def receiverAttaching(session: Session, sender: Sender) = {}

  def senderAttaching(session: Session, receiver: Receiver) = {
    receiver.setListener(msgListener)
  }
}

class DummyConnection extends SessionConnection with ConnectionHandler with Logging {

  def sasl_outcome(saslOutcome: AmqpSaslOutcome) {}

  def sasl_init(saslInit: AmqpSaslInit) {}

  def sasl_mechanisms(saslMechanisms: AmqpSaslMechanisms) {}

  def sasl_response(saslResponse: AmqpSaslResponse) {}

  def sasl_challenge(saslChallenge: AmqpSaslChallenge) {}

  val queue = Dispatch.createQueue

  var _peer:DummyConnection = null

  def peer = _peer
  def peer_=(p:DummyConnection) = {
    _peer = p
    p._peer = this
  }

  val session = new AmqpSession(this, 0)
  val handler = new SessionHandler(this, session)

  def getDispatchQueue = queue

  def getOperationTimeout = 10L
  def setOperationTimeout(timeout:Long) = {}

  def getContainerId = "test"
  def getPeerContainerId = "test"

  def release(channel:Int) = {}

  def send(channel:Int, cmd:AmqpCommand) = {
    info("Channel %s sending %s", channel, cmd)
    val frame = new AmqpFrame(cmd)
    frame.setChannel(channel)
    frame.handle(_peer.handler.handler)
    true
  }

  def header(header:AmqpProtocolHeader) = {}
  def open(open:AmqpOpen) = {}
  def close = {}
  def close(s:String) = {}
  def close(t:Throwable) = {}
  */
}
