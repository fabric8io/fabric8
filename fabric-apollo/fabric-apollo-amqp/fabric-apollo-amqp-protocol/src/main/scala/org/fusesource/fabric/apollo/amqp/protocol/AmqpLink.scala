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

import protocol._
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.util.Logging
import java.util._
import org.fusesource.fabric.apollo.amqp.api._
import Role.SENDER
import Role.RECEIVER
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType

/**
 *
 */
abstract class AmqpLink(val session:LinkSession) extends Link with Logging {

  var name:String = UUID.randomUUID.toString

  var remotely_created = false

  var handle: Option[Short] = None
  var remoteHandle: Option[Short] = None

  var onAttach:Option[Runnable] = None
  var onDetach:Option[Runnable] = None

  var source = new Source
  var target = new Target

  source.setDistributionMode(StdDistMode.MOVE)

  override def toString: String = {
    return getClass.getSimpleName + "{name=" + name + " handle=" + handle + " remoteHandle=" + remoteHandle + " flowState=" + flowstate + "}"
  }

  def getSession = session.asInstanceOf[Session]

  def established: Boolean = handle != None && remoteHandle != None

  def attach(task:Runnable) = {
    onAttach = Option(task)
    attach
  }

  def setOnDetach(task:Runnable) = {
    onDetach = Option(task)
  }

  def attach:Unit = {
    handle match {
      case Some(handle) =>
      case None =>
        session.attach(this)
        val attach = new Attach
        attach.setInitialDeliveryCount(0L)
        attach.setHandle(handle.get)
        attach.setName(name)
        attach.setRole(role.getValue)
        attach.setSource(source)
        attach.setTarget(target)
        session.send(this, attach)
    }
  }

  def attach(attach: Attach): Unit = {
    remoteHandle = Some(attach.getHandle.shortValue)
    handle match {
      case None =>
        this.attach
      case Some(handle) =>
    }
    if (established) {
      onAttach.foreach((x) => session.dispatch_queue << x)
    }
  }

  def send_updated_flow_state(flowstate:Flow): Unit = {
    handle.foreach((x) => {
      flowstate.setHandle(x)
      //trace("Updating peer with flow state : %s", flowstate)
      session.send(this, flowstate)
    })
  }

  def detach: Unit = detach(None)
  def detach(description:String): Unit = detach(LinkError.DETACH_FORCED, description)
  def detach(t:Throwable):Unit = detach(LinkError.DETACH_FORCED, t)
  def detach(l:LinkError, d:String):Unit = detach(l.getValue.ascii.toString, d)
  def detach(l:LinkError, t:Throwable):Unit = detach(l.getValue.ascii.toString, t)

  def detach(condition:String, description:String):Unit = {
    val error = new Error
    error.setCondition(condition)
    error.setDescription(description)
    detach(Option(error))
  }

  def detach(condition:String, t:Throwable):Unit = {
    val error = new Error
    error.setCondition(condition)
    error.setDescription(t.toString + "\n" + t.getStackTraceString)
    detach(Option(error))
  }

  def detach(error:Option[Error]):Unit = {
    if (established) {
      val d = new Detach
      d.setHandle(handle.get)
      handle = None
      error match {
        case Some(e) =>
          warn("Detaching link due to error %s", e)
          d.setError(e)
          session.send(this, d)
        case None =>
          info("Detaching link")
          session.send(this, d)
      }
      onDetach match {
        case Some(r) =>
          onDetach = Option(^{
            session.detach(this)
            r.run
          })
        case None =>
          onDetach = Option(^{
            session.detach(this)
          })
      }
    }
  }

  def detach(detach:Detach) : Unit = {
    remoteHandle = None
    val response = new Detach
    handle.foreach((h) => {
      response.setHandle(h)
      session.detach(this)
      handle = None
      session.send(this, response)
    })
    onDetach.foreach((x) => session.dispatch_queue << x)
  }

