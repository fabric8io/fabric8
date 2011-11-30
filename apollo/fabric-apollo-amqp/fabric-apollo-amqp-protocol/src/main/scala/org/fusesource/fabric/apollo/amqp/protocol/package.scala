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

import org.fusesource.hawtbuf.Buffer
import org.fusesource.hawtbuf.Buffer._
import org.fusesource.fabric.apollo.amqp.codec.types.StdDistMode

package object protocol {
  implicit def string2Buffer(s:String):Buffer = ascii(s).buffer
  implicit def buffer2String(b:Buffer):String = b.ascii.toString

  implicit def stdDistMode2Buffer(s:StdDistMode) = s.getValue
  implicit def buffer2StdDistMode(b:Buffer) = {
    if (b.equals(StdDistMode.MOVE.getValue)) {
      StdDistMode.MOVE
    } else if (b.equals(StdDistMode.COPY.getValue)) {
      StdDistMode.COPY
    }
  }

}