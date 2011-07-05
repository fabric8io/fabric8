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

import org.scalatest.matchers.ShouldMatchers
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import scala.math.max
import org.fusesource.fabric.apollo.amqp.api._
import java.util.concurrent.{TimeUnit, CountDownLatch}

/**
 *
 */

class FilterTest extends BrokerTestSupport with ShouldMatchers {

  // This is failing..
  ignore("Connect sender and receiver that has some filter") {
    val latch = new CountDownLatch(1)
    val connection_latch = new CountDownLatch(1)

    val connection = AmqpConnectionFactory.create
    connection.setOnClose(^{
      connection_latch.countDown
    })

    connection.connect(getConnectionUri, ^{

      val session = connection.createSession
      session.begin(^{

        val receiver = session.createReceiver
        val sender = session.createSender

        val address = "queue:test"
        receiver.setAddress(address)
        sender.setAddress(address)

        val filter_set = createAmqpFilterSet
        val filter1 = createAmqpFilter
        val filter2 = createAmqpFilter

        filter1.setPredicate(createAmqpString("a='1'"))
        filter2.setPredicate(createAmqpString("color='red'"))
        filter_set.put(createAmqpSymbol("One"), filter1);
        filter_set.put(createAmqpSymbol("Two"), filter2);

        receiver.setFilter(filter_set)

        receiver.setListener(new MessageListener {
          def needLinkCredit(available: Long) = max(available, 1)

          def refiller(refiller: Runnable) {}

          def offer(receiver: Receiver, message: Message) = {
            println("Got : " + message)
            if (!message.getSettled) {
              receiver.settle(message, Outcome.ACCEPTED)
            }
            latch.countDown
            true
          }

          def full() = false
        })

        receiver.attach(^{})

        sender.setOnDetach(^{
          session.end(^{
            connection.close
          })
        })

        sender.attach(^{

          val message1 = sender.getSession.createMessage
          val attrs = createAmqpMessageAttributes
          attrs.put(createAmqpSymbol("a"), createAmqpString("1"))
          attrs.put(createAmqpSymbol("color"), createAmqpString("red"))
          message1.getHeader.setMessageAttrs(attrs)
          message1.onAck(^{
            sender.detach
          })

          val message2 = sender.getSession.createMessage
          sender.put(message2)
          sender.put(message1)

        })
      })
    })

    latch.await(10, TimeUnit.SECONDS) should be(true)
    connection_latch.await(10, TimeUnit.SECONDS) should be (true)

  }


}
