package org.fusesource.fabric.apollo.amqp.protocol.commands

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame

/**
 *
 */
object HeaderSent {
  val INSTANCE = new HeaderSent
}

class HeaderSent extends AMQPFrame {
  override def toString = getClass.getSimpleName
}