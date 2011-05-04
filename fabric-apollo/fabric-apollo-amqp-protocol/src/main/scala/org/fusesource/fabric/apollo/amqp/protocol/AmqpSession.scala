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

import org.fusesource.fabric.apollo.amqp.codec.AmqpCommand
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.hawtdispatch._
import AmqpRole.SENDER
import AmqpRole.RECEIVER
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.apache.activemq.apollo.util.Logging
import java.util.concurrent.atomic.{AtomicLong, AtomicInteger, AtomicBoolean}
import AmqpConversions._
import AmqpProtocolSupport._
import org.fusesource.fabric.apollo.amqp.api._
import collection.immutable.HashMap
import collection.mutable.ListBuffer
import java.util.concurrent.{TimeUnit, Executors, ExecutorService, LinkedBlockingQueue}
import java.util.Date
import org.fusesource.hawtbuf.Buffer

/**
 *
 */
class AmqpSession (connection:SessionConnection, val channel:Int) extends Session with LinkSession with Logging {

  val current_transfer_id = new AtomicLong(1)
  val current_handle: AtomicInteger = new AtomicInteger(0)
  val begin_sent: AtomicBoolean = new AtomicBoolean(false)
  val end_sent: AtomicBoolean = new AtomicBoolean(false)

  val store:LinkStore = new LinkStore
  var unsettled_outgoing = HashMap[AmqpDeliveryTag, AmqpProtoMessage]()
  var unsettled_incoming = HashMap[AmqpDeliveryTag, AmqpProtoMessage]()
  var id_to_tag = HashMap[Long, AmqpDeliveryTag]()
  var tag_to_id = HashMap[AmqpDeliveryTag, Long]()

  var remote_channel: Int = 0
  var listener:Option[LinkListener] = None

  var on_begin:Option[Runnable] = None
  var on_end:Option[Runnable] = None

  var incoming_window_max = 1L
  var outgoing_window_max = 1L

  var incoming_window = incoming_window_max
  var outgoing_window = outgoing_window_max

  var remote_incoming_window = 0L
  var remote_outgoing_window = 0L

  var next_incoming_transfer_id:Option[Long] = None

  val outstandingAcks = new LinkedBlockingQueue[Tuple4[Long, AmqpType[_,AmqpBuffer[_]], Boolean, Boolean]]
  val service:ExecutorService = Executors.newSingleThreadExecutor
  val startAckThread:AtomicBoolean = new AtomicBoolean(true)
  // TODO - add accessors for this
  val ackTime = 500


  override def toString = {
    "AmqpSession{local_channel=%s remote_channel=%s incoming_window_max=%s incoming_window=%s remote_outgoing_window=%s outgoing_window_max=%s outgoing_window=%s remote_outgoing_window=%s}" format (channel, remote_channel, incoming_window_max, incoming_window, remote_outgoing_window, outgoing_window_max, outgoing_window, remote_outgoing_window)
  }

  def session_credit = 1L

  def unsettled_lwm = 0

  def established = begin_sent.get == true && end_sent.get == false

  def create_link(role:AmqpRole):AmqpLink = {
    role match {
      case SENDER =>
        new OutgoingLink(this)
      case RECEIVER =>
        new IncomingLink(this)
    }
  }

  def create_link(attach:AmqpAttach):AmqpLink = {
    val rc = attach.getRole match {
      case SENDER =>
        create_link(RECEIVER)
      case RECEIVER =>
        create_link(SENDER)
    }
    rc.source = attach.getSource.asInstanceOf[AmqpSource]
    rc.target = attach.getTarget.asInstanceOf[AmqpTarget]
    rc.name = attach.getName
    rc.remotely_created = true
    rc
  }

  def createSender: Sender = create_link(SENDER).asInstanceOf[Sender]
  def createReceiver:Receiver = create_link(RECEIVER).asInstanceOf[Receiver]

  def begin(onBegin:Runnable) = {
    this.on_begin = Option(onBegin)
    if (!begin_sent.getAndSet(true)) {
      begin(false)
    }
  }

