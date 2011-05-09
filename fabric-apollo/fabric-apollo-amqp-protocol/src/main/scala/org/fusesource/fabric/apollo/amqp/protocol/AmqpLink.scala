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

import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.util.Logging
import java.util._
import org.fusesource.fabric.apollo.amqp.api._
import AmqpRole.SENDER
import AmqpRole.RECEIVER
import AmqpConversions._
import scala.util.continuations._
import collection.mutable.ListBuffer

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

  var source = createAmqpSource
  var target = createAmqpTarget

  source.setDistributionMode(DistributionMode.MOVE)

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
        val attach: AmqpAttach = createAmqpAttach
        attach.setHandle(handle.get)
        attach.setName(name)
        attach.setRole(role)
        attach.setSource(source)
        attach.setTarget(target)
        session.send(this, attach)
    }
  }

  def attach(attach: AmqpAttach): Unit = {
    remoteHandle = Some(attach.getHandle.getValue.shortValue)
    handle match {
      case None =>
        this.attach
      case Some(handle) =>
    }
    if (established) {
      onAttach.foreach((x) => session.dispatch_queue << x)
    }
  }

  def send_updated_flow_state(flowstate:AmqpFlow): Unit = {
    handle.foreach((x) => {
      flowstate.setHandle(x)
      //trace("Updating peer with flow state : %s", flowstate)
      session.send(this, flowstate)
    })
  }

  def detach: Unit = detach(None)
  def detach(description:String): Unit = detach(AmqpLinkError.DETACH_FORCED, description)
  def detach(t:Throwable):Unit = detach(AmqpLinkError.DETACH_FORCED, t)

  def detach(condition:String, description:String):Unit = {
    val error = createAmqpError
    error.setCondition(condition)
    error.setDescription(description)
    detach(Option(error))
  }

  def detach(condition:String, t:Throwable):Unit = {
    val error = createAmqpError
    error.setCondition(condition)
    error.setDescription(t.toString + "\n" + t.getStackTraceString)
    detach(Option(error))
  }

  def detach(error:Option[AmqpError]):Unit = {
    if (established) {
      val d = createAmqpDetach
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

  def detach(detach:AmqpDetach) : Unit = {
    remoteHandle = None
    val response = createAmqpDetach
    handle.foreach((h) => {
      response.setHandle(h)
      session.detach(this)
      handle = None
      session.send(this, response)
    })
    onDetach.foreach((x) => session.dispatch_queue << x)
  }

  def flowstate = createAmqpFlow

  def isFlowControlEnabled():Boolean

  def setName(name:String) = this.name = name
  def getName:String = this.name

  def address = {
    role match {
      case SENDER =>
        if (remotely_created) {
          source.getAddress.asInstanceOf[AmqpString].getValue
        } else {
          target.getAddress.asInstanceOf[AmqpString].getValue
        }
      case RECEIVER =>
        if (remotely_created) {
          target.getAddress.asInstanceOf[AmqpString].getValue
        } else {
          source.getAddress.asInstanceOf[AmqpString].getValue
        }
    }
  }

  def address_=(address:String) = {
    // TODO - maybe not the best place to set this
    if (address.startsWith("queue")) {
      setDistributionMode(DistributionMode.MOVE)
    } else if (address.startsWith("topic")) {
      setDistributionMode(DistributionMode.COPY)
    }

    role match {
      case SENDER =>
        if (remotely_created) {
          source.setAddress(createAmqpString(address))
        } else {
          target.setAddress(createAmqpString(address))
        }
      case RECEIVER =>
        if (remotely_created) {
          target.setAddress(createAmqpString(address))
        } else {
          source.setAddress(createAmqpString(address))
        }
    }
  }

  def setAddress(address:String) = this.address = address
  def getAddress = this.address

  def getSourceTimeout = source.getTimeout
  def setSourceTimeout(timeout: AmqpSeconds) = source.setTimeout(timeout)

  def getTargetTimeout = target.getTimeout
  def setTargetTimeout(timeout: AmqpSeconds) = target.setTimeout(timeout)

  def getSourceExpiryPolicy = source.getExpiryPolicy
  def setSourceExpiryPolicy(policy: AmqpTerminusExpiryPolicy) = source.setExpiryPolicy(policy)

  def getTargetExpiryPolicy = target.getExpiryPolicy
  def setTargetExpiryPolicy(policy: AmqpTerminusExpiryPolicy) = target.setExpiryPolicy(policy)

  def getSourceDurable = source.getDurable
  def setSourceDurable(durable: Boolean) = source.setDurable(durable)

  def getTargetDurable = target.getDurable
  def setTargetDurable(durable: Boolean) = target.setDurable(durable)

  // TODO - implement with tmp topic/queues
  def setDynamic(lifetime:Lifetime) = {}
  def getDynamic = null.asInstanceOf[Lifetime]

  def setDistributionMode(mode:DistributionMode) = source.setDistributionMode(mode)
  def getDistributionMode = source.getDistributionMode

  def setFilter(filter:AmqpFilterSet) = source.setFilter(filter)
  def getFilter = source.getFilter

  def setDefaultOutcome(outcome:Outcome) = source.setDefaultOutcome(outcome)
  def getDefaultOutcome = source.getDefaultOutcome

  def array2Multiple[T <: AmqpType[_, _]](arr:Array[T]):Multiple = {
    val rc = createMultiple
    if (arr.size == 0) {

    } else if (arr.size == 1) {
      rc.setValue(arr(0))
    } else {
      val arrayList = new ArrayList[T]
      arr.foreach((x) => arrayList.add(x))
      rc.setValue(createAmqpList(new IAmqpList.AmqpWrapperList(arrayList.asInstanceOf[java.util.List[AmqpType[_, _]]])))
    }
    rc
  }

  def multiple2Array[T <: AmqpType[_, _] : ClassManifest](mult:Multiple):Array[T] = {
    mult.getValue match {
      case t:T =>
        val rc = new ListBuffer[T]
        rc.append(t)
        rc.toArray
      case list:AmqpList =>
        val iter = list.iterator
        val rc = ListBuffer[T]()
        while (iter.hasNext) {
          rc.append(iter.next.asInstanceOf[T])
        }
        rc.toArray
      case _ =>
        null
    }
  }

  def array_string_2_array_symbol(arr:Array[String]) = {
    val tmp = ListBuffer[AmqpSymbol]()
    arr.foreach((x) => tmp.append(createAmqpSymbol(x)))
    tmp.toArray
  }

  def array_symbol_2_array_string(arr:Array[AmqpSymbol]) = {
    val tmp = ListBuffer[String]()
    arr.foreach((x) => tmp.append(x.getValue))
    tmp.toArray
  }

  def array_outcome_2_array_amqp_outcome(arr:Array[Outcome]) = {
    val tmp = ListBuffer[AmqpType[_, _]]()
    arr.foreach((x) => tmp.append(outcome2AmqpType(x)))
    tmp.toArray
  }

  def array_amqp_outcome_2_array_outcome(arr:Array[AmqpType[_, _]]) = {
    val tmp = ListBuffer[AmqpType[_, _]]()
    arr.foreach((x) => tmp.append(amqpType2Outcome(x)))
    tmp.toArray
  }

  def setPossibleOutcomes(outcomes:Array[Outcome]) = source.setOutcomes(array2Multiple(array_outcome_2_array_amqp_outcome(outcomes)))
  def getPossibleOutcomes = array_amqp_outcome_2_array_outcome(multiple2Array(source.getOutcomes))

  def setCapabilities(capabilities:Array[String]) = source.setCapabilities(array2Multiple(array_string_2_array_symbol(capabilities)))
  def getCapabilities = array_symbol_2_array_string(multiple2Array(source.getCapabilities))

  def setDesiredCapabilities(capabilities:Array[String]) = source.setCapabilities(array2Multiple(array_string_2_array_symbol(capabilities)))
  def getDesiredCapabilities = array_symbol_2_array_string(multiple2Array(source.getCapabilities))

  def peer_flowstate(flowState: AmqpFlow) = {
    Option(flowstate.getEcho) match {
      case Some(echo) =>
        if (echo.booleanValue) {
          session.send(this, flowstate)
        }
      case None =>
    }
  }
  def transfer(message:AmqpProtoMessage) : Unit

  def role:AmqpRole

  implicit def jlLong2OptionLong(value:java.lang.Long) = {
    if (value == null) {
      None
    } else {
      Option(value.longValue)
    }
  }

  implicit def amqpSequenceNo2OptionLong(value:AmqpSequenceNo) = {
    if (value == null) {
      None
    } else {
      Option(value.getValue.longValue)
    }
  }
}
