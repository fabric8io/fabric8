/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.launcher.api

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