  def begin(setRemoteChannel: Boolean): Unit = {
    val begin: AmqpBegin = createAmqpBegin
    if ( setRemoteChannel ) {
      begin.setRemoteChannel(remote_channel)
    }
    begin.setOutgoingWindow(outgoing_window)
    begin.setIncomingWindow(incoming_window)
    //trace("Sending begin from local channel %s for remote channel %s", channel, remote_channel)
    connection.send(channel, begin)
  }

  def begin(peer_begin: AmqpBegin): Unit = {
    Option(peer_begin.getRemoteChannel) match {
      case Some(channel) =>
        remote_channel = channel.intValue
        on_begin.foreach((x) => dispatch_queue << x)
      case None =>
    }
    remote_incoming_window = Option(peer_begin.getIncomingWindow) match {
      case Some(window) =>
        window.longValue
      case None =>
        throw new RuntimeException("Remote incoming window not specified")
    }
    remote_outgoing_window = Option(peer_begin.getOutgoingWindow) match {
      case Some(window) =>
        window.longValue
      case None =>
        throw new RuntimeException("Remote outgoing window not specified")
    }
    if (!begin_sent.getAndSet(true)) {
      begin(true)
    }
  }

  def end(onEnd:Runnable) = {
    this.on_end = Option(onEnd)
    end
  }
  def end(frame: AmqpEnd):Unit = {
    end(Option(frame.getError))
    on_end.foreach((x) => dispatch_queue << x)
  }
  def end:Unit = end(None)

  def end(condition:String, t:Throwable):Unit = {
    val error = createAmqpError
    error.setCondition(condition)
    error.setDescription(t.getClass + " : " + t.getMessage + "\n" + t.getStackTraceString)
    end(Option(error))
  }

  def end(t:Throwable):Unit = end("link error", t)

  def end(condition:String, description:String):Unit = {
    val error = createAmqpError
    error.setCondition(condition)
    error.setDescription(description)
    end(Option(error))
  }

  def end(reason:String):Unit = end("link error", reason)

  def end(error:Option[AmqpError]):Unit = {
    if (!end_sent.getAndSet(true)) {
      val end = createAmqpEnd
      error match {
        case Some(e) =>
          warn("Ending session due to error %s", e)
          end.setError(e)
        case None =>
          info("Ending session")
      }

      unsettled_outgoing.foreach {
        case (tag, message) =>
          error match {
            case Some(e) =>
              message.setSettled(true)
              message.outcome = Outcome.REJECTED
              message.error = e
              message.onAck.foreach((x) => dispatch_queue << x)
            case None =>
              message.setSettled(true)
              message.outcome = Outcome.RELEASED
              message.onAck.foreach((x) => dispatch_queue << x)
          }
      }

      // TODO - actually need to detach only links that expire with the createSession
      store.foreach((l) => {
        error match {
          case Some(e) =>
            l.detach(Option(e))
          case None =>
            l.detach
        }
        l.onDetach.foreach((x) => dispatch_queue << x)
      })

      connection.send(channel, end)

      error match {
        case Some(e) =>
          connection.release(channel)
          on_end.foreach((x) => dispatch_queue << x)
        case None =>
          on_end match {
            case Some(r) =>
              dispatch_queue << ^{
                connection.release(channel)
                r.run
              }
            case None =>
              dispatch_queue << ^{
                connection.release(channel)
              }
          }
      }
    }
  }

  def attach(link:AmqpLink) = {
    link.handle = Option(store.allocate_handle(0, link))
    store.add(link)
  }

  def attach(attach: AmqpAttach): Unit = {
    val link = get_link(attach)
    listener.foreach((l) => {
      attach.getRole match {
        case AmqpRole.SENDER =>
          l.senderAttaching(this, link.asInstanceOf[Receiver])
        case AmqpRole.RECEIVER =>
          l.receiverAttaching(this, link.asInstanceOf[Sender])
      }
    })
    link.attach(attach)
    //trace("Attached remote handle %s to link %s", attach.getHandle.getValue, link)
  }

