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

package org.apache.activemq.apollo.mqtt

import org.fusesource.hawtbuf._
import dto.{MqttConnectionStatusDTO, MqttDTO}
import org.fusesource.hawtdispatch._

import org.apache.activemq.apollo.broker._
import java.lang.String
import protocol.{ProtocolFilter, ProtocolHandler}
import security.SecurityContext
import org.apache.activemq.apollo.util._
import java.util.concurrent.TimeUnit
import java.util.Map.Entry
import java.security.cert.X509Certificate
import java.io.IOException
import org.apache.activemq.apollo.dto._
import java.util.regex.Pattern
import org.fusesource.mqtt.client.QoS._
import org.fusesource.mqtt.client.Topic
import org.fusesource.mqtt.codec.CONNACK.Code._
import org.fusesource.mqtt.client.QoS
import org.fusesource.hawtdispatch.transport.{SecureTransport, HeartBeatMonitor, SslTransport}
import org.apache.activemq.apollo.util.path.{Path, PathParser, PathMap}
import org.fusesource.mqtt.codec._
import scala.collection.mutable.{HashSet, HashMap}
import org.apache.activemq.apollo.mqtt.MqttSessionManager._
import org.apache.activemq.apollo.broker.store.{Store, StoreUOW}
import scala.Array._

object MqttProtocolHandler extends Log {
  
  case class Request(id:Short, message:MessageSupport.Message, ack:(DeliveryResult)=>Unit) {
    val frame = if(message==null) null else message.encode()
    var delivered = false
  }

  def received[T](value:T):T = {
    trace("received: %s", value)
    value
  }

