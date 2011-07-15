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

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
/**
 *
 */

class IncomingLinkBufferTest extends FunSuiteSupport with ShouldMatchers with Logging {
  /*

  test("Buffer messages at incoming link") {

    val server = new TestReceiver
    val queue = Dispatch.createQueue

    var received = 0

    var sleeping = false
    var _refiller = ^{ }

    server._full = () => {
      sleeping
    }
    server._refiller = (refiller) => {
      _refiller = refiller
    }
    server._offer = (receiver, message) => {
      if (sleeping) {
        false
      } else {
        def settle = {
          if (!message.getSettled) {
            receiver.settle(message, Outcome.ACCEPTED)
          }
          received = received + 1
        }

        sleeping = true
        debug("received %s", message)
        queue.after(200, TimeUnit.MILLISECONDS) {
          sleeping = false
          settle
          _refiller.run
        }

        true
      }
    }

    server.bind("tcp://localhost:0")

    val latch = new CountDownLatch(1)
    val max = 50

    val connection = AmqpConnectionFactory.create
    connection.setOnClose(^{latch.countDown})
    connection.connect(server.getConnectionUri, ^{
      val session = connection.createSession
      session.begin(^{
        val sender = session.createSender
        sender.setOnDetach(^{
          session.end(^{
            connection.close
          })
        })
        sender.attach(^{
          (1 to max).foreach((x) => {
            val message = sender.getSession.createMessage
            message.setSettled(false)
            if (x >= max) {
              message.onAck(^{
                sender.detach
              })
            }
            sender.put(message)
          })
        })
      })
    })

    latch.await(max * 2, TimeUnit.SECONDS) should be (true)
    received should be (max)

  }
*/
}