  def detach(detach: AmqpDetach): Unit = {
    val key: Short = detach.getHandle.getValue.shortValue
    store.remove_by_remote_handle(key) match {
      case Some(link) =>
        //trace("Detaching link : %s", link)
        link.detach(detach)
        listener.foreach((l) => {
          link match {
            case o:OutgoingLink =>
              l.receiverDetaching(this, o)
            case i:IncomingLink =>
              l.senderDetaching(this, i)
          }
        })
      case None =>
        info("Dropping incoming detach frame %s for non existing link key %s", detach, key)
    }
  }

  // TODO - maybe not take the link out of the store until the peer detach comes in
  def detach(link:AmqpLink) = link.handle.foreach((h) => store.remove_by_local_handle(h))

  def get_link(attach: AmqpAttach): AmqpLink = {
    val key = attach.getHandle.getValue.shortValue

    def if_exists(maybe:Option[AmqpLink]): AmqpLink = {
      maybe match {
        case Some(link) =>
          //trace("Attaching existing link %s to remote handle %s", link, key)
          store.add_remote(key, link)
          link
        case None =>
          val link = create_link(attach)
          //trace("Attaching new link %s to remote handle %s", link, key)
          store.add_remote(key, link)
          link
      }
    }

    attach.getRole match {
      case SENDER =>
        store.add_remote(RECEIVER, key, attach.getName, if_exists)
      case RECEIVER =>
        store.add_remote(SENDER, key, attach.getName, if_exists)
    }
  }

  def update_flow_state(flowState:AmqpFlow) = {
    // TODO - session-level flow control
    flowState.setNextOutgoingId(current_transfer_id.get + 1L)
    flowState.setIncomingWindow(incoming_window)
    flowState.setOutgoingWindow(outgoing_window)
    next_incoming_transfer_id.foreach((x) => flowState.setNextIncomingId(x))
  }

  def send(link:AmqpLink, message:AmqpProtoMessage):Unit = {

    message.getHeader.setTransmitTime(new Date)
    Option(message.getProperties.getUserId).getOrElse(message.getProperties.setUserId(new Buffer(System.getProperty("user.name").getBytes)))

    val transfer = message.transfer(current_transfer_id.getAndIncrement)
    transfer.setHandle(link.handle.get)

    if (!Option(transfer.getSettled).getOrElse(false).asInstanceOf[Boolean]) {
      //trace("Adding outgoing transfer ID %s to unsettled map for link handle %s", transfer.getTransferId, transfer.getHandle)
      unsettled_outgoing += message.tag -> message
      id_to_tag += transfer.getTransferId.getValue.longValue -> message.tag
    } else {
      //trace("Directly ack'ing transfer ID %s that has already been settled", transfer.getTransferId);
      message.outcome = Outcome.ACCEPTED
      message.onAck.foreach((x) => dispatch_queue << x)
    }

    message.onSend.foreach((x) => dispatch_queue << x)
    send(link, transfer)
  }

  def send(link:AmqpLink, command:AmqpCommand):Unit = {
    if (!begin_sent.getAndSet(true)) {
      begin(false)
    }
    if ( end_sent.get ) {
      command match {
        case end:AmqpEnd =>
        case detach:AmqpDetach =>
        case _ =>
          throw new RuntimeException("Session is closed")
      }
    }
    command match {
      case flow:AmqpFlow =>
        update_flow_state(flow)
      case _ =>
    }
    send(command)
  }

  def send(command: AmqpCommand): Unit = connection.send(channel, command)

  def transfer(transfer: AmqpTransfer) = {
    Option(transfer.getTransferId) match {
      case Some(id) =>
        next_incoming_transfer_id = Option(id.getValue.longValue + 1L)
      case None =>
        throw new RuntimeException("No transfer ID specified by peer")
    }
    val key = transfer.getHandle.getValue.shortValue
    store.get_by_remote_handle(key) match {
      case Some(link) =>
        //trace("Directing transfer id %s from remote handle %s to local handle %s", transfer.getTransferId, key, link.handle)
        val msg = AmqpProtoMessage.create(transfer)
        if (!msg.settled) {
          unsettled_incoming = unsettled_incoming + (msg.tag -> msg)
          tag_to_id = tag_to_id + (msg.tag -> transfer.getTransferId.getValue.longValue)
        }
        link.transfer(msg)
      case None =>
        end(AmqpAmqpError.NOT_FOUND, new RuntimeException("Link for handle " + key + " not found"))
    }
  }

