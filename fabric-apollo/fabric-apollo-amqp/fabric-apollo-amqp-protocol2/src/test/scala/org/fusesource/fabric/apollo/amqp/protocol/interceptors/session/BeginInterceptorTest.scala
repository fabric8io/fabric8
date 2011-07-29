/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{Begin, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.{TaskExecutingInterceptor, TestSendInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks

/**
 *
 */

class BeginInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Begin a session locally") {

    val begin = new BeginInterceptor

    begin.head.outgoing = new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getPerformative match {
            case b:Begin =>
              Option(b.getRemoteChannel) match {
            case Some(c) =>
              fail("Remote channel shouldn't be set")
            case None =>
              }
          }
      }
    })

    begin.head.outgoing = new TaskExecutingInterceptor

    begin.send_begin
  }

  test("Begin a session remotely") {

    val begin = new BeginInterceptor

    begin.head.outgoing = new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getPerformative match {
            case b:Begin =>
              Option(b.getRemoteChannel) match {
                case Some(c) =>
                  c should be (7)
                case None =>
                  fail("Remote channel should be set")
              }
          }
      }
    })

    begin.head.outgoing = new TaskExecutingInterceptor

    begin.head.receive(new AMQPTransportFrame(7, new Begin), Tasks())
    begin.send_begin
  }

}