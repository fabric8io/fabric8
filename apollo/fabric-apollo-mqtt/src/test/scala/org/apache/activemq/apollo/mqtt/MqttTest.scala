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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import java.lang.String
import org.fusesource.hawtdispatch._
import org.fusesource.hawtbuf.Buffer._
import java.util.concurrent.TimeUnit._
import java.net.InetSocketAddress
import org.apache.activemq.apollo.broker._
import org.apache.activemq.apollo.util._
import org.fusesource.mqtt.client._
import QoS._
import org.apache.activemq.apollo.dto.TopicStatusDTO
import java.util.concurrent.TimeUnit._
import org.fusesource.hawtbuf.UTF8Buffer

class MqttTestSupport extends FunSuiteSupport with ShouldMatchers with BeforeAndAfterEach with Logging {
  var broker: Broker = null
  var port = 0

  val broker_config_uri = "xml:classpath:apollo-mqtt.xml"

  override protected def beforeAll() = {
    try {
      info("Loading broker configuration from the classpath with URI: " + broker_config_uri)
      broker = BrokerFactory.createBroker(broker_config_uri)
      ServiceControl.start(broker, "Starting broker")
      port = broker.get_socket_address.asInstanceOf[InetSocketAddress].getPort
    }
    catch {
      case e:Throwable => e.printStackTrace
    }
  }

  override protected def afterAll() = {
    broker.stop
  }

  override protected def afterEach() = {
    super.afterEach
    clients.foreach(_.close)
    clients = Nil
    client = new MqttClient
  }

  def queue_exists(name:String):Boolean = {
    val host = broker.virtual_hosts.get(ascii("default")).get
    host.dispatch_queue.future {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_queue_domain.destination_by_id.get(name).isDefined
    }.await()
  }

  def topic_exists(name:String):Boolean = {
    val host = broker.virtual_hosts.get(ascii("default")).get
    host.dispatch_queue.future {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_topic_domain.destination_by_id.get(name).isDefined
    }.await()
  }

  def topic_status(name:String):TopicStatusDTO = {
    val host = broker.virtual_hosts.get(ascii("default")).get
    sync(host) {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_topic_domain.destination_by_id.get(name).get.status
    }
  }
  
  class MqttClient extends MQTT {

    var connection: BlockingConnection = _

    def open(host: String, port: Int) = {
      setHost(host, port)
      connection = blockingConnection();
      connection.connect();
    }
  
    def close() = {
      connection.disconnect()
    }
  }
  
  var client = new MqttClient
  var clients = List[MqttClient]()

  def connect(c:MqttClient=client) = {
    c.open("localhost", port)
  }
  
  def publish(topic:String, message:String, qos:QoS=AT_MOST_ONCE, retain:Boolean=false, c:MqttClient=client) = {
    c.connection.publish(topic, message.getBytes("UTF-8"), qos, retain)
  }
  def subscribe(topic:String, qos:QoS=AT_MOST_ONCE, c:MqttClient=client) = {
    c.connection.subscribe(Array(new org.fusesource.mqtt.client.Topic(topic, qos)))
  }

}

class MqttDestinationTest extends MqttTestSupport {
  
  test("Publish") {
    connect()
    publish("test", "message", EXACTLY_ONCE)
    topic_status("test").metrics.enqueue_item_counter should be(1)

    publish("test", "message", AT_LEAST_ONCE)
    topic_status("test").metrics.enqueue_item_counter should be(2)

    publish("test", "message", AT_MOST_ONCE)

    within(1, SECONDS) { // since AT_MOST_ONCE use non-blocking sends.
      topic_status("test").metrics.enqueue_item_counter should be(3)
    }
  }

  def get_session_count = {
    (MqttSessionManager.queue.future {
      MqttSessionManager.sessions.size
    }).await()
  }
  
  def find_session(id:String) = {
    val t = new UTF8Buffer(id)
    (MqttSessionManager.queue.future {
      MqttSessionManager.sessions.find(_._1.client_id == t).map(_._2)
    }).await()
  }
  
  test("Subscribe") {
    get_session_count should be(0)
    client.setClientId("c#1");
    connect()
    get_session_count should be(1)

    subscribe("foo")
    publish("foo", "#1", EXACTLY_ONCE)
    val msg = client.connection.receive();
    msg.getTopic should equal("foo")
    msg.getPayload should equal ("#1".getBytes("UTF-8"))
    msg.ack()
    client.close()

    get_session_count should be(0)
  }

}
//
//class MqttConnectionTest extends MqttTestSupport {
//
//  test("MQTT CONNECT") {
//    client.open("localhost", port)
//  }
//
//  test("MQTT Broker times out idle connection") {
//
//    val queue = createQueue("test")
//
//    client.setKeepAlive(1)
//    client.setDispatchQueue(queue)
//    client.setReconnectAttemptsMax(0)
//    client.setDispatchQueue(queue);
//    client.open("localhost", port)
//
//    client.connection.isConnected should be(true)
//    queue.suspend() // this will cause the client to hang
//    Thread.sleep(1000*2);
//    queue.resume()
//    within(1, SECONDS) {
//      client.connection.isConnected should be(false)
//    }
//  }
//
//}
