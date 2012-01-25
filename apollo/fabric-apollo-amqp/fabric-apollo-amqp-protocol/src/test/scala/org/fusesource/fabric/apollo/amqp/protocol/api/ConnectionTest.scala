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

package org.fusesource.fabric.apollo.amqp.protocol.api

import org.fusesource.hawtdispatch._
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import java.util.concurrent.{TimeUnit, CountDownLatch}
import collection.mutable.ListBuffer

/**
 *
 */
class ConnectionTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create server connection using pipe transport") {
    info("Starting %s", testName)

    val uri = "pipe://foobar/blah"

    val latch = new CountDownLatch(2)

    val server = AMQPConnectionFactory.createServerConnection(new ConnectionHandler {
      def connectionCreated(connection: Connection) {
        info("Created connection : %s", connection)
        connection.onDisconnected(^ {
          info("Connection %s closed", connection)
        })
        connection.onConnected(^ {
          connection.close
          latch.countDown
        })
      }
    })

    server.bind(uri, NOOP)
    val client = AMQPConnectionFactory.createConnection()
    client.onDisconnected(^ {
      latch.countDown
    })
    client.onConnected(^ {
      client.close
      latch.countDown
    })

    client.connect(uri)

    latch.await(10, TimeUnit.SECONDS) should be(true)
    server.unbind
  }

  test("Create connection, create session") {
    info("Starting %s\n", testName)

    val uri = "pipe://foobar2/blah"
    val count = 2
    val latch = new CountDownLatch(count)

    var sessions_started = 0
    var sessions_ended = 0

    val server = AMQPConnectionFactory.createServerConnection(new ConnectionHandler {
      def connectionCreated(connection: Connection) {
        info("Created new incoming connection\n")
        connection.onDisconnected(^ {
          info("Connection closed\n")
          latch.countDown
        })
        connection.setSessionHandler(new SessionHandler {
          def sessionCreated(session: Session) {
            session.setOnEnd(^{
              info("Server session ended\n")
              sessions_ended = sessions_ended + 1
              sessions_ended should be(1)
            })
            info("Session created for incoming client\n")
            session.begin(^{
              info("Server session started\n")
              sessions_started = sessions_started + 1
              sessions_started should be (1)
            })
          }

          def sessionReleased(session: Session) {
            info("Session released for incoming client\n")
            session.end
          }
        })
      }
    })

    server.bind(uri, NOOP)
    val client = AMQPConnectionFactory.createConnection()
    client.onDisconnected(^ {
      info("Disconnecting\n")
    })
    client.onConnected(^ {
      info("Created connection\n")
      val session = client.createSession
      session.setOnEnd(^{
        info("Session stopped\n")
        client.close
        latch.countDown
      })

      session.begin(^{
        info("Session started\n")
        session.end
      })
    })

    client.connect(uri)

    latch.await

    latch.await(10, TimeUnit.SECONDS)

    latch.getCount should be (0)
    sessions_ended should be (1)
    sessions_started should be (1)

    client.error() should be (null)
    server.unbind
  }

  test("Multiple sessions") {
    info("Starting %s\n", testName)

    val uri = "pipe://foobar2/blah"
    val max_sessions = 100
    val count = max_sessions + 1
    val latch = new CountDownLatch(count)

    val server = AMQPConnectionFactory.createServerConnection(new ConnectionHandler {
      def connectionCreated(connection: Connection) {
        info("Created new incoming connection\n")
        connection.onDisconnected(^ {
          info("Connection closed\n")
          latch.countDown
        })
        connection.setSessionHandler(new SessionHandler {
          def sessionCreated(session: Session) {
            session.setOnEnd(^{
              info("Server session ended\n")
            })
            info("Session created for incoming client\n")
            session.begin(^{
              info("Server session started\n")
            })
          }

          def sessionReleased(session: Session) {
            info("Session released for incoming client\n")
            session.end
          }
        })
      }
    })

    server.bind(uri, NOOP)

    var num = 0

    val client = AMQPConnectionFactory.createConnection()
    client.setSessionHandler(new SessionHandler {
      def sessionCreated(session: Session) {
        num = num + 1
      }

      def sessionReleased(session: Session) {
        num = num - 1
        if (num == 0) {
          client.close
        }
      }
    })
    client.onDisconnected(^ {
      info("Disconnecting\n")
      latch.countDown
    })
    client.onConnected(^ {
      info("Created connection\n")
      val sessions = new ListBuffer[Session]
      for (i <- 1 to 100 ) {
        val session = client.createSession
        session.setOnEnd(^{
          info("Session stopped\n")
          latch.countDown
        })
        sessions.append(session)
      }

      sessions.foreach((session) => {
        session.begin(^{
          info("Session started\n")
          session.end
        })

      })
    })

    client.connect(uri)

    latch.await(10, TimeUnit.SECONDS)

    latch.getCount should be (0)

    client.error() should be (null)
    server.unbind
  }

  test("Multiple sessions close with one call to close") {
    info("Starting %s\n", testName)

    val uri = "pipe://foobar2/blah"
    val max_sessions = 100
    val count = max_sessions + 1
    val latch = new CountDownLatch(count)

    val server = AMQPConnectionFactory.createServerConnection(new ConnectionHandler {
      def connectionCreated(connection: Connection) {
        info("Created new incoming connection\n")
        connection.onDisconnected(^ {
          info("Connection closed\n")
          latch.countDown
        })
        connection.setSessionHandler(new SessionHandler {
          def sessionCreated(session: Session) {
            session.setOnEnd(^{
              info("Server session ended\n")
            })
            info("Session created for incoming client\n")
            session.begin(^{
              info("Server session started\n")
            })
          }

          def sessionReleased(session: Session) {
            info("Session released for incoming client\n")
            session.end
          }
        })
      }
    })

    server.bind(uri, NOOP)

    var num = 0

    val client = AMQPConnectionFactory.createConnection()
    client.setSessionHandler(new SessionHandler {
      def sessionCreated(session: Session) {
        num = num + 1
        if (num == max_sessions) {
          client.close
        }
      }

      def sessionReleased(session: Session) {
        num = num - 1
      }
    })
    client.onDisconnected(^ {
      info("Disconnecting\n")
      latch.countDown
    })
    client.onConnected(^ {
      info("Created connection\n")
      val sessions = new ListBuffer[Session]
      for (i <- 1 to 100 ) {
        val session = client.createSession
        session.setOnEnd(^{
          info("Session stopped\n")
          latch.countDown
        })
        sessions.append(session)
      }

      sessions.foreach((session) => {
        session.begin(^{
          info("Session started\n")
        })

      })
    })

    client.connect(uri)

    latch.await(10, TimeUnit.SECONDS)

    latch.getCount should be (0)

    client.error() should be (null)
    server.unbind
  }
}