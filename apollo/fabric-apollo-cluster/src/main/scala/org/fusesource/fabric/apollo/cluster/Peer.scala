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

package org.fusesource.fabric.apollo.cluster

import dto._
import org.fusesource.hawtdispatch._
import java.lang.String
import org.apache.activemq.apollo.broker._
import org.apache.activemq.apollo.util._
import org.fusesource.hawtbuf.proto.MessageBuffer
import org.fusesource.fabric.apollo.cluster.model._
import org.fusesource.hawtbuf._
import scala.collection.mutable.{HashMap, HashSet}
import org.fusesource.hawtbuf.Buffer._
import scala.util.continuations._
import org.apache.activemq.apollo.dto.{DestinationDTO, XmlCodec}
import org.apache.activemq.apollo.broker.protocol.ProtocolFactory
import org.apache.activemq.apollo.broker.store.MessageRecord
import org.fusesource.fabric.apollo.cluster.protocol.{ClusterProtocolConstants, ClusterProtocolCodec, ClusterProtocolHandler}
import ClusterProtocolConstants._

object Peer extends Log

/**
 * A peer is remote broker which is part of of the local broker's cluster.
 * Multiple connections can exist to a peer broker and this class tracks
 * both the inbound and outbound connections to that peer.
 */
class Peer(cluster_connector:ClusterConnector, val id:String) extends Dispatched {
  import Peer._

  val channel_window_size: Int = 64*1024

  implicit def to_buffer(value:Long):Buffer = {
    val os = new DataByteArrayOutputStream
    os.writeVarLong(value)
    os.toBuffer
  }

  val dispatch_queue = createQueue("peer:"+id)

  var outbound:Queue = _
  var joined_cluster_at = System.currentTimeMillis
  var left_cluster_at = 0L
  var peer_info:ClusterNodeDTO = _

  ///////////////////////////////////////////////////////////////////////////////
  //
  // Connection Lifecycle.  These occur for both inbound and outbound connections.
  //
  ///////////////////////////////////////////////////////////////////////////////
  var connecting = false
  var handlers = HashSet[ClusterProtocolHandler]()

  var primary:ClusterProtocolHandler = _
  var session_manager:SessionSinkMux[Frame] = _
  var connection_sink:Sink[Frame] = _

  private def connection_send(command:Int, data:MessageBuffer[_,_]):Unit = connection_send(command, data.toFramedBuffer)
  private def connection_send(command:Int, data:Buffer):Unit = connection_send(Frame(command, data))
  private def connection_send(frame:Frame):Unit = dispatch_queue {
    if( connection_sink!=null ) {
      val accepted = connection_sink.offer(frame)
      assert(accepted)
    }
  }

  def on_client_connected(handler:ClusterProtocolHandler) = {
    val accepted =  handler.connection.transport.offer(create_hello_frame(handler))
    assert(accepted)
  }

  def on_client_hello(handler:ClusterProtocolHandler, hello:ProtocolHello.Buffer) = {
    // This is the hello sent from the client.. so we are the server.
    on_hello(handler, hello)

    // we need to send the client a hello now..
    handler.dispatch_queue {
      val accepted =  handler.connection.transport.offer(create_hello_frame(handler))
      assert(accepted)
    }
  }

  def on_server_hello(handler:ClusterProtocolHandler, hello:ProtocolHello.Buffer) = {
    // Server just responded to the client hello we sent in on_client_connected method.
    on_hello(handler, hello)
  }

  private def on_hello(handler:ClusterProtocolHandler, hello:ProtocolHello.Buffer) = {

    if( hello.getVersion != PROTOCOL_VERSION ) {
      handler.die("Unsupported protocol version: "+hello.getVersion)
    }

    if(  id != hello.getId  ) {
      handler.die("Peer's id does not match what was expected.")
    }

    val local_tokens:Set[String] = {
      import collection.JavaConversions._
      collectionAsScalaIterable(cluster_connector.config.security_tokens).toSet
    }

    // does the client need to give us a matching token?
    if( !local_tokens.isEmpty ) {
      // We just need one token to match.  Nodes may be configured with multiple tokens
      // when a token is getting changed across a cluster.
      val remote_tokens:Set[String] = collection.JavaConversions.collectionAsScalaIterable(hello.getSecurityTokensList).toSet

      val intersection = local_tokens.intersect( remote_tokens )
      if( intersection.isEmpty ) {
        handler.die("Peer did not supply a valid security token.")
      }
    }

    dispatch_queue {
      handlers += handler
      connecting = false
      if( primary == null ) {
        make_primary(handler)
      }
    }
  }

