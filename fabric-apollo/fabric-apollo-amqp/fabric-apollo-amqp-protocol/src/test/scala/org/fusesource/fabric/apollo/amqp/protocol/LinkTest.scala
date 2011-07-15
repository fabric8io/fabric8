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
import org.fusesource.fabric.apollo.amqp.codec.AmqpCommand
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.scalatest.BeforeAndAfterEach
import AmqpConversions._
class LinkTest extends FunSuiteSupport with ShouldMatchers with BeforeAndAfterEach with Logging {
  /*
  def get_sender_receiver(address:String) : (OutgoingLink, IncomingLink) = {
    val sender_session = new TestSession
    val receiver_session = new TestSession

    val sender = new OutgoingLink(sender_session)
    sender.setAddress(address)
    val receiver = new IncomingLink(receiver_session)
    receiver.setAddress(address)

    sender_session.peer = receiver
    receiver_session.peer = sender
    (sender, receiver)
  }

  test("Create sender/receiver link, attach and detach") {

    val (sender, receiver) = get_sender_receiver("test")

    receiver.setListener(new MessageListener {
      def needLinkCredit(available: Long) = 0L
      def refiller(refiller: Runnable) = {}
      def offer(receiver: Receiver, message: Message) = false
      def full = false
    })

    var sender_established_after_attach = false
    var receiver_established_after_attach = false
    var sender_not_established_after_detach = false
    var receiver_not_established_after_detach = false

    val await = new CountDownLatch(1)

    sender.attach(^{
      debug("Sender attached")
      sender_established_after_attach = sender.established
      receiver_established_after_attach = receiver.established
      sender.setOnDetach(^{
        debug("Sender detaching")
        sender_not_established_after_detach = !sender.established
        receiver_not_established_after_detach = !receiver.established
        await.countDown
      })
      sender.detach
    })

    await.await(1, TimeUnit.SECONDS) should be (true)
    sender_established_after_attach should be (true)
    receiver_established_after_attach should be (true)
    sender_not_established_after_detach should be (true)
    receiver_not_established_after_detach should be (true)

  }

  for (settled <- List(true); amount <- List(1, 5, 50, 100)) {
    val name = "Create sender/receiver, attach, and send " + amount + " settled=" + settled + " messages, check and detach"
    test(name) {
      val (sender, receiver) = get_sender_receiver("test")

      var sent = 0
      var recv = 0

      val latch = new CountDownLatch(1)
      val expected = new CountDownLatch(amount)

      receiver.setListener(new MessageListener {
        def needLinkCredit(available: Long) = 1L
        def refiller(refiller: Runnable) = {}
        def offer(receiver: Receiver, message: Message) = {
          debug("%s got message %s", receiver.getName, message)
          recv = recv + 1
          expected.countDown
          true
        }
        def full = false
      })

      sender.setName("TestLink")
      sender.setOnDetach(^{
        debug("Sender detached")
        latch.countDown
      })

      sender.attach(^{
        debug("Attached sender")
        def put(sender:Sender, settled:Boolean, i:Int, max:Int):Unit = {
          val msg = sender.getSession.createMessage
          msg.addBodyPart(("message #" + i).getBytes)
          msg.setSettled(settled)
          if (i < max) {
            msg.onSend( ^{
              sent = sent + 1
              debug("Sent message %s, total sent %s", i, sent)
            })
            sender.put(msg)
            put(sender, settled, i + 1, max)
          } else {
            msg.onSend(^{
              sent = sent + 1
              debug("Sent message %s and detaching, total sent %s", i, sent)
              sender.detach
            })
            sender.put(msg)
          }
        }
        put(sender, settled, 1, amount)
      })

      latch.await(30, TimeUnit.SECONDS) should be (true)
      expected.await(30, TimeUnit.SECONDS) should be (true)
      //println("sent=" + sent + " recv=" + recv)
      sent should be (recv)
    }
  }
}

class TestSession extends LinkSession with Session with MessageFactory with Logging {
  import AmqpConversions._

  var peer:AmqpLink = null
  var me:AmqpLink = null

  var handle = 0
  val id = new AtomicLong(1)

  var attach_called: AmqpLink => Unit = (link) => {}
  var detach_called: AmqpLink => Unit = (link) => {}
  var send_called: AmqpCommand => Unit = (command) => {}
  var unsettled = HashMap[Long, AmqpProtoMessage]()

  val queue = Dispatch.createQueue

  def established = peer != null
  def release_transfer_id(id:Long) = {}

  def end(reason:String) = {}
  def end(t:Throwable) = {}
  def end(error:Option[AmqpError]) = {}

  def attach(link:AmqpLink) = {
    link.handle = Option(handle.asInstanceOf[Short])
    me = link
    attach_called(link)
    handle += 1
  }

  def detach(link:AmqpLink) = {
    detach_called(link)
  }

  def settle_incoming(message:AmqpProtoMessage, outcome:AmqpType[_, AmqpBuffer[_]]) = {
    // TODO - give disposition to peer
  }

  def send(link:AmqpLink, message:AmqpProtoMessage) = {
    val transfer = message.transfer(id.getAndIncrement)
    transfer.setHandle(link.handle.get)
    message.onSend.foreach((x) => dispatch_queue << x)
    if (!Option(transfer.getSettled).getOrElse(false).asInstanceOf[Boolean]) {
      unsettled += transfer.getTransferId.getValue.longValue -> message
    }
    send(link, transfer)
  }

  def send(link:AmqpLink, command:AmqpCommand) = send(command)

  def send(command:AmqpCommand) = {
    debug("%s:%s sending command %s", me.getClass.getSimpleName, me.getName, command)
    command match {
      case attach:AmqpAttach =>
        peer.attach(attach)
      case detach:AmqpDetach =>
        peer.detach(detach)
      case transfer:AmqpTransfer =>
        peer match {
          case peer:IncomingLink =>
            val msg = AmqpProtoMessage.create(transfer)
            peer.transfer(msg)
        }
      case flow:AmqpFlow =>
        peer.peer_flowstate(flow)
      case disposition:AmqpDisposition =>
        peer match {
          case peer:OutgoingLink => {
              val i = (disposition.getFirst.getValue.longValue to disposition.getLast.getValue.longValue).toList
              i.foreach((x) => {
                unsettled.get(x) match {
                  case Some(message) =>
                    unsettled -= x
                    message.outcome = disposition.getState
                    message.onAck.foreach((r) => dispatch_queue << r)
                  case None =>
                }
              })
            }
          }
      case _ =>
        info("%s, received unknown command %s", me, command)
        assert(false)
    }
  }

  def getConnection = null.asInstanceOf[Connection]

  def dispatch_queue = queue

  def end(onEnd: Runnable) {}

  def setLinkListener(listener: LinkListener) {}

  def createReceiver() = null

  def createSender() = null

  def begin(onBegin: Runnable) {}

  def sufficientSessionCredit() = false

  def getIncomingWindow = 0L

  def getOutgoingWindow = 0L

  def setIncomingWindow(window: Long) {}

  def setOutgoingWindow(window: Long) {}
  */
}
