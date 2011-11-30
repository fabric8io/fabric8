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

class LinkageTest extends FunSuiteSupport with ShouldMatchers with Logging {
  /*

  test("Create sender, set address, check address at receiver end") {

    val test_address = "queue:foo"
    val test_link_name = "foo"

    var received_address = ""
    var received_name = ""

    val server = new TestReceiver
    server.sender_attaching = (session:Session, receiver:Receiver) => {
      received_address = receiver.getAddress
      received_name = receiver.getName
      receiver.setListener(new MessageListener {
        def needLinkCredit(available: Long) = 0L

        def refiller(refiller: Runnable) {}

        def offer(receiver: Receiver, message: Message) = false

        def full() = false
      })
    }

    server.bind("tcp://localhost:0")

    val latch = new CountDownLatch(1)

    val connection = AmqpConnectionFactory.create
    connection.setOnClose(^{latch.countDown})

    connection.connect(server.getConnectionUri, ^{
      val session = connection.createSession
      session.begin(^{
        val sender = session.createSender
        sender.setName(test_link_name)
        sender.setAddress(test_address)
        sender.setOnDetach(^{
          session.end(^{
            connection.close
          })
        })

        sender.attach(^{
          sender.detach
        })

      })
    })

    latch.await(10, TimeUnit.SECONDS) should be (true)
    received_address should be (test_address)
    received_name should be (test_link_name)

  }
*/
}
