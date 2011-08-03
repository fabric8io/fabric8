/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.api

import org.fusesource.hawtdispatch._
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import java.util.concurrent.{TimeUnit, CountDownLatch}

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
    printf("Starting %s\n", testName)

    val uri = "pipe://foobar2/blah"
    val count = 2
    val latch = new CountDownLatch(count)

    var sessions_started = 0
    var sessions_ended = 0

    val server = AMQPConnectionFactory.createServerConnection(new ConnectionHandler {
      def connectionCreated(connection: Connection) {
        printf("Created new incoming connection\n")
        connection.onDisconnected(^ {
          printf("Connection closed\n")
          latch.countDown
        })
        connection.setSessionHandler(new SessionHandler {
          def sessionCreated(session: Session) {
            session.setOnEnd(^{
              printf("Server session ended\n")
              sessions_ended = sessions_ended + 1
              sessions_ended should be(1)
            })
            printf("Session created for incoming client\n")
            session.begin(^{
              printf("Server session started\n")
              sessions_started = sessions_started + 1
              sessions_started should be (1)
            })
          }

          def sessionReleased(session: Session) {
            printf("Session released for incoming client\n")
            session.end
          }
        })
      }
    })

    server.bind(uri, NOOP)
    val client = AMQPConnectionFactory.createConnection()
    client.onDisconnected(^ {
      printf("Disconnecting\n")
    })
    client.onConnected(^ {
      printf("Created connection\n")
      val session = client.createSession
      session.setOnEnd(^{
        printf("Session stopped\n")
        client.close
        latch.countDown
      })

      session.begin(^{
        printf("Session started\n")
        session.end
      })
    })

    client.connect(uri)

    latch.await()

    latch.await(10, TimeUnit.SECONDS)

    latch.getCount should be (0)

    client.error() should be (null)
    server.unbind
  }

}