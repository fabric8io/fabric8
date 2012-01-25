/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.apollo.mqtt

import org.apache.activemq.apollo.broker._
import java.lang.String
import protocol.{ProtocolCodecFactory, ProtocolFactory, Protocol}
import org.apache.activemq.apollo.broker.store._
import org.fusesource.mqtt.codec.MQTTProtocolCodec
import org.fusesource.hawtbuf._
import org.fusesource.hawtbuf.Buffer._

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class MqttProtocolFactory extends ProtocolFactory {
  def create() = MqttProtocol
  def create(config: String) = if(config == "mqtt") {
    MqttProtocol
  } else {
    null
  }
}

/**
 * Creates MqttCodec objects that encode/decode the
 * <a href="http://activemq.apache.org/mqtt/">Mqtt</a> protocol.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class MqttProtocolCodecFactory extends ProtocolCodecFactory.Provider {

  def id = "mqtt"
  def createProtocolCodec() = new MQTTProtocolCodec();
  def isIdentifiable() = true

  //
  // An MQTT CONNECT message has between 10-13 bytes:
  //    Message Type     : 0x10 @ [0]
  //    Remaining Length : Byte{1-4} @ [1]
  //    Protocol Name    : 0x00 0x06 'M' 'Q' 'I' 's' 'd' 'p' @ [2|3|4|5]
  //
  val HEAD_MAGIC = new Buffer(Array[Byte](0x10 ))
  val TAIL_MAGIC = new Buffer(Array[Byte](0x00, 0x06, 'M', 'Q', 'I', 's', 'd', 'p'))

  def maxIdentificaionLength() = 13;
  def matchesIdentification(header: Buffer):Boolean = {
    if (header.length < 10) {
      false
    } else {
      header.startsWith(HEAD_MAGIC) && header.indexOf(TAIL_MAGIC, 2) < 6
    }
  }
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object MqttProtocol extends MqttProtocolCodecFactory with Protocol {

  val destination_parser = new DestinationParser
  destination_parser.queue_prefix = null
  destination_parser.topic_prefix = null
  destination_parser.path_separator = "/"
  destination_parser.any_child_wildcard = "+"
  destination_parser.any_descendant_wildcard = "#"
  destination_parser.dsub_prefix = null
  destination_parser.temp_queue_prefix = null
  destination_parser.temp_topic_prefix = null
  destination_parser.destination_separator = null
  destination_parser.regex_wildcard_end = null
  destination_parser.regex_wildcard_end = null

  val PROTOCOL_ID = ascii(id)
  def createProtocolHandler = new MqttProtocolHandler

  def encode(message: Message):MessageRecord = {
    message match {
      case message:MqttMessage =>
        val rc = new MessageRecord
        rc.protocol = PROTOCOL_ID
        rc.buffer = message.payload
        rc
      case _ => throw new RuntimeException("Invalid message type");
    }
  }

  def decode(message: MessageRecord) = {
    MqttMessage(false, message.buffer)
  }

}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class MqttMessage(

  /**
   * This depends on the QoS the PUBLISH is sent with. 
   */
  persistent:Boolean,

  /**
   * The payload in the PUBLISH frame.
   */
  payload:Buffer) extends Message {
  
  def protocol = MqttProtocol

  /**
   * MQTT does not support specifying a priority.
   */
  def priority:Byte = 4;

  /**
   * MQTT does not support specifying an expiration.
   */
  def expiration: Long = 0;

  def getBodyAs[T](toType : Class[T]) = {
    payload.asInstanceOf[T]
  }

  def getLocalConnectionId = null

  def getProperty(name: String):AnyRef = null

  def setDisposer(disposer: Runnable) = throw new UnsupportedOperationException
  def retained = throw new UnsupportedOperationException
  def retain = {}
  def release = {}
}