  val WAITING_ON_CLIENT_REQUEST = ()=> "client request"
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class MqttProtocolHandler extends ProtocolHandler {
  import MqttProtocolHandler._

  def protocol = "mqtt"

  def broker = connection.connector.broker
  def queue = connection.dispatch_queue

  var connection_log:Log = MqttProtocolHandler
  var config:MqttDTO = _

  def destination_parser = {
    var destination_parser = MqttProtocol.destination_parser
    if( config.queue_prefix!=null ||
        config.path_separator!= null ||
        config.any_child_wildcard != null ||
        config.any_descendant_wildcard!= null ||
        config.regex_wildcard_start!= null ||
        config.regex_wildcard_end!= null ||
        config.part_pattern!= null
    ) {
      destination_parser = new DestinationParser().copy(destination_parser)
      if( config.queue_prefix!=null ) { destination_parser.queue_prefix = config.queue_prefix }
      if( config.path_separator!=null ) { destination_parser.path_separator = config.path_separator }
      if( config.any_child_wildcard!=null ) { destination_parser.any_child_wildcard = config.any_child_wildcard }
      if( config.any_descendant_wildcard!=null ) { destination_parser.any_descendant_wildcard = config.any_descendant_wildcard }
      if( config.regex_wildcard_start!=null ) { destination_parser.regex_wildcard_start = config.regex_wildcard_start }
      if( config.regex_wildcard_end!=null ) { destination_parser.regex_wildcard_end = config.regex_wildcard_end }
      if( config.part_pattern!=null ) { destination_parser.part_pattern = Pattern.compile(config.part_pattern) }
    }
    destination_parser
  }

  def protocol_filters = {
    import collection.JavaConversions._
    ProtocolFilter.create_filters(config.protocol_filters.toList, this)
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits related setting up a client connection
  //
  /////////////////////////////////////////////////////////////////////
  def session_id = security_context.session_id

  val security_context = new SecurityContext
  var sink_manager:SinkMux[Request] = null
  var connection_sink:Sink[Request] = null
  var codec:MQTTProtocolCodec = _

  override def on_transport_connected() = {
    import collection.JavaConversions._

    codec = connection.transport.getProtocolCodec.asInstanceOf[MQTTProtocolCodec]
    val connector_config = connection.connector.config.asInstanceOf[AcceptingConnectorDTO]
    config = connector_config.protocols.find( _.isInstanceOf[MqttDTO]).map(_.asInstanceOf[MqttDTO]).getOrElse(new MqttDTO)
    import OptionSupport._
    config.max_message_length.foreach( codec.setMaxMessageLength(_) )

    connection.transport match {
      case t:SslTransport=>
        security_context.certificates = Option(t.getPeerX509Certificates).getOrElse(Array[X509Certificate]())
      case _ => None
    }
    security_context.local_address = connection.transport.getLocalAddress
    security_context.remote_address = connection.transport.getRemoteAddress

    connection_log = connection.connector.broker.connection_log
    sink_manager = new SinkMux[Request]( connection.transport_sink.map { request=>
      trace("sent: %s", request.message)
      val frame = request.frame
      request.delivered = true
      if (request.id == 0 && request.ack != null) {
        request.ack(Consumed)
      }
      frame
    })
    connection_sink = new OverflowSink(sink_manager.open());
    resume_read
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits related tearing down a client connection
  //
  /////////////////////////////////////////////////////////////////////
  var closed = false
  def dead_handler(command:AnyRef):Unit = {}

  override def on_transport_disconnected() = {
    if( !closed ) {
      closed=true;
      dead = true;
      command_handler = dead_handler _

      security_context.logout( e => {
        if(e!=null) {
          connection_log.info(e, "MQTT connection '%s' log out error: %s", security_context.remote_address, e.toString)
        }
      })

      heart_beat_monitor.stop
      if( !connection.stopped ) {
        connection.stop()
      }
      trace("mqtt protocol resources released")
    }
  }

  override def on_transport_failure(error: IOException) = {
    if( !dead ) {
      command_handler("failure")
      dead = true
      command_handler = dead_handler _
      if( !connection.stopped ) {
        connection_log.info(error, "Shutting connection '%s'  down due to: %s", security_context.remote_address, error)
        super.on_transport_failure(error);
      }
    }
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits related managing connection flow control
  //
  /////////////////////////////////////////////////////////////////////

  var status = WAITING_ON_CLIENT_REQUEST
  def suspend_read(reason: => String) = {
    status = reason _
    connection.transport.suspendRead
    heart_beat_monitor.suspendRead
  }

  def resume_read() = {
    status = WAITING_ON_CLIENT_REQUEST
    connection.transport.resumeRead
    heart_beat_monitor.resumeRead
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits related to raising connection failure signals
  //
  /////////////////////////////////////////////////////////////////////

  var dead = false
  def die_delay = {
    import OptionSupport._
    config.die_delay.getOrElse(1000*5L)
  }

  class Break extends RuntimeException

  def async_die(msg:String, e:Throwable=null):Unit = try {
    die(msg, e)
  } catch {
    case x:Break=>
  }

  def async_die(response:MessageSupport.Message, msg:String):Unit = try {
    die(response, msg, null)
  } catch {
    case x:Break=>
  }

  def die[T](msg:String):T = die(null, msg, null)
  def die[T](msg:String, e:Throwable):T = die(null, msg, e)
  def die[T](response:MessageSupport.Message, msg:String):T = die(response, msg, null)
  def die[T](response:MessageSupport.Message, msg:String, e:Throwable):T = {
    if( e!=null) {
      connection_log.info(e, "MQTT connection '%s' error: %s", security_context.remote_address, msg, e)
    } else {
      connection_log.info("MQTT connection '%s' error: %s", security_context.remote_address, msg)
    }
    die(response)
  }

  def die[T](response:MessageSupport.Message):T = {
    if( !dead ) {
      command_handler("failure")
      dead = true
      command_handler = dead_handler _
      status = ()=>"shuting down"
      if( response!=null ) {
        connection.transport.resumeRead
        connection_sink.offer(Request(0, response, null))
        // TODO: if there are too many open connections we should just close the connection
        // without waiting for the error to get sent to the client.
        queue.after(die_delay, TimeUnit.MILLISECONDS) {
          connection.stop()
        }
      } else {
        connection.stop()
      }
    }
    throw new Break()
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits for dispatching client requests.
  //
  /////////////////////////////////////////////////////////////////////
  var command_handler: (AnyRef)=>Unit = connect_handler _

  override def on_transport_command(command:AnyRef)= {
    try {
      command_handler(command)
    }  catch {
      case e: Break =>
      case e:Exception =>
        // To avoid double logging to the same log category..
        var msg: String = "Internal Server Error: " + e
        if( connection_log!=MqttProtocolHandler ) {
          // but we also want the error on the apollo.log file.
          warn(e, msg)
        }
        async_die(msg, e);
    }
  }

  
  /////////////////////////////////////////////////////////////////////
  //
  // Bits related establishing the client connection
  //
  /////////////////////////////////////////////////////////////////////
  
  var connect_message:CONNECT = _
  var heart_beat_monitor = new HeartBeatMonitor
  var host:VirtualHost = _

  def connect_handler(command:AnyRef):Unit = command match {
    case s:MQTTProtocolCodec =>
      // this is passed on to us by the protocol discriminator
      // so we know which wire format is being used.
    case frame:MQTTFrame=>

      var command = frame
      protocol_filters.foreach { filter =>
        command = filter.filter(command)
      }

      command.messageType() match {
        case CONNECT.TYPE =>
          connect_message = received(new CONNECT().decode(command)) 
          on_mqtt_connect
        case _ =>
          die("Expecting an MQTT CONNECT message, but got: "+command.getClass);
      }
    case "failure" =>
    case _=>
      die("Internal Server Error: unexpected mqtt command: "+command.getClass);
  }

  def on_mqtt_connect:Unit = {
    
    val connack = new CONNACK

    if(connect_message.version!=3) {
      connack.code(CONNECTION_REFUSED_UNACCEPTED_PROTOCOL_VERSION)
      die(connack, "Unsupported protocol version: "+connect_message.version)
    }
    
    val client_id = connect_message.clientId()

    connection.transport match {
      case t:SecureTransport=>
        security_context.certificates = Option(t.getPeerX509Certificates).getOrElse(Array[X509Certificate]())
      case _ =>
    }
    security_context.user = Option(connect_message.userName).map(_.toString).getOrElse(null)
    security_context.password = Option(connect_message.password).map(_.toString).getOrElse(null)
    security_context.session_id = Some(client_id.toString)

    val keep_alive = connect_message.keepAlive
    if( keep_alive > 0 ) {
      heart_beat_monitor.setReadInterval((keep_alive*1.5).toLong*1000)
      heart_beat_monitor.setOnDead(^{
        async_die("Missed keep alive set to "+keep_alive+" seconds")
      });
    }
    heart_beat_monitor.suspendRead()
    heart_beat_monitor.setTransport(connection.transport)
    heart_beat_monitor.start

    suspend_read("virtual host lookup")
    broker.dispatch_queue {
      host = connection.connector.broker.get_default_virtual_host
      queue {
        resume_read
        if(host==null) {
          connack.code(CONNECTION_REFUSED_SERVER_UNAVAILABLE)
          async_die(connack, "Default virtual host not found.")
        } else if(!host.service_state.is_started) {
          connack.code(CONNECTION_REFUSED_SERVER_UNAVAILABLE)
          async_die(connack, "Default virtual host stopped.")
        } else {
          connection_log = host.connection_log
          if( host.authenticator!=null &&  host.authorizer!=null ) {
            suspend_read("authenticating and authorizing connect")
            host.authenticator.authenticate(security_context) { auth_err =>
              queue {
                if( auth_err!=null ) {
                  connack.code(CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD)
                  async_die(connack, auth_err+". Credentials="+security_context.credential_dump)
                } else if( !host.authorizer.can(security_context, "connect", connection.connector) ) {
                  connack.code(CONNECTION_REFUSED_NOT_AUTHORIZED)
                  async_die(connack, "Not authorized to connect to connector '%s'. Principals=".format(connection.connector.id, security_context.principal_dump))
                } else if( !host.authorizer.can(security_context, "connect", host) ) {
                  connack.code(CONNECTION_REFUSED_NOT_AUTHORIZED)
                  async_die(connack, "Not authorized to connect to virtual host '%s'. Principals=".format(host.id, security_context.principal_dump))
                } else {
                  resume_read
                  on_host_connected(host)
                }
              }
            }
          } else {
            on_host_connected(host)
          }
        }
      }
    }
  }
  
  def on_host_connected(host:VirtualHost):Unit = {
    MqttSessionManager.attach(host, connect_message.clientId(), this)
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Other msic bits.
  //
  /////////////////////////////////////////////////////////////////////
  var messages_sent = 0L
  var messages_received = 0L
  var subscription_count = 0

  override def create_connection_status = {
    var rc = new MqttConnectionStatusDTO
    rc.protocol_version = "3.1"
    rc.messages_sent = messages_sent
    rc.messages_received = messages_received
    rc.subscription_count = subscription_count
    rc.waiting_on = status()
    rc
  }

}


/**
 * Tracks active sessions so that we can ensure that a given
 * session id is only associated with once connection
 * at a time.  If a client tries to establish a 2nd
 * connection, the first one will be closed before the session
 * is switch to the new connection.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object MqttSessionManager {

  val queue = createQueue("session manager")

  class SessionState {
    var durable_sub:SubscriptionAddress = _
    val subscriptions = HashMap[UTF8Buffer, (Topic, BindAddress)]()
    val received_message_ids: HashSet[Short] = new HashSet[Short]

    trait StorageStrategy {
      def update(cb: =>Unit)
      def destroy(cb: =>Unit)
      def create(store:Store, client_id:UTF8Buffer)
    }
    case class NoopStrategy() extends StorageStrategy {
      def update(cb: =>Unit) = { cb }
      def destroy(cb: =>Unit) { cb }
      def create(store:Store, client_id:UTF8Buffer) = {
        if(store!=null)
          strategy = StoreStrategy(store, client_id)
      }
    }

    case class StoreStrategy(store:Store, client_id:UTF8Buffer) extends StorageStrategy {
      val session_key = new UTF8Buffer("mqtt:"+client_id)
      def update(cb: =>Unit) = {
        val uow = store.create_uow()
        val session_pb = new SessionPB.Bean
        session_pb.setClientId(client_id)
        received_message_ids.foreach(session_pb.addReceivedMessageIds(_))
        subscriptions.values.foreach { case (topic, address) =>
          val topic_pb = new TopicPB.Bean
          topic_pb.setName(topic.name())
          topic_pb.setQos(topic.qos().ordinal())
          topic_pb.setAddress(new UTF8Buffer(address.toString))
          session_pb.addSubscriptions(topic_pb)
        }
        uow.put(session_key, session_pb.freeze().toUnframedBuffer)

        val current = getCurrentQueue
        uow.on_complete {
          current {
            cb
          }
        }
        uow.release()
      }

      def destroy(cb: =>Unit) {
        val uow = store.create_uow()
        uow.put(session_key, null)
        val current = getCurrentQueue
        uow.on_complete {
          current {
            strategy = NoopStrategy()
            cb
          }
        }
        uow.release()
      }
      def create(store:Store, client_id:UTF8Buffer) = {
      }
    }
    var strategy:StorageStrategy = new NoopStrategy

  }


  case class HostState(host:VirtualHost) {
    val session_states = HashMap[UTF8Buffer, SessionState]()
    val sessions = HashMap[UTF8Buffer, MqttSession]()

    var loaded = false;
    def on_load(func: =>Unit) = {
      if( loaded ) {
        func
      } else {
        if(host.store!=null) {
          // We load all the persisted session's from the host's store when we are first accessed.
          queue.suspend()
          host.store.get_prefixed_map_entries(new AsciiBuffer("mqtt:")) { entries =>
            queue.resume()
            queue {
              for( (_, value) <- entries ) {
                import collection.JavaConversions._
                val session_pb = SessionPB.FACTORY.parseUnframed(value)
                val session_state = new SessionState()
                session_state.strategy.create(host.store, session_pb.getClientId)
                if( session_pb.hasReceivedMessageIds ) {
                  session_state.received_message_ids ++= session_pb.getReceivedMessageIdsList.map(_.toShort)
                }
                if( session_pb.hasSubscriptions ) {
                  session_pb.getSubscriptionsList.foreach { sub =>
                    val address = SimpleAddress(sub.getAddress.toString)
                    val topic = new Topic(sub.getName, QoS.values()(sub.getQos))
                    session_state.subscriptions += sub.getName -> (topic,address)
                  }
                }
                session_states.put(session_pb.getClientId, session_state)
              }
              loaded = true
              func
            }
          }
        } else {
          loaded = true
          func
        }
      }
    }
  }

  def attach(host:VirtualHost, client_id:UTF8Buffer, handler:MqttProtocolHandler) = queue {
    val host_state = host.plugin_state(new HostState(host), classOf[HostState])
    host_state.on_load {
      host_state.sessions.get(client_id) match {
        case Some(assignment) =>
          assignment.connect(handler)
        case None =>
          val state = if( handler.connect_message.cleanSession() ) {
            host_state.session_states.remove(client_id).getOrElse(new SessionState())
          } else {
            host_state.session_states.getOrElseUpdate(client_id, new SessionState())
          }
          val assignment = MqttSession(host_state, client_id, state)
          assignment.connect(handler)
          host_state.sessions.put(client_id, assignment)
      }
    }
  }

  def disconnect(host_state:HostState, client_id:UTF8Buffer, handler:MqttProtocolHandler) = queue {
    host_state.sessions.get(client_id) match {
      case Some(assignment) => assignment.disconnect(handler)
      case None => // Don't expect this to hit.
    }
  }

  def remove(host_state:HostState, client_id:UTF8Buffer) = queue {
    host_state.sessions.remove(client_id)
  }
}

/**
 * An MqttSession can be switch from one connection/protocol handler to another,
 * but it will only be associated with one at a time. An MqttSession tracks
 * the state of the communication with a client.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class MqttSession(host_state:HostState, client_id:UTF8Buffer, session_state:SessionState) {
  
  import MqttProtocolHandler._

  def host = host_state.host

  val queue = createQueue("mqtt: "+client_id)
  var manager_disconnected = false

  var handler:Option[MqttProtocolHandler] = None
  var security_context:SecurityContext = _
  var clean_session = false
  var protocol_filters = List[ProtocolFilter]()
  var connect_message:CONNECT = _
  var destination_parser = MqttProtocol.destination_parser

  def connect(next:MqttProtocolHandler):Unit = queue {
    if(manager_disconnected) {
      // we are not the assignment anymore.. go to the session manager
      // again to setup a new session.
      MqttSessionManager.attach(host, client_id, next)
    } else {
      
      // so that we don't switch again until this current switch completes
      queue.suspend()
      if( handler != None ) {
        detach
        handler = None
      }
      queue {
        handler=Some(next)
        attach
      }
      
      // switch the connection to the session queue..
      next.connection.set_dispatch_queue(queue) {
        queue.resume()
      }
    }
  }

  def disconnect(prev:MqttProtocolHandler) = queue {
    if( handler==Some(prev) ) {
      MqttSessionManager.remove(host_state, client_id)
      manager_disconnected = true
      detach
      handler = None
    }
  }
  /////////////////////////////////////////////////////////////////////
  //
  // Bits that deal with connections attaching/detaching from the session
  //
  /////////////////////////////////////////////////////////////////////
  def attach = {
    queue.assertExecuting()
    val h = handler.get
    clean_session = h.connect_message.cleanSession()
    security_context = h.security_context
    h.command_handler = on_transport_command _
    protocol_filters = h.protocol_filters
    destination_parser = h.destination_parser
    mqtt_consumer.consumer_sink.downstream = Some(h.sink_manager.open)

    def ack_connect = {
      queue.assertExecuting()
      connect_message = h.connect_message
      val connack = new CONNACK
      connack.code(CONNECTION_ACCEPTED)
      send(connack)
    }

    if( !clean_session ) {
      // Setup the previous subscriptions..
      session_state.strategy.create(host.store, client_id)
      if( !session_state.subscriptions.isEmpty ) {
        h.suspend_read("subscribing")
        subscribe(session_state.subscriptions.map(_._2._1)) {
          h.resume_read()
          h.queue {
            ack_connect
          }
        }
      } else {
        ack_connect
      }
    } else {
      // do we need to clear the received ids?
      // durable_session_state.received_message_ids.clear()
      session_state.subscriptions.clear()
      if( session_state.durable_sub !=null ) {
        var addresses = Array(session_state.durable_sub)
        session_state.durable_sub = null
        host.dispatch_queue {
          host.router.delete(addresses, security_context)
        }
      }
      session_state.strategy.destroy {
        ack_connect
      }
    }

  }

  def detach:Unit = {
    queue.assertExecuting()

    if(!producerRoutes.isEmpty) {
      import collection.JavaConversions._
      val routes = producerRoutes.values.toSeq.toArray
      host.dispatch_queue {
        routes.foreach { route=>
          host.router.disconnect(Array(route.address), route)
        }
      }
      producerRoutes.clear
    }

    if( clean_session ) {
      if(!mqtt_consumer.addresses.isEmpty) {
        var addresses = mqtt_consumer.addresses.keySet.toArray
        host.dispatch_queue {
          host.router.unbind(addresses, mqtt_consumer, false , security_context)
        }
        mqtt_consumer.addresses.clear()
      }
      session_state.subscriptions.clear()
    } else {
      if(session_state.durable_sub!=null) {
        var addresses = Array(session_state.durable_sub)
        host.dispatch_queue {
          host.router.unbind(addresses, mqtt_consumer, false , security_context)
        }
        mqtt_consumer.addresses.clear()
        session_state.durable_sub = null
      }
    }

    in_flight_publishes.values.foreach { request =>
      if( request.ack!=null ) {
        if(request.delivered) {
          request.ack(Delivered)
        } else {
          request.ack(Undelivered)
        }
      }
    }
    in_flight_publishes.clear()
    
    handler.get.sink_manager.close(mqtt_consumer.consumer_sink.downstream.get, (request)=>{})
    mqtt_consumer.consumer_sink.downstream = None

    handler.get.on_transport_disconnected()
  }

  def decode_destination(value:UTF8Buffer):SimpleAddress = {
    val rc = destination_parser.decode_single_destination(value.toString, (name)=>{
      SimpleAddress("topic", destination_parser.decode_path(name))
    })
    if( rc==null ) {
      handler.foreach(_.die("Invalid mqtt destination name: "+value))
    }
    rc
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits that deal with assigning message ids to QoS > 0 requests
  // and tracking those requests so that they can get replayed on a
  // reconnect.
  //
  /////////////////////////////////////////////////////////////////////

  var in_flight_publishes = HashMap[Short, Request]()

  def send(message: MessageSupport.Message): Unit = {
    queue.assertExecuting()
    handler.foreach(_.connection_sink.offer(Request(0, message, null)))
  }

  def publish_completed(id: Short): Unit = {
    queue.assertExecuting()
    in_flight_publishes.remove(id) match {
      case Some(request) =>
        if ( request.ack != null ) {
          request.ack(Consumed)
        }
      case None =>
        // It's possible that on a reconnect, we get an ACK
        // in for message that was not dispatched yet. store
        // a place holder so we ack it upon the dispatch 
        // attempt.
        in_flight_publishes.put(id, Request(id, null, null))
    }
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits that deal with processing new messages from the client.
  //
  /////////////////////////////////////////////////////////////////////
  def on_transport_command(command:AnyRef):Unit = command match {
    case frame:MQTTFrame=>
      
      var command = frame
      protocol_filters.foreach { filter =>
        command = filter.filter(command)
      }

      command.messageType() match {

        case PUBLISH.TYPE =>
          on_mqtt_publish(received(new PUBLISH().decode(command)))

        // This follows a Publish with QoS EXACTLY_ONCE
        case PUBREL.TYPE =>
          var ack = received(new PUBREL().decode(command))
          // TODO: perhaps persist the processed list.. otherwise
          // we can't filter out dups after a broker restart.
          session_state.received_message_ids.remove(ack.messageId)
          session_state.strategy.update {
            send(new PUBCOMP().messageId(ack.messageId))
          }

        case SUBSCRIBE.TYPE =>
          on_mqtt_subscribe(received(new SUBSCRIBE().decode(command)))

        case UNSUBSCRIBE.TYPE =>
          on_mqtt_unsubscribe(received(new UNSUBSCRIBE().decode(command)))

        // AT_LEAST_ONCE ack flow for a client subscription
        case PUBACK.TYPE =>
          val ack = received(new PUBACK().decode(command))
          publish_completed(ack.messageId)

        // EXACTLY_ONCE ack flow for a client subscription
        case PUBREC.TYPE =>
          val ack = received(new PUBREC().decode(command))
          send(new PUBREL().messageId(ack.messageId))

        case PUBCOMP.TYPE =>
          val ack: PUBCOMP = received(new PUBCOMP().decode(command))
          publish_completed(ack.messageId)

        case PINGREQ.TYPE =>
          received(new PINGREQ().decode(command))
          send(new PINGRESP())

        case DISCONNECT.TYPE =>
          received(new DISCONNECT())
          MqttSessionManager.disconnect(host_state, client_id, handler.get)

        case _ =>
          handler.get.die("Invalid MQTT message type: "+command.messageType());
      }
    case "failure" =>
      // Publish the client's will
      publish_will {
        // then disconnect him.
        MqttSessionManager.disconnect(host_state, client_id, handler.get)
      }

    case _=>
      handler.get.die("Internal Server Error: unexpected mqtt command: "+command.getClass);
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Bits that deal with processing PUBLISH messages
  //
  /////////////////////////////////////////////////////////////////////
  var producerRoutes = new LRUCache[UTF8Buffer, MqttProducerRoute](10) {
    override def onCacheEviction(eldest: Entry[UTF8Buffer, MqttProducerRoute]) = {
      host.router.disconnect(Array(eldest.getValue.address), eldest.getValue)
    }
  }
  case class MqttProducerRoute(address:SimpleAddress, handler:MqttProtocolHandler) extends DeliveryProducerRoute(host.router) {
    override def send_buffer_size = handler.codec.getReadBufferSize
    override def connection = Some(handler.connection)
    override def dispatch_queue = queue
    refiller = ^{
      handler.resume_read
    }
  }

  def on_mqtt_publish(publish:PUBLISH):Unit = {

    if( (publish.qos eq EXACTLY_ONCE) && session_state.received_message_ids.contains(publish.messageId)) {
      val response = new PUBREC
      response.messageId(publish.messageId)
      send(response)
      return
    }

    handler.get.messages_received += 1

    queue.assertExecuting()
    producerRoutes.get(publish.topicName()) match {
      case null =>
        // create the producer route...

        val destination = decode_destination(publish.topicName())
        val route = MqttProducerRoute(destination, handler.get)

        // don't process commands until producer is connected...
        route.handler.suspend_read("route publish lookup")
        host.dispatch_queue {
          host.router.connect(Array(destination), route, security_context)
          queue {
            // We don't care if we are not allowed to send..
            if (!route.handler.connection.stopped) {
              route.handler.resume_read
              producerRoutes.put(publish.topicName(), route)
              send_via_route(route, publish)
            }
          }
        }

      case route =>
        // we can re-use the existing producer route
        send_via_route(route, publish)
    }
  }

  def send_via_route(route:DeliveryProducerRoute, publish:PUBLISH):Unit = {
    queue.assertExecuting()

    def at_least_once_ack(r:DeliveryResult, uow:StoreUOW):Unit = queue {
      val response = new PUBACK
      response.messageId(publish.messageId)
      send(response)
    }

    def exactly_once_ack(r:DeliveryResult, uow:StoreUOW):Unit = queue {
      queue.assertExecuting()
      // TODO: perhaps persist the processed list..
      session_state.received_message_ids.add(publish.messageId)
      session_state.strategy.update {
        val response = new PUBREC
        response.messageId(publish.messageId)
        send(response)
      }
    }

    val ack = publish.qos match {
      case AT_LEAST_ONCE => at_least_once_ack _
      case EXACTLY_ONCE => exactly_once_ack _
      case AT_MOST_ONCE => null
    }

    if( !route.targets.isEmpty ) {
      val delivery = new Delivery
      val persistent = publish.qos().ordinal() > 0
      delivery.message = MqttMessage(persistent, publish.payload)
      delivery.size = publish.payload.length
      delivery.ack = ack
      if( publish.retain() ) {
        if( delivery.size == 0 ) {
          delivery.retain = RetainRemove
        } else {
          delivery.retain = RetainSet
        }
      }

      // routes can always accept at least 1 delivery...
      assert( !route.full )
      route.offer(delivery)
      if( route.full ) {
        // but once it gets full.. suspend to flow control the producer.
        handler.get.suspend_read("blocked sending to: "+route.overflowSessions.mkString(", "))
      }

    } else {
      ack(null, null)
    }
  }

  
  //
  def publish_will(complete_close: =>Unit) = {
    if(connect_message!=null) {
      if( connect_message.willTopic()==null ) {
        complete_close
      } else {
  
        val destination = decode_destination(connect_message.willTopic())
        val prodcuer = new DeliveryProducerRoute(host.router) {
          override def send_buffer_size = 1024*64
          override def connection = handler.map(_.connection)
          override def dispatch_queue = queue
          refiller = NOOP
        }
  
        host.dispatch_queue {
          host.router.connect(Array(destination), prodcuer, security_context)
          queue {
            if(prodcuer.targets.isEmpty) {
              complete_close
            } else {
              val delivery = new Delivery
              val persistent = connect_message.willQos().ordinal() > 0
              delivery.message = MqttMessage(persistent, connect_message.willMessage())
              delivery.size = connect_message.willMessage().length
              if( connect_message.willRetain() ) {
                if( delivery.size == 0 ) {
                  delivery.retain = RetainRemove
                } else {
                  delivery.retain = RetainSet
                }
              }
  
              delivery.ack = (x,y) => {
                host.dispatch_queue {
                  host.router.disconnect(Array(destination), prodcuer)
                }
                complete_close
              }
              handler.get.messages_received += 1
              prodcuer.offer(delivery)
            }
          }
        }
      }
    }
  }
  /////////////////////////////////////////////////////////////////////
  //
  // Bits that deal with subscriptions
  //
  /////////////////////////////////////////////////////////////////////
  
  def on_mqtt_subscribe(sub:SUBSCRIBE):Unit = {
    subscribe(sub.topics()) {
      queue {
        session_state.strategy.update {
          val suback = new SUBACK
          suback.messageId(sub.messageId())
          suback.grantedQos(sub.topics().map(_.qos().ordinal().toByte))
          send(suback)
        }
      }
    }
  }
  
  def subscribe(topics:Traversable[Topic])(on_subscribed: => Unit):Unit = {
    var addresses:Array[_ <: BindAddress] = topics.toArray.map { topic =>
      var address:BindAddress = decode_destination(topic.name)
      session_state.subscriptions += topic.name -> (topic, address)
      mqtt_consumer.addresses += address -> topic.qos
      if(PathParser.containsWildCards(address.path)) {
        mqtt_consumer.wildcards.put( address.path, topic.qos() )
      }
      address
    }

    handler.get.subscription_count = mqtt_consumer.addresses.size

    addresses = if( clean_session ) {
      addresses
    } else {
      session_state.durable_sub = SubscriptionAddress(Path(client_id.toString), null, mqtt_consumer.addresses.keySet.toArray)
      Array(session_state.durable_sub)
    }      

    host.dispatch_queue {
      addresses.foreach { address=>
        host.router.bind(Array[BindAddress](address), mqtt_consumer, security_context)
        // MQTT ignores subscribe failures.
      }
      on_subscribed
    }

  }

  def on_mqtt_unsubscribe(unsubscribe:UNSUBSCRIBE):Unit = {

    val addresses:Array[_ <: BindAddress] = unsubscribe.topics.flatMap { topic =>
      session_state.subscriptions.remove(topic).map { case (topic, address)=>
        mqtt_consumer.addresses.remove(address)
        if(PathParser.containsWildCards(address.path)) {
          mqtt_consumer.wildcards.remove(address.path, topic.qos)
        }
        address
      }
    }

    handler.get.subscription_count = mqtt_consumer.addresses.size

    if(!clean_session) {
      session_state.durable_sub = SubscriptionAddress(Path(client_id.toString), null, mqtt_consumer.addresses.keySet.toArray)
    }
    
    host.dispatch_queue {
      if(clean_session) {
        host.router.unbind(addresses, mqtt_consumer, false, security_context)
      } else {
        if( mqtt_consumer.addresses.isEmpty ) {
          host.router.unbind(Array(session_state.durable_sub), mqtt_consumer, true, security_context)
          session_state.durable_sub = null
        } else {
          host.router.bind(Array(session_state.durable_sub), mqtt_consumer, security_context)
        }
      }
      queue {
        session_state.strategy.update {
          val ack = new UNSUBACK
          ack.messageId(unsubscribe.messageId())
          send(ack)
        }
      }
    }

  }

  var publish_body = false

  lazy val mqtt_consumer = new MqttConsumer
  class MqttConsumer extends BaseRetained with DeliveryConsumer {
    
    override def toString = "mqtt client:"+client_id+" remote address: "+security_context.remote_address

    val addresses = HashMap[BindAddress, QoS]()
    val wildcards = new PathMap[QoS]()

    val credit_window_source = createSource(new EventAggregator[(Int, Int), (Int, Int)] {
      def mergeEvent(previous:(Int, Int), event:(Int, Int)) = {
        if( previous == null ) {
          event
        } else {
          (previous._1+event._1, previous._2+event._2)
        }
      }
      def mergeEvents(previous:(Int, Int), events:(Int, Int)) = mergeEvent(previous, events)
    }, dispatch_queue)

    credit_window_source.setEventHandler(^{
      val data = credit_window_source.getData
      credit_window_filter.credit(data._1, data._2)
    });
    credit_window_source.resume

    val consumer_sink = new MutableSink[Request]()
    consumer_sink.downstream = None

    var next_seq_id = 1L
    def get_next_seq_id = {
      val rc = next_seq_id
      next_seq_id += 1
      rc
    }

    def to_message_id(value:Long):Short = (
        0x8000 | // MQTT message ids cannot be zero, so we always set the highest bit.
        (value & 0x7FFF) // the lower 15 bits come for the original seq id.
      ).toShort

    val credit_window_filter = new CreditWindowFilter[Delivery](consumer_sink.flatMap{ delivery =>
      queue.assertExecuting()
      
      // Look up which QoS we need to send this message with..
      var topic = delivery.sender.simple
      import collection.JavaConversions._
      addresses.get(topic).orElse(wildcards.get(topic.path).headOption) match {
          
        case None =>
          // draining messages after an un-subscribe
          acked(delivery, Consumed)
          None
          
        case Some(qos) =>

          // Convert the Delivery into a Request
          var publish = new PUBLISH
          publish.topicName(new UTF8Buffer(destination_parser.encode_destination(Array(delivery.sender))))
          if( delivery.redeliveries > 0) {
            publish.dup(true)
          }

          if( delivery.message.protocol eq MqttProtocol ) {
            publish.payload(delivery.message.asInstanceOf[MqttMessage].payload)
          } else {
            if( publish_body ) {
              publish.payload(delivery.message.getBodyAs(classOf[Buffer]))
            } else {
              publish.payload(delivery.message.encoded)
            }
          }

          handler.get.messages_sent += 1

          if (delivery.ack!=null && (qos ne AT_MOST_ONCE)) {
            publish.qos(qos)
            val id = to_message_id(if(clean_session) {
              get_next_seq_id // generate our own seq id.
            } else {
              delivery.seq // use the durable sub's seq id..
            })

            publish.messageId(id)
            val request = Request(id, publish, (result)=>{acked(delivery, result)})
            in_flight_publishes.put(id, request) match {
              case Some(r) =>
                // A reconnecting client could have acked before
                // we get dispatched by the durable sub.
                if( r.message == null ) {
                  in_flight_publishes.remove(id)
                  acked(delivery, Consumed)
                } else {
                  // Looks we sent out a msg with that id.  This could only
                  // happen once we send out 0x7FFF message and the first
                  // one has not been acked.
                  handler.foreach(_.async_die("Client not acking regularly.", null))
                }
              case None =>
            }
            
            Some(request)

          } else {
            // This callback gets executed once the message
            // sent to the transport.
            publish.qos(AT_MOST_ONCE)
            Some(Request(0, publish, (result)=>{ acked(delivery, result) }))
          }
      }
      
    }, Delivery)

    def acked(delivery:Delivery, result:DeliveryResult) = {
      queue.assertExecuting()
      credit_window_source.merge((delivery.size, 1))
      if( delivery.ack!=null ) {
        delivery.ack(result, null)
      }
    }
    
    credit_window_filter.credit(handler.get.codec.getWriteBufferSize*2, 1)

    val session_manager = new SessionSinkMux[Delivery](credit_window_filter, queue, Delivery) {
      override def time_stamp = host.broker.now
    }

    override def dispose() = queue {
      super.dispose()
    }

    def dispatch_queue = queue
    override def connection = handler.map(_.connection)
    override def receive_buffer_size = 1024*64; // handler.codec.getWriteBufferSize
    def is_persistent = false
    def matches(delivery:Delivery):Boolean = true

    //
    // Each destination we subscribe to will establish a session with us.
    //
    class MqttConsumerSession(val producer:DeliveryProducer) extends DeliverySession with SessionSinkFilter[Delivery] {
      producer.dispatch_queue.assertExecuting()
      retain

      val downstream = session_manager.open(producer.dispatch_queue, receive_buffer_size)

      override def toString = "connection to "+handler.map(_.connection.transport.getRemoteAddress).getOrElse("unconnected")

      def consumer = mqtt_consumer
      var closed = false

      def close = {
        assert(producer.dispatch_queue.isExecuting)
        if( !closed ) {
          closed = true
          dispose
        }
      }

      def dispose = {
        session_manager.close(downstream, (delivery)=>{
          // We have been closed so we have to nak any deliveries.
          if( delivery.ack!=null ) {
            delivery.ack(Undelivered, delivery.uow)
          }
        })
        release
      }

      // Delegate all the flow control stuff to the session
      override def full = {
        val rc = super.full
        rc
      }

      def offer(delivery:Delivery) = {
        if( full ) {
          false
        } else {
          delivery.message.retain()
          val rc = downstream.offer(delivery)
          assert(rc, "offer should be accepted since it was not full")
          true
        }
      }

    }
    def connect(p:DeliveryProducer) = new MqttConsumerSession(p)
  }

}
