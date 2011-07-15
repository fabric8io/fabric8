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

import org.fusesource.fabric.apollo.amqp.api._
import org.apache.activemq.apollo.util.Logging
import scala.math._
import org.fusesource.hawtdispatch._

/**
 *
 */

class BaseTestServer extends Logging {
  val server = AmqpConnectionFactory.createServer(new ConnectionListener {
    def connectionCreated(connection: Connection) = {
      connection_created(connection)
    }
  })

  var connection_created = (c:Connection) => {
    info("Created connection %s", c)
    c.setSessionListener(new SessionListener {
      def sessionReleased(connection: Connection, session: Session) = session_released(connection, session)
      def sessionCreated(connection: Connection, session: Session) = session_created(connection, session)
    })
  }

  var session_released = (c:Connection, s:Session) => {
    info("%s released session %s", c, s)
  }

  var session_created = (c:Connection, s:Session) => {
    info("%s created session %s")
  }

  def bind(addr:String) = server.bind(addr)
  def getListenPort = server.getListenPort

  def getConnectionUri = "tcp://" + server.getListenHost + ":" + server.getListenPort
}

class DefaultLinkListener extends BaseTestServer {
  session_created = (c:Connection, s:Session) => {
    s.setLinkListener(new LinkListener {
      def receiverDetaching(session: Session, sender: Sender):Unit = receiver_detaching(session, sender)
      def senderDetaching(session: Session, receiver: Receiver):Unit = sender_detaching(session, receiver)
      def receiverAttaching(session: Session, sender: Sender):Unit = receiver_attaching(session, sender)
      def senderAttaching(session: Session, receiver: Receiver):Unit = sender_attaching(session, receiver)
    })
  }

  var receiver_detaching = (session:Session, sender:Sender) => info("%s detaching %s", session, sender)
  var sender_detaching = (session:Session, receiver:Receiver) => info("%s detaching %s", session, receiver)
  var receiver_attaching = (session:Session, sender:Sender) => info("%s attaching %s", session, sender)
  var sender_attaching = (session:Session, receiver:Receiver) => info("%s attaching %s", session, receiver)
}

class TestReceiver extends DefaultLinkListener {
  var enableFlowControl = true

  sender_attaching = (session:Session, receiver:Receiver) => {
    info("Attaching remote sender")
    receiver.setOnDetach(_onDetach)
    receiver.setListener(new MessageListener {
      def needLinkCredit(available: Long) = need_link_credit(available)
      def refiller(refiller: Runnable) = _refiller(refiller)
      def offer(receiver: Receiver, message: BareMessage) = _offer(receiver, message)
      def full = _full()
    })
  }

  var _onDetach:Runnable = ^{}

  var need_link_credit = (available:Long) => max(available, 1)
  var _refiller = (refiller:Runnable) => {}
  var _offer = (receiver:Receiver, message:BareMessage) => {
    info("%s received message %s", receiver.getName, message)
    true
  }
  var _full = () => false


}

