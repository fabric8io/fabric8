/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.broker

import org.apache.activemq.apollo.broker.protocol.ProtocolHandler
import java.io.IOException
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.protocol._
import AmqpConstants._
import org.fusesource.fabric.apollo.amqp.api._
import org.apache.activemq.apollo.util.{Logging, Log}
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.hawtbuf.AsciiBuffer.ascii
import org.apache.activemq.apollo.broker.{VirtualHost, BrokerConnection, DestinationParser}
import java.net.URI

// TODO - map Dynamic linkage to temp queue/topic
class AmqpProtocolHandler extends AmqpConnection with ProtocolHandler with SessionListener with LinkListener with Logging {

  //trace("Constructing new AmqpProtocolHandler")

  val parser = new DestinationParser
  var deliveryConsumers = Map[String, AmqpDeliveryConsumer]()
  var deliveryProducers = Map[String, AmqpDeliveryProducer]()
  var waiting_on: String = "client request"

  def protocol = PROTOCOL
  var host:VirtualHost = null

  override def init(uri: String) = {
    //trace("Received new connection from %s", uri)
    this.uri = new URI(uri)
    dispatchQueue = connection.dispatch_queue
    transport = connection.transport
    setSessionListener(this)
  }

  override protected def stop(on_stop:Runnable):Unit = connection.stop(on_stop)

  override def open(open:AmqpOpen) = {
    Option(open) match {
      case Some(open) =>
        reset {
          suspendRead("Virtual hostname lookup")
          val h = Option(open.getHostname) match {
            case Some(hostname) =>
              this.hostname = Option(hostname.asInstanceOf[String])
              connection.connector.broker.get_virtual_host(ascii(hostname))
            case None =>
              connection.connector.broker.get_default_virtual_host
          }
          resumeRead
          if (h == null) {
            throw new RuntimeException("virtual host \"" + open.getHostname + "\" not found");
          }
          host = h
          Option(host.toString).foreach( (x) => containerId = x)
          info("Using containerId %s and hostname %s", containerId, hostname)
          super.open(open)
        }
      case None =>
    }
  }

  override def set_connection(brokerConnection:BrokerConnection) = {
    this.connection = brokerConnection
    init(connection.transport.getTypeId + "://" + connection.transport.getRemoteAddress)
  }

  def sessionReleased(connection: Connection, session: Session) = {
    info("Released session for client %s : %s", uri, session)
  }

  def sessionCreated(connection: Connection, session: Session) = {
    info("Created new session for client %s : %s", uri, session)
    session.setLinkListener(this)
  }

  def suspendRead(reason: String) = {
    waiting_on = reason
    connection.transport.suspendRead
  }

  def resumeRead = {
    waiting_on = "client request"
    connection.transport.resumeRead
  }

  def receiverDetaching(session: Session, sender: Sender) = {
  }

  def senderDetaching(session: Session, receiver: Receiver) = {
  }

  def receiverAttaching(session: Session, sender: Sender) = {
    sender.setOnDetach(^{
      info("Detaching sender %s", sender)
      val name = sender.getName
      deliveryConsumers.get(name) match {
        case Some(deliveryConsumer) =>
          // TODO - set persistent flag if link is durable
          info("Unbinding delivery consumer from destination %s", deliveryConsumer.destination)
          host.router.unbind(deliveryConsumer.destination, deliveryConsumer, false , null)
          deliveryConsumers -= name
        case None =>
          info("No delivery consumer found for link name %s", name)
      }
    })
    connectDeliveryConsumer(sender)
    info("Created new outgoing link %s", sender)
  }

  def senderAttaching(session: Session, receiver: Receiver) = {
    receiver.setOnDetach(^{
      info("Detaching receiver %s", receiver)
      val name = receiver.getName
      deliveryProducers.get(name) match {
        case Some(listener) =>
          info("Disconnecting message producer from destination %s", listener.destination)
          host.router.disconnect(listener.destination, listener.producer)
          deliveryProducers -= name
        case None =>
          info("No delivery producer found for link name %s", name)
      }
    })
    receiver.setListener(new AmqpDeliveryProducer(this, receiver, get_destination(receiver)))
    info("Created new incoming link %s", receiver)
  }

  import org.apache.activemq.apollo.util.Failure
  import org.apache.activemq.apollo.util.Success

  def get_destination[T <: Link](link:T) = {
    val addr = link.getAddress
    if (addr.startsWith(parser.queue_prefix)) {
      parser.decode_destination(addr)
    } else if (addr.startsWith(parser.topic_prefix)) {
      parser.decode_destination(addr)
      // TODO - handle "dynamic" linkage and set prefix based on distribution mode
    } else {
      Option(link.getDistributionMode) match {
        case Some(mode) =>
          mode match {
            case DistributionMode.MOVE =>
              val new_addr = parser.queue_prefix + addr.toString
              parser.decode_destination(new_addr)
            case DistributionMode.COPY =>
              val new_addr = parser.topic_prefix + addr.toString
              parser.decode_destination(new_addr)
          }
        case None =>
          throw new IllegalArgumentException("Address (" + addr + ") has no prefix and no distribution mode specified")
      }

    }
  }

  def connectDeliveryConsumer(sender:Sender): Unit = {
    if (deliveryConsumers.contains(sender.getName)) {
      throw new RuntimeException("Link name \"" + sender.getName + "\" in use")
    }
    val deliveryConsumer = new AmqpDeliveryConsumer(this, sender, get_destination(sender))
    debug("Binding delivery consumer to destination %s", deliveryConsumer.destination)
    deliveryConsumers += (sender.getName -> deliveryConsumer)
    reset {
      val x = host.router.bind(deliveryConsumer.destination, deliveryConsumer, null)
      deliveryConsumer.release
      x match {
        case None =>

        case Some(reason) =>
          deliveryConsumers -= sender.getName
          sender.detach(reason)
      }
    }
  }

  def connectDeliveryProducer(listener:AmqpDeliveryProducer) {
    // don't process frames until producer is connected...
    connection.transport.suspendRead
    deliveryProducers += (listener.link.getName -> listener)
    reset {
      val rc = host.router.connect(listener.destination, listener.producer, null);
      rc match {
        case Some(failure) =>
          deliveryProducers -= listener.link.getName
          close(failure)
        case None =>
          if (!connection.stopped) {
            resumeRead
          }
      }
    }
  }

  override def on_transport_failure(error:IOException) = super.onTransportFailure(error)

  override def on_transport_connected = {
    transport_sink = connection.transport_sink
    super.onTransportConnected
  }

  override def on_transport_command(command:AnyRef) = super.onTransportCommand(command)

  override def on_transport_disconnected = super.onTransportDisconnected

}
