/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
