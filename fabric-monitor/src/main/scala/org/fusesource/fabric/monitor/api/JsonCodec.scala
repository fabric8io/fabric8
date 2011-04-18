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

import org.codehaus.jackson.map.ObjectMapper
import java.io._
import java.util.Properties
import org.fusesource.fabric.monitor.internal.IOSupport._
import org.fusesource.fabric.monitor.internal.FilterSupport


/**
 *
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object JsonCodec {
  import FilterSupport._

  def decode[T](t : Class[T], buffer: Array[Byte]): T = decode(t, new ByteArrayInputStream(buffer))
  def decode[T](t : Class[T], in: InputStream): T = decode(t, in, null)

  val encode_escapes = Map(
    """\"""->"""\\""",
    "\b"->"""\b""",
    "\f"->"""\f""",
    "\n"->"""\n""",
    "\0"->"""\0""",
    "\r"->"""\r""",
    "\t"->"""\t""",
    "\'"->"""\'""",
    "\""->"\\\""
  )

  def decode[T](t : Class[T], in: InputStream, props: Properties): T = {
    val fin = if( props==null || props.isEmpty ) {
      in
    } else {      
      val filtered = filter(new String(read_bytes(in), "UTF-8"), props.mapValues(translate(_,encode_escapes)))
      new ByteArrayInputStream(filtered.getBytes("UTF-8"))
    }
    return mapper.readValue(fin, t)
  }

  def encode(value: AnyRef): Array[Byte] = {
    var baos: ByteArrayOutputStream = new ByteArrayOutputStream
    encode(value, baos)
    return baos.toByteArray
  }

  def encode(value: AnyRef, out: OutputStream): Unit = {
    mapper.writeValue(out, value)
  }

  private var mapper: ObjectMapper = new ObjectMapper
}