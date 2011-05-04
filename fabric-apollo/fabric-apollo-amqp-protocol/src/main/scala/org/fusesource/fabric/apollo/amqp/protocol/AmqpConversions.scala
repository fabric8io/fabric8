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
import TypeFactory._
import org.fusesource.fabric.apollo.amqp.api._
import collection.mutable.ListBuffer
import Lifetime._
import DistributionMode._
import Outcome._
import java.util.ArrayList
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

/**
 *
 */

object AmqpConversions {

  implicit def amqpLinkError2String(value:AmqpLinkError) = value.getValue.getValue
  implicit def amqpSessionError2String(value:AmqpSessionError) = value.getValue.getValue
  implicit def amqpAmqpError2String(value:AmqpAmqpError) = value.getValue.getValue
  implicit def amqpAmqpConnectionError2String(value:AmqpConnectionError) = value.getValue.getValue
  implicit def string2JavaLangString(value:String) = value.asInstanceOf[java.lang.String]

  implicit def jlShort2Short(value:java.lang.Short) = value.asInstanceOf[Short]
  implicit def jlBoolean2Boolean(value:java.lang.Boolean) = value.asInstanceOf[Boolean]
  implicit def handle2Int(value:AmqpHandle) = value.getValue.intValue
  implicit def transferNumber2Long(value:AmqpTransferNumber) = value.getValue.longValue
  implicit def sequenceNumber2Long(value:AmqpSequenceNo) = value.getValue.longValue

  implicit def narrow(value:AmqpType[_,_]) = value.asInstanceOf[AmqpType[_, AmqpBuffer[_]]]
  implicit def widen(value:AmqpType[_, AmqpBuffer[_]]) = value.asInstanceOf[AmqpType[_, _]]

  implicit def amqpType2AmqpTarget(value:AmqpType[_,_]):AmqpTarget = value.asInstanceOf[AmqpTarget]

  implicit def amqpType2AmqpSource(value:AmqpType[_,_]):AmqpSource = value.asInstanceOf[AmqpSource]

  implicit def outcome2AmqpType(outcome:Outcome) = {
    outcome match {
      case ACCEPTED =>
        createAmqpAccepted
      case REJECTED =>
        createAmqpRejected
      case RELEASED =>
        createAmqpReleased
      case MODIFIED =>
        createAmqpModified
    }
  }

  implicit def amqpType2Outcome(value:AmqpType[_, _]):Outcome = {
    Option[AmqpType[_, _]](value) match {
      case Some(value) =>
        if (value.isInstanceOf[AmqpAccepted]) {
          ACCEPTED
        } else if ( value.isInstanceOf[AmqpRejected]) {
          REJECTED
        } else if (value.isInstanceOf[AmqpReleased]) {
          RELEASED
        } else if (value.isInstanceOf[AmqpModified]) {
          MODIFIED
        } else {
          null
        }
      case None =>
        null
    }
  }

  implicit def amqpType2Lifetime[T <: AmqpType[_, _]](value:T):Lifetime = {
    Option[T](value) match {
      case Some(value) =>
        if (value.isInstanceOf[AmqpDeleteOnClose]) {
          DELETE_ON_CLOSE
        } else if (value.isInstanceOf[AmqpDeleteOnNoLinks]) {
          DELETE_ON_NO_LINKS
        } else if (value.isInstanceOf[AmqpDeleteOnNoMessages]) {
          DELETE_ON_NO_MESSAGES
        } else if (value.isInstanceOf[AmqpDeleteOnNoLinksOrMessages]) {
          DELETE_ON_NO_LINKS_OR_MESSAGES
        } else {
          UNDEFINED
        }
      case None =>
        null
    }
  }

  implicit def lifetime2AmqpType(value:Lifetime): AmqpType[_, _] = {
    value match {
      case DELETE_ON_CLOSE =>
        createAmqpDeleteOnClose
      case DELETE_ON_NO_LINKS =>
        createAmqpDeleteOnNoLinks
      case DELETE_ON_NO_MESSAGES =>
        createAmqpDeleteOnNoMessages
      case DELETE_ON_NO_LINKS_OR_MESSAGES =>
        createAmqpDeleteOnNoLinksOrMessages
    }
  }

