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

package io.fabric8.launcher.api

import org.fusesource.hawtdispatch.Future

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait LaunchManager {

  /**
   * Updates the launcher manger with a new set of launcher configurations.
   */
  def configure( value:Traversable[ServiceDTO] ):Unit

  /**
   * Gets the status of all the managed services.
   */
  def status:Future[Seq[ServiceStatusDTO]]

  def close:Unit
}