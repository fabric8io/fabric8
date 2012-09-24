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

package org.fusesource.camel.tooling.util

object Objects {
  /**
   * A helper method to return a non null value or the default value if it is null
   */
  def getOrElse[T](value: T, defaultValue: => T) = if (value != null) value else defaultValue


  def notNull[T <: AnyRef](value: T, message: => String): T = {
    if (value == null) {
      throw new IllegalArgumentException(message)
    }
    value
  }

  def assertInjected[T <: AnyRef](value: T)(implicit m: ClassManifest[T]): T = notNull(value, "Value of type " + m.erasure.getName + " has not been injected!")
}