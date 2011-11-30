/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.cluster.model

import org.fusesource.hawtbuf.Buffer
import org.apache.activemq.apollo.broker.Sizer

object Frame extends Sizer[Frame] {
  def size(value: Frame) = value.data.length
}


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class Frame(command:Int, data:Buffer) {
  def length = data.length
}
