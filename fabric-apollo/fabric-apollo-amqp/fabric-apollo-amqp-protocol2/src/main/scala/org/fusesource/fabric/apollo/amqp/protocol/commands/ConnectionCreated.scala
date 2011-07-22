package org.fusesource.fabric.apollo.amqp.protocol.commands

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame

object ConnectionCreated {
  val INSTANCE = new ConnectionCreated
}
/**
 *
 */
class ConnectionCreated extends AMQPFrame {

  override def toString = getClass.getSimpleName

}