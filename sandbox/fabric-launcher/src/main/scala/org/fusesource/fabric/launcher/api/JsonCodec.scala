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

import org.codehaus.jackson.map.ObjectMapper
import java.io._
import java.util.Properties
import io.fabric8.launcher.internal.IOSupport._
import java.util.regex.Matcher
import java.util.regex.Pattern
import io.fabric8.launcher.internal.FilterSupport


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