  def flow(flow: AmqpFlow) = {
    // look at session level info
    Option(flow.getOutgoingWindow) match {
      case None =>
      case Some(window) =>
        remote_outgoing_window = window.longValue
    }
    Option(flow.getIncomingWindow) match {
      case None =>
      case Some(window) =>
        remote_incoming_window = window.longValue
    }

    // now see if this flow needs to be passed on to a link
    Option(flow.getHandle) match {
      case None =>
      case Some(handle) =>
        store.get_by_remote_handle(handle.getValue.shortValue) match {
          case Some(link) =>
            //trace("Directing incoming flow from remote handle %s to local handle %s", key, link.handle)
            link.peer_flowstate(flow)
          case None =>
            info("Link not found for incoming flow : %s", flow)
        }
    }
  }

  def disposition(disposition: AmqpDisposition): Unit = {
    //trace("Received incoming disposition : %s", disposition)
    settle_outgoing(disposition.getFirst,
      disposition.getLast,
      disposition.getState,
      Option[Boolean](disposition.getSettled).getOrElse(false),
      disposition.getRole)
  }

  private def settle_outgoing(first:Long, last:Long, outcome:AmqpType[_, _], settled:Boolean, role: AmqpRole): Unit = {
    for ( i <- (first to last) ) {
      id_to_tag.get(i) match {
        case Some(tag) =>
          id_to_tag = id_to_tag - i
          unsettled_outgoing.get(tag) match {
            case Some(message) =>
              message.settled = settled
              message.outcome = outcome
              message.outcome match {
                case Outcome.ACCEPTED =>
                case Outcome.REJECTED =>
                  val error = outcome.asInstanceOf[AmqpRejected]
                  info("Transfer ID %s rejected with error %s", i, error)
                  message.error = error.getError
                case Outcome.RELEASED =>
                // TODO
                case Outcome.MODIFIED =>
                // TODO
              }
              if (message.settled) {
                unsettled_outgoing = unsettled_outgoing - tag
              }
              message.onAck.foreach((r) => dispatch_queue << r)
            case None =>
              debug("Delivery tag %s has already been released", tag)
          }
        case None =>
          debug("Transfer ID %s has already been released", i)
      }
    }
  }

  def settle_incoming(message:AmqpProtoMessage, outcome:AmqpType[_, AmqpBuffer[_]]): Unit = {
    val id = tag_to_id.get(message.tag)
    if (message.settled) {
      unsettled_incoming = unsettled_incoming - message.tag
      tag_to_id = tag_to_id - message.tag
    }
    id match {
      case Some(id) =>
        val settled = message.settled
        val batchable = message.batchable

        val disposition = createAmqpDisposition
        disposition.setFirst(id)
        disposition.setLast(id)
        disposition.setSettled(settled)
        disposition.setBatchable(batchable)
        disposition.setState(outcome)
        //trace("Sending ack for transfer id %s with outcome=%s and settled=%s, batchable=%s", id, outcome, settled, batchable)
        send(disposition)
      case None =>
    }
  }

  def setLinkListener(listener: LinkListener) = this.listener = Option(listener)
  def setRemoteChannel(channel: Short) = remote_channel = channel
  def getConnection = connection
  def getChannel = channel
  def dispatch_queue = connection.getDispatchQueue

  def getOutgoingWindow = outgoing_window
  def getIncomingWindow = incoming_window

  def setOutgoingWindow(window:Long) = {
    outgoing_window_max = window
    val diff = outgoing_window_max - outgoing_window
    val old = outgoing_window
    outgoing_window = outgoing_window + diff
    if (old != outgoing_window && established) {
      val flow = createAmqpFlow
      update_flow_state(flow)
      send(flow)
    }
  }

  def setIncomingWindow(window:Long) = {
    incoming_window_max = window
    val diff = incoming_window_max - incoming_window
    val old = incoming_window
    incoming_window = incoming_window + diff
    if (old != incoming_window && established) {
      val flow = createAmqpFlow
      update_flow_state(flow)
      send(flow)
    }
  }

}
