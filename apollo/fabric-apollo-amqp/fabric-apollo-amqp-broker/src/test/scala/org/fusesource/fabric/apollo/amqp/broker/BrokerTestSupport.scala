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

package org.fusesource.fabric.apollo.amqp.broker

import org.apache.activemq.apollo.broker.Broker
import org.apache.activemq.apollo.broker.BrokerFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{FunSuiteSupport, Logging, ServiceControl}
import java.net.InetSocketAddress

/**
 *
 */
trait BrokerTestSupport extends FunSuiteSupport with BeforeAndAfterEach with Logging {

  def startBroker: Unit = {
    debug("Starting broker")
    broker = BrokerFactory.createBroker(brokerConfigUri)
    ServiceControl.start(broker, "Starting broker")
    port = broker.get_socket_address.asInstanceOf[InetSocketAddress].getPort
  }

  protected var host: String = "localhost"
  protected var broker: Broker = null

  def stopBroker: Unit = {
    debug("Stopping broker")
    ServiceControl.stop(broker, "Stopping broker")
  }

  def getConnectionUri = "tcp://" + host + ":" + port

  protected var brokerConfigUri: String = "xml:classpath:fusemq-amqp.xml"
  protected var port: Int = 5672

  override protected def beforeEach = startBroker
  override protected def afterEach = stopBroker
}
