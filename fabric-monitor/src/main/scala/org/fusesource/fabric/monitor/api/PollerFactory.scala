/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.api

/**
 * Creates a Poller for a given DataSourceDTO
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait PollerFactory {

  def jaxb_package: String

  def accepts(source: DataSourceDTO): Boolean

  def create(source: DataSourceDTO): Poller

}

/**
 * Capable of polling for a value
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait Poller {
  def close: Unit

  def source: DataSourceDTO

  def poll: Double
}