  def create_hello_frame(handler:ClusterProtocolHandler) = {
    val hello = new ProtocolHello.Bean
    hello.setVersion(PROTOCOL_VERSION)
    hello.setId(cluster_connector.node_id)
    hello.setAddress(handler.connection.transport.getRemoteAddress.toString)
    hello.addAllSecurityTokens(cluster_connector.config.security_tokens)
    Frame(COMMAND_HELLO, hello.freeze.toFramedBuffer)
  }

  def on_peer_disconnected(handler:ClusterProtocolHandler) = dispatch_queue {
    handlers -= handler
    if( handler == primary ) {
      outbound_channels.values.foreach( _.peer_disconnected )
      primary = null
      if( !handlers.isEmpty ) {
        make_primary(handlers.head)
      }
    }

  }

  def make_primary(handler: ClusterProtocolHandler): Unit = {
    assert_executing
    primary = handler
    session_manager = new SessionSinkMux[Frame](handler.connection.transport_sink.map(x => x), handler.dispatch_queue, Frame)
    connection_sink = new OverflowSink(session_manager.open(dispatch_queue));

    // resend the consumer infos...
    exported_consumers.values.foreach { consumer =>
      connection_send(COMMAND_ADD_CONSUMER, consumer.consumer_info)
    }

    outbound_channels.values.foreach(_.peer_connected)
  }

  //
  // Lets try to maintain at least one connection up for now.. we might
  // want to relax this in big clusters.
  //
  def check() = dispatch_queue {
    // Should we try to connect to the peer?
    if( !connecting  && handlers.isEmpty && peer_info.cluster_address!=null ) {
      connecting = true
      cluster_connector.connect(peer_info.cluster_address) {
        case Success(connection) =>
          connection.transport.setProtocolCodec(new ClusterProtocolCodec)
          connection.protocol_handler = new ClusterProtocolHandler(this)
          connection.protocol_handler.connection = connection
          connection.protocol_handler.on_transport_connected

        case Failure(error) =>
          connecting = false
      }
    }
  }

