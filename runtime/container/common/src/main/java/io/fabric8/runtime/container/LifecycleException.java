/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.runtime.container;

/**
 * LifecycleException
 *
 * @since 26-Feb-2014
 */
public class LifecycleException extends Exception
{
   private static final long serialVersionUID = 1L;

   public LifecycleException(String message)
   {
      super(message);
   }

   public LifecycleException(String message, Throwable cause)
   {
      super(message, cause);
   }

}
