/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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