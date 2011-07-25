/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.apache.activemq.apollo.util.FunSuiteSupport
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import org.fusesource.fabric.apollo.amqp.codec.types.Begin
import org.fusesource.fabric.apollo.amqp.protocol.api.Session
import org.fusesource.fabric.apollo.amqp.protocol.api.SessionHandler
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.FailInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.TaskExecutingInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.TerminationInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.SessionFactory
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable.Queue

/**
 * 
 */
class MultiplexerTest extends FunSuiteSupport with ShouldMatchers with Logging {
  
  test("Create multiplexer, create some chains on the fly, remove a chain") {
    
    val multiplexer = new Multiplexer
    
    multiplexer.channel_selector = Option((frame:AMQPFrame) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getChannel          
        case _ =>
          throw new RuntimeException("Unexpected frame type")
      }
    })
    
    var instances = 0
    
    var saved:Interceptor = null
    
    multiplexer.interceptor_factory = Option((frame:AMQPFrame) => {
      instances = instances + 1      
      val rc = new FrameLoggingInterceptor("Chain instance " + instances)
      if (instances == 2) {
        saved = rc
      }
      rc.tail.incoming = new TerminationInterceptor
      rc
    })
    
    multiplexer.head.outgoing = new FrameLoggingInterceptor("Outgoing")
    multiplexer.head.outgoing = new TaskExecutingInterceptor    
    
    multiplexer.head.receive(new AMQPTransportFrame(0, new Begin()), new Queue[() => Unit])
    multiplexer.head.receive(new AMQPTransportFrame(1, new Begin()), new Queue[() => Unit])
    multiplexer.head.receive(new AMQPTransportFrame(2, new Begin()), new Queue[() => Unit])
    
    instances should be (3)
    
    // now remove one
    saved.tail.remove
    saved.tail.incoming = new FailInterceptor
    
    multiplexer.release(saved.head)
    multiplexer.head.receive(new AMQPTransportFrame(0, new Begin()), new Queue[() => Unit])
    multiplexer.head.receive(new AMQPTransportFrame(1, new Begin()), new Queue[() => Unit])
    multiplexer.head.receive(new AMQPTransportFrame(2, new Begin()), new Queue[() => Unit])
  }
  

}
