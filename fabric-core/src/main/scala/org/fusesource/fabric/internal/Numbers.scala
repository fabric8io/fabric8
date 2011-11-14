/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal

/**
 * Helper methods for working with numbers
 */
object Numbers {

  def toNumber(value: Any, message: => String): Double = {
    value match {
      case n: Number => n.doubleValue
      case null => throw new NullPointerException(message + " is not a number")
      case x: AnyRef =>
        x.toString.toDouble
      //case v: AnyRef => throw new ValueNotNumberException(message, v)
    }
  }
}

class ValueNotNumberException(message: String, val value: AnyRef)
        extends IllegalArgumentException(message + " is not a number " + value + (if (value != null) {
          " of type " + value.getClass.getName
        } else " it was null")) {

}