  implicit def distributionMode2AmqpSymbol(mode:DistributionMode):AmqpSymbol = {
    mode match {
      case MOVE =>
        AmqpStdDistMode.MOVE.getValue
      case COPY =>
        AmqpStdDistMode.COPY.getValue
    }
  }

  implicit def string2DistributionMode(mode:String):DistributionMode={
    Option[String](mode) match {
      case Some(mode) =>
        if (mode.compareToIgnoreCase("move") == 0) {
          MOVE
        } else if (mode.compareToIgnoreCase("copy") == 0) {
          COPY
        } else {
          null
        }
      case None =>
        null
    }
  }

  implicit def arrayOutcome2Multiple(value:Array[Outcome]):Multiple = {
    if (value.size == 1) {
      val multiple = createMultiple
      multiple.setValue(value(0))
      multiple
    } else if (value.size > 1) {
      val multiple = createMultiple
      val arrayList:ArrayList[AmqpType[_,_]] = new ArrayList[AmqpType[_,_]]()
      value.foreach((x) => arrayList.add(x))
      multiple.setValue(createAmqpList(new IAmqpList.AmqpWrapperList(arrayList)))
      multiple
    } else {
      createMultiple
    }
  }

  implicit def multiple2ArrayOutcome[T <: AmqpType[_,_]](value:Multiple):Array[Outcome] = {
    Option(value) match {
      case Some(value) =>
        if (value.getValue.isInstanceOf[AmqpList]) {
          val l = value.getValue.asInstanceOf[AmqpList]
          val list = new ListBuffer[Outcome]
          var iter = l.iterator
          while (iter.hasNext) {
            val outcome:Outcome = iter.next
            if (outcome != null) {
              list.append(outcome)
            }
          }
          list.toArray
        } else {
          val rc:Outcome = value.getValue.asInstanceOf[AmqpType[_, AmqpBuffer[_]]]
          if (rc != null) {
            List[Outcome](rc).toArray
          } else {
            null
          }
        }
      case None =>
        null
    }
  }

  implicit def amqpSymbol2String(value:AmqpSymbol) = value.getValue

  implicit def multiple2ListString(value:Multiple):List[String] = {
    Option(value) match {
      case Some(value) =>
        value.getValue match {
          case s:AmqpSymbol =>
            List(s.getValue)
          case s:AmqpString =>
            List(s.getValue)
          case l:AmqpList =>
            val list = new ListBuffer[String]
            val iter = l.iterator
            while (iter.hasNext) {
              iter.next match {
                case s:AmqpSymbol => list.append(s.getValue)
                case _ =>
              }
            }
            list.toList
          case _ =>
            null
        }
      case None =>
        null
    }
  }

  implicit def string2AmqpString(string:String):AmqpString = createAmqpString(string)
  implicit def amqpString2String(string:AmqpString) = {
    if (string == null) {
      null
    } else {
      string.getValue
    }
  }

  implicit def multiple2ArrayString(value:Multiple):Array[String] = {
    val out = multiple2ListString(value)
    if (out != null) {
      out.toArray
    } else {
      null
    }
  }
  implicit def arrayList2List(arg:ArrayList[_]):List[_] = arg.asInstanceOf[List[_]]

  implicit def arrayString2Multiple(value:Array[String]):Multiple = {
    val rc = createMultiple
    if (value.size == 0) {
      rc
    } else if (value.size == 1) {
      rc.setValue(value(0))
      rc
    } else {
      val list = new ArrayList[AmqpString]()
      value.foreach((x) => list.add(x))
      rc.setValue(createAmqpList(new IAmqpList.AmqpWrapperList(list.asInstanceOf[java.util.List[AmqpType[_, _]]])))
      rc
    }
  }

  implicit def amqpTypeToString[T <: AmqpType[_, _]](value:T):String = {
    Option[T](value) match {
      case Some(value) =>
        value.asInstanceOf[AmqpString].getValue.asInstanceOf[String]
      case None =>
        null
    }
  }

  implicit def narrowAmqpType2String[T <: AmqpType[_, AmqpBuffer[_]]](value:T):String = {
    Option[T](value) match {
      case Some(value) =>
        value.asInstanceOf[AmqpString].getValue.asInstanceOf[String]
      case None =>
        null
    }
  }

  implicit def message2ProtoMessage(message:Message) = message.asInstanceOf[AmqpProtoMessage]
  implicit def protoMessage2Message(message:AmqpProtoMessage) = message.asInstanceOf[Message]
}