  def close() = {
    handlers.foreach{ handler=>
      if( handler == primary ) {
        outbound_channels.values.foreach( _.peer_disconnected )
        primary = null
      }
      handlers -= handler
      handler.connection.stop
    }
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Internal support methods.
  //
  //////////////////////////////////////////////////////////////////////////////
  private def get_virtual_host(host:AsciiBuffer) = cluster_connector.broker.get_virtual_host(host).asInstanceOf[ClusterVirtualHostDTO]

  ///////////////////////////////////////////////////////////////////////////////
  //
  // Handle events from the connections.
  //
  ///////////////////////////////////////////////////////////////////////////////
  def on_frame(source:ClusterProtocolHandler, frame:Frame) = dispatch_queue {
    trace(id+" got "+to_string(frame))
    frame.command match {

      case COMMAND_ADD_CONSUMER =>
        val consumer = ConsumerInfo.FACTORY.parseFramed(frame.data)
        on_add_consumer(consumer)

      case COMMAND_REMOVE_CONSUMER =>
        val consumer = ConsumerInfo.FACTORY.parseFramed(frame.data)
        on_remove_consumer(consumer)

      case COMMAND_CHANNEL_OPEN =>
        val producer = ChannelOpen.FACTORY.parseFramed(frame.data)
        on_channel_open(producer)

      case COMMAND_CHANNEL_SEND =>
        val delivery = ChannelDelivery.FACTORY.parseFramed(frame.data)
        on_channel_send(delivery)

      case COMMAND_CHANNEL_ACK =>
        val ack = ChannelAck.FACTORY.parseFramed(frame.data)
        on_channel_ack(ack)

      case COMMAND_CHANNEL_CLOSE =>
        on_channel_close(frame.data.buffer.bigEndianEditor.readVarInt)

      case value =>
        source.die("Unkown command value: "+source)
    }
  }

  def to_string(frame:Frame) = {
    frame.command match {
      case COMMAND_ADD_CONSUMER =>
        "COMMAND_ADD_CONSUMER("+ConsumerInfo.FACTORY.parseFramed(frame.data)+")"
      case COMMAND_REMOVE_CONSUMER =>
        "COMMAND_REMOVE_CONSUMER("+ConsumerInfo.FACTORY.parseFramed(frame.data)+")"
      case COMMAND_CHANNEL_OPEN =>
        "COMMAND_CHANNEL_OPEN("+ChannelOpen.FACTORY.parseFramed(frame.data)+")"
      case COMMAND_CHANNEL_SEND =>
        "COMMAND_CHANNEL_SEND("+ChannelDelivery.FACTORY.parseFramed(frame.data)+")"
      case COMMAND_CHANNEL_ACK =>
        "COMMAND_CHANNEL_ACK("+ChannelAck.FACTORY.parseFramed(frame.data)+")"
      case COMMAND_CHANNEL_CLOSE =>
        "COMMAND_CHANNEL_CLOSE("+frame.data.buffer.bigEndianEditor.readVarInt+")"
      case value =>
        "UNKNOWN"
    }
  }

  def create_connection_status(source:ClusterProtocolHandler) = {
    var rc = new ClusterConnectionStatusDTO

    rc.waiting_on = source.waiting_on
    rc.node_id = id
    rc.exported_consumer_count = exported_consumers.size
    rc.imported_consumer_count = imported_consumers.size

    outbound_channels.foreach { case (key,value)=>
      val s = new ChannelStatusDTO
      s.id = key
      s.byte_credits = value.byte_credits
      s.delivery_credits = value.delivery_credits
      s.connected = value.connected
      rc.outbound_channels.add(s)
    }

    inbound_channels.foreach { case (key,value)=>
      val s = new ChannelStatusDTO
      s.id = key
      s.byte_credits = value.byte_credits
      s.delivery_credits = value.delivery_credits
      s.connected = value.connected
      rc.outbound_channels.add(s)
    }

    rc
  }

  ///////////////////////////////////////////////////////////////////////////////
  //
  // Consumer management.  Allows master brokers to know about consumers
  // on remote peers.
  //
  ///////////////////////////////////////////////////////////////////////////////

  var next_consumer_id = 0L
  val exported_consumers = HashMap[Long, ExportedConsumer]()
  val imported_consumers = HashMap[Long, ClusterDeliveryConsumer]()

  def add_cluster_consumer( bean:ConsumerInfo.Bean, consumer:DeliveryConsumer) = dispatch_queue ! {

    val consumer_id = next_consumer_id
    bean.setConsumerId(consumer_id)
    next_consumer_id += 1

    val exported = new ExportedConsumer(bean.freeze, consumer)
    exported_consumers.put(consumer_id, exported)
    connection_send(COMMAND_ADD_CONSUMER, exported.consumer_info)
    exported
  }

  case class ExportedConsumer(consumer_info:ConsumerInfo.Buffer, consumer:DeliveryConsumer) {
    def close() = {
      assert_executing
      exported_consumers -= consumer_info.getConsumerId.longValue
      connection_send(COMMAND_REMOVE_CONSUMER, consumer_info)
    }
  }

  private def unit = {}

  def on_add_consumer(consumer_info:ConsumerInfo.Buffer) = {
    assert_executing
    val consumer_id = consumer_info.getConsumerId.longValue
    if( !imported_consumers.contains(consumer_id) ) {
      val consumer = new ClusterDeliveryConsumer(consumer_info)
      imported_consumers.put(consumer_id, consumer)

      reset[Unit,Unit] {
        val host = cluster_connector.broker.get_virtual_host(consumer_info.getVirtualHost)
        // assert(host!=null, "Unknown virtual host: "+consumer_info.getVirtualHost)
        val router = host.router.asInstanceOf[ClusterRouter]
        router.bind(consumer.destinations, consumer, null)
        unit // continuations compiler is not too smart..
      }
    }
  }


  def on_remove_consumer(info:ConsumerInfo.Buffer) = {
    assert_executing
    imported_consumers.remove(info.getConsumerId.longValue).foreach { consumer=>
      reset {
        val host = cluster_connector.broker.get_virtual_host(consumer.info.getVirtualHost)
        if( host!=null ) {
          val router = host.router.asInstanceOf[ClusterRouter]
          router.unbind(consumer.destinations, consumer, false, null)
        }
      }
    }
  }

  class ClusterDeliveryConsumer(val info:ConsumerInfo.Buffer) extends BaseRetained with DeliveryConsumer {

    import collection.JavaConversions._

    def consumer_id = info.getConsumerId.longValue
    def destinations = info.getDestinationList.toSeq.toArray.map { x=>
      XmlCodec.decode(classOf[DestinationDTO], new ByteArrayInputStream(x))
    }


    def matches(message: Delivery): Boolean = true
    def is_persistent: Boolean = false
    def dispatch_queue: DispatchQueue = Peer.this.dispatch_queue

    def connect(p: DeliveryProducer): DeliverySession = {

      val open = new ChannelOpen.Bean
      open.setConsumerId(consumer_id)

      new MutableSink[Delivery] with DeliverySession {

        var closed = false
        reset {
          val channel = open_channel(p.dispatch_queue, open)
          if( !closed ) {
            downstream = Some(channel)
          } else {
            channel.close
          }
        }

        def close: Unit = {
          if( !closed ) {
            closed = true
            downstream.foreach(_.asInstanceOf[Peer#OutboundChannelSink].close)
          }
        }

        def producer: DeliveryProducer = p
        def consumer: DeliveryConsumer = ClusterDeliveryConsumer.this

        def remaining_capacity = downstream.map(_.asInstanceOf[OutboundChannelSink].remaining_capacity).getOrElse(0)

        @volatile
        var enqueue_item_counter = 0L
        @volatile
        var enqueue_size_counter = 0L
        @volatile
        var enqueue_ts = 0L

        override def offer(value: Delivery) = {
          if( super.offer(value) ){
            enqueue_item_counter += 1
            enqueue_size_counter += value.size
            enqueue_ts = now
            true
          } else {
            false
          }
        }

      }
    }
  }

  def now = this.cluster_connector.broker.now
  ///////////////////////////////////////////////////////////////////////////////
  //
  // Channel Management:  A channel provides a flow controlled message
  // delivery window between the brokers.  Used for both producer to destination
  // and destination to consumer deliveries.
  //
  ///////////////////////////////////////////////////////////////////////////////

  var next_channel_id = 0L
  val outbound_channels = HashMap[Long, OutboundChannelSink]()

  def open_channel(q:DispatchQueue, open:ChannelOpen.Bean) = dispatch_queue ! {
    open.setChannel(next_channel_id)
    next_channel_id += 1

    val channel = new OutboundChannelSink(q, open.freeze)
    outbound_channels.put(channel.id, channel)

    debug("opening channel %d to peer %s", channel.id, id)
    connection_send(COMMAND_CHANNEL_OPEN, channel.open_command)
    channel
  }

  class OutboundChannelSink(val producer_queue:DispatchQueue, val open_command:ChannelOpen.Buffer) extends Sink[Delivery] with SinkFilter[Frame] {

    def id = open_command.getChannel.longValue

    // The deliveries waiting for acks..
    val waiting_for_ack = HashMap[Long, Delivery]()

    var last_seq_id = 0L

    // Messages overflow.. while the peer is disconnected.
    val sink_switcher = new MutableSink[Frame]()

    // On the next delivery, we will tell the consumer what was the
    // last ack from him that we saw, so that he can discard data
    // needed to do duplicate/redelivery detection.
    var next_ack_seq:Option[Long] = None

    def remaining_capacity = {
      if ( byte_credits > 0 ) {
        byte_credits
      } else {
        delivery_credits
      }
    }

    def byte_credits = session.byte_credits
    def delivery_credits = session.delivery_credits
    def connected = sink_switcher.downstream.isDefined

    val session = new OverflowSink[Frame](sink_switcher) {
      var byte_credits = 0
      var delivery_credits = 0

      override def full: Boolean = super.full || ( byte_credits <= 0 && delivery_credits <= 0 )

      override def offer(frame: Frame): Boolean = {
        byte_credits -= frame.data.length
        delivery_credits -= 1
        trace("outbound-channel %d: decreased credits (%d,%d) ... window (%d,%d)".format(open_command.getChannel.longValue, 1, frame.data.length, delivery_credits, byte_credits));
        super.offer(frame)
      }

      def on_channel_ack(ack:ChannelAck.Buffer) = {
        val was_full = full
        if( ack.hasByteCredits ) {
          byte_credits += ack.getByteCredits
        }
        if( ack.hasDeliveryCredits ) {
          delivery_credits += ack.getDeliveryCredits
        }
        trace("outbound-channel %d: increased credits (%d,%d) ... window (%d,%d)".format(open_command.getChannel.longValue, ack.getDeliveryCredits, ack.getByteCredits, delivery_credits, byte_credits));
        if( !full && was_full ) {
          drain
        }
      }
    }

    if( primary!=null ) {
      peer_connected
    }

    def peer_connected = {
      producer_queue {
        sink_switcher.downstream = Some(session_manager.open(producer_queue))
        // Queue up the re-deliveries...
        waiting_for_ack.foreach { case (seq, delivery) =>
          session.offer(to_frame(Some(seq), delivery))
        }
      }
    }

    def peer_disconnected = {
      sink_switcher.downstream = None
      next_ack_seq = None
      session.clear
    }

    def close() = {
      outbound_channels.remove(id)
      connection_send(COMMAND_CHANNEL_CLOSE, open_command.getChannel)
    }


    def next_seq_id = {
      val rc = last_seq_id
      last_seq_id += 1
      rc
    }

    def to_frame(seq:Option[Long], value: Delivery):Frame = {
      val record = value.createMessageRecord
      val bean = new ChannelDelivery.Bean
      bean.setChannel(open_command.getChannel)
      seq.foreach(bean.setSeq(_))
      bean.setProtocol(record.protocol)
      bean.setData(record.buffer)
      bean.setSize(record.size)
      next_ack_seq.foreach{x=>
        bean.setAckSeq(x)
        next_ack_seq = None
      }
      Frame(COMMAND_CHANNEL_SEND, bean.freeze.toFramedBuffer)
    }


    def downstream = session

    def offer(delivery: Delivery): Boolean = {
      if( full ) {
        false
      } else {
        // deliveries only get a seq if they need an ack..
        val seq = Option(delivery.ack).map { x=>
          val seq = next_seq_id
          waiting_for_ack.put(seq, delivery)
          seq
        }
        session.offer(to_frame(seq, delivery))
      }
    }


    def on_channel_ack(ack:ChannelAck.Buffer) = producer_queue {
      session.on_channel_ack(ack)
      if( ack.hasDeliverySeq ) {
        import collection.JavaConversions._
        val l = collectionAsScalaIterable(ack.getDeliverySeqList)
        next_ack_seq = Some(l.last.longValue)
        l.foreach { seq =>
          val delivery = waiting_for_ack.remove(seq.longValue)
          assert(delivery.isDefined)
          delivery.get.ack(Consumed, null)
        }
      }
    }


  }

  def on_channel_ack(ack:ChannelAck.Buffer) = {
    outbound_channels.get(ack.getChannel.longValue).foreach(
      _.on_channel_ack(ack)
    )
  }

  val inbound_channels = HashMap[Long, InboundChannelSink]()

  class InboundChannelSink(val open_command:ChannelOpen.Buffer) extends Sink[ChannelDelivery.Buffer] with SinkFilter[ChannelDelivery.Buffer] {

    def channel_id = open_command.getChannel.longValue

    def byte_credits = sink_switcher.byte_credits
    def delivery_credits = sink_switcher.delivery_credits
    def connected = sink_switcher.downstream.isDefined

    // We use a source to merge multiple ack events into a single ack message.
    val ack_source = createSource(new EventAggregator[ChannelAck.Bean, ChannelAck.Bean]() {
      def mergeEvents(p1: ChannelAck.Bean, p2: ChannelAck.Bean): ChannelAck.Bean = mergeEvent(p1,p2)
      def mergeEvent(previous: ChannelAck.Bean, event: ChannelAck.Bean): ChannelAck.Bean = {
        if( previous == null ) {
          event
        } else {
          if(event.hasByteCredits) {
            previous.setByteCredits(previous.getByteCredits + event.getByteCredits)
          }
          if(event.hasDeliveryCredits) {
            previous.setDeliveryCredits(previous.getDeliveryCredits + event.getDeliveryCredits)
          }
          if(event.hasDeliverySeq) {
            previous.addAllDeliverySeq(event.getDeliverySeqList)
          }
          previous
        }
      }
    }, dispatch_queue)

    ack_source.onEvent {
      val ack = ack_source.getData
      trace("inbound-channel %d: sending credits (%d,%d) ... window (%d,%d)".format(open_command.getChannel.longValue, ack.getDeliveryCredits, ack.getByteCredits, sink_switcher.delivery_credits, sink_switcher.byte_credits));
      ack.setChannel(channel_id)
      connection_send(COMMAND_CHANNEL_ACK, ack.freeze)
    }
    ack_source.resume

    // Messages overflow.. while the peer is disconnected.
    val sink_switcher = new MutableSink[ChannelDelivery.Buffer]() {
      var byte_credits = 0
      var delivery_credits = 0

      // our downstream sink is an overflow.. so he can handle buffering up
      // any extra we give him.
      override def full: Boolean = byte_credits <= 0 && delivery_credits <= 0

      override def offer(value: ChannelDelivery.Buffer): Boolean = {
        trace("inbound-channel %d: reducing credits (%d,%d) ... window (%d,%d)".format(open_command.getChannel.longValue, 1, value.serializedSizeFramed, delivery_credits, byte_credits));

        byte_credits -= value.serializedSizeFramed
        delivery_credits -= 1
        super.offer(value)
      }
    }

    // flow control is maintained using a credit window.  We
    // allow internal overflow as long as the remote peer does not
    // violate his credit window.
    override def full = false

    def downstream = sink_switcher

    def offer(value: ChannelDelivery.Buffer): Boolean = {
      if( value.hasProtocol ) {
        if( sink_switcher.full ) {
          // oh oh.. sender violated the credit window.
          warn("channel: %d sender violated the flow control window", open_command.getChannel.longValue)

          val ack = new ChannelAck.Bean()
          ack.setError(ascii("flow control window violation"))
          ack.setChannel(channel_id)
          connection_send(COMMAND_CHANNEL_ACK, ack.freeze)

          false
        } else {
          sink_switcher.offer(value)
        }
      }

      if( value.hasAckSeq ) {
        // TODO:
      }
      true
    }

    def to_delivery(value: ChannelDelivery.Buffer):Delivery = {

      val message_record = new MessageRecord
      message_record.buffer = value.getData
      message_record.protocol = value.getProtocol
      message_record.size = value.getSize

      val delivery = new Delivery
      delivery.message = ProtocolFactory.get(value.getProtocol.toString).get.decode(message_record)
      delivery.size = message_record.size
      delivery.ack = if( value.hasSeq ) {
        (x, uow)=> {
          // message was acked.. now we need to post it back.

          // TODO: use an event source to coalesce multiple events.
          val ack = new ChannelAck.Bean()
          ack.addDeliverySeq(value.getSeq)
          ack_source.merge(ack)

        }
      } else {
        null
      }
      delivery
    }

    def send_credit(delivery_credits:Int, byte_credits:Int) = {
      val ack = new ChannelAck.Bean()
      if( delivery_credits != 0 ) {
        sink_switcher.delivery_credits += delivery_credits
        ack.setDeliveryCredits(delivery_credits)
      }
      if( byte_credits!=0 ) {
        sink_switcher.byte_credits += byte_credits
        ack.setByteCredits(byte_credits)
      }
      ack_source.merge(ack)
    }

    def set_producer_route(route:ClusterDeliveryProducerRoute) = {
      val mapped:Sink[ChannelDelivery.Buffer] = route.map(to_delivery(_))
      sink_switcher.downstream = Some(new OverflowSink(mapped) {
        override protected def onDelivered(value: ChannelDelivery.Buffer) = {
          send_credit(1, value.serializedSizeFramed)
        }
      })
      send_credit(1, channel_window_size)
    }

    def close = {
//      TODO:
    }
  }

  class ClusterDeliveryProducerRoute(router:ClusterRouter, val info:ChannelOpen.Buffer) extends DeliveryProducerRoute(router) {

    import collection.JavaConversions._
    def consumer_id = info.getConsumerId.longValue
    def destinations = info.getDestinationList.toSeq.toArray.map { x=>
      XmlCodec.decode(classOf[DestinationDTO], new ByteArrayInputStream(x))
    }

    override def dispatch_queue: DispatchQueue = Peer.this.dispatch_queue
  }

  def on_channel_open(open:ChannelOpen.Buffer) = {
    val channel_id = open.getChannel.longValue

    val existing = inbound_channels.get(channel_id)
    if( !existing.isDefined || open!=existing.get.open_command ) {

      if( open.hasConsumerId ) {
        debug("Peer %s opened channel %d to consumer %d".format(id, channel_id, open.getConsumerId))

        exported_consumers.get(open.getConsumerId) match {
          case Some(target) =>

            val route = new ClusterDeliveryProducerRoute(null, open)
            route.bind(target.consumer::Nil)

            val sink = new InboundChannelSink(open)
            inbound_channels.put(channel_id, sink)
            sink.set_producer_route(route)

          case None =>
            val ack = new ChannelAck.Bean()
            ack.setChannel(channel_id)
            ack.setError(ascii("consumer does not exist"))
            connection_send(COMMAND_CHANNEL_ACK, ack.freeze)
        }
      } else {

        // If the channel is not sending to a consumer, it must be sending
        // to a destination.
        assert( open.hasVirtualHost && open.hasDestination )

        debug("Peer %s opened channel %d to destinations %s on host %s".format(id, channel_id, open.getDestinationList, open.getVirtualHost))

        val sink = new InboundChannelSink(open)
        inbound_channels.put(channel_id, sink)

        reset {
          val host = cluster_connector.broker.get_virtual_host(open.getVirtualHost)
//          if( host==null ) {
//
//            // TODO: perhaps cluster config is not consistent across all the nodes.
//            warn("Unknown virtual host: %s", open.getVirtualHost)
//            inbound_channels.remove(channel_id)
//
//          } else {
            val router = host.router.asInstanceOf[ClusterRouter]
            val producer = new ClusterDeliveryProducerRoute(router, open)
            router.connect(producer.destinations, producer, null)
            sink.set_producer_route(producer)
//          }
        }


      }

    }
  }

  def on_channel_close(channel:Long) = {
    inbound_channels.remove(channel).foreach { channel =>
      channel.close
    }
  }

  def on_channel_send(delivery:ChannelDelivery.Buffer) = {
    inbound_channels.get(delivery.getChannel.longValue) match {
      case Some(channel) =>
        channel.offer(delivery)

      case None =>
        val ack = new ChannelAck.Bean()
        ack.setChannel(delivery.getChannel)
        ack.setError(ascii("channel does not exist"))
        connection_send(COMMAND_CHANNEL_ACK, ack.freeze)
    }
  }


}