  def flowstate = new Flow

  def isFlowControlEnabled():Boolean

  def setName(name:String) = this.name = name
  def getName:String = this.name

  def address = {
    role match {
      case SENDER =>
        if (remotely_created) {
          source.getAddress.asInstanceOf[AMQPString].getValue
        } else {
          target.getAddress.asInstanceOf[AMQPString].getValue
        }
      case RECEIVER =>
        if (remotely_created) {
          target.getAddress.asInstanceOf[AMQPString].getValue
        } else {
          source.getAddress.asInstanceOf[AMQPString].getValue
        }
    }
  }

  def address_=(address:String) = {
    // TODO - maybe not the best place to set this
    if (address.startsWith("queue")) {
      setDistributionMode(StdDistMode.MOVE)
    } else if (address.startsWith("topic")) {
      setDistributionMode(StdDistMode.COPY)
    }

    role match {
      case SENDER =>
        if (remotely_created) {
          source.setAddress(new AMQPString(address))
        } else {
          target.setAddress(new AMQPString(address))
        }
      case RECEIVER =>
        if (remotely_created) {
          target.setAddress(new AMQPString(address))
        } else {
          source.setAddress(new AMQPString(address))
        }
    }
  }

  def setAddress(address:String) = this.address = address
  def getAddress = this.address

  def getSourceTimeout:Long = source.getTimeout
  def setSourceTimeout(timeout: Long) = source.setTimeout(timeout)

  def getTargetTimeout:Long = target.getTimeout
  def setTargetTimeout(timeout: Long) = target.setTimeout(timeout)

  def getSourceExpiryPolicy:Buffer = source.getExpiryPolicy
  def setSourceExpiryPolicy(policy: Buffer) = source.setExpiryPolicy(policy)

  def getTargetExpiryPolicy:Buffer = target.getExpiryPolicy
  def setTargetExpiryPolicy(policy: Buffer) = target.setExpiryPolicy(policy)

  def getSourceDurable:Long = source.getDurable
  def setSourceDurable(durable: Long) = source.setDurable(durable)

  def getTargetDurable:Long = target.getDurable
  def setTargetDurable(durable: Long) = target.setDurable(durable)

  // TODO - implement with tmp topic/queues
  def setDynamic(lifetime:Lifetime) = {}
  def getDynamic = null.asInstanceOf[Lifetime]

  def setDistributionMode(mode:Buffer) = source.setDistributionMode(mode)
  def getDistributionMode:Buffer = source.getDistributionMode

  def setFilter(filter:Map[_, _]) = source.setFilter(filter)
  def getFilter:Map[_, _] = source.getFilter

  def setDefaultOutcome(outcome:AMQPType) = source.setDefaultOutcome(outcome)
  def getDefaultOutcome:AMQPType = source.getDefaultOutcome

  def setPossibleOutcomes(outcomes:Array[AMQPSymbol]) = source.setOutcomes(outcomes)
  def getPossibleOutcomes:Array[AMQPSymbol] = source.getOutcomes

  def setCapabilities(capabilities:Array[AMQPSymbol]) = source.setCapabilities(capabilities)
  def getCapabilities:Array[AMQPSymbol] = source.getCapabilities

  def setDesiredCapabilities(capabilities:Array[AMQPSymbol]) = source.setCapabilities(capabilities)
  def getDesiredCapabilities:Array[AMQPSymbol] = source.getCapabilities

  def peer_flowstate(flowState: Flow) = {
    Option(flowstate.getEcho) match {
      case Some(echo) =>
        if (echo.booleanValue) {
          session.send(this, flowstate)
        }
      case None =>
    }
  }
  def transfer(message:Message) : Unit

  def role:Role

  implicit def jlLong2OptionLong(value:java.lang.Long) = {
    if (value == null) {
      None
    } else {
      Option(value.longValue)
    }
  }
}
