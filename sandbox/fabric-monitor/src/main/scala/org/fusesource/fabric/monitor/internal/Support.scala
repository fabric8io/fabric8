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

package io.fabric8.monitor.internal

/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
import java.io._
import java.util.concurrent.{ThreadFactory, LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}
import java.util.Properties
import java.util.regex.{Matcher, Pattern}

object IOSupport {

  def read_bytes(in:InputStream) = {
    val out = new ByteArrayOutputStream()
    copy(in, out)
    out.toByteArray
  }

  /**
   * Returns the number of bytes copied.
   */
  def copy(in: InputStream, out: OutputStream): Long = {
    var bytesCopied: Long = 0
    val buffer = new Array[Byte](8192)
    var bytes = in.read(buffer)
    while (bytes >= 0) {
      out.write(buffer, 0, bytes)
      bytesCopied += bytes
      bytes = in.read(buffer)
    }
    bytesCopied
  }

  def using[R, C <: Closeable](closable: C)(proc: C => R) = {
    try {
      proc(closable)
    } finally {
      try {
        closable.close
      } catch {
        case ignore =>
      }
    }
  }

  def write_text(out: OutputStream, value: String, charset:String="UTF-8"): Unit = {
    write_bytes(out, value.getBytes(charset))
  }

  def write_bytes(out: OutputStream, data: Array[Byte]): Unit = {
    copy(new ByteArrayInputStream(data), out)
  }
}

object FileSupport {
  import IOSupport._

  implicit def to_rich_file(file: File): RichFile = new RichFile(file)

  val file_separator = System.getProperty("file.separator")

  def fix_file_separator(command:String) = command.replaceAll("""/|\\""", Matcher.quoteReplacement(file_separator))

  case class RichFile(self: File) {

    def /(path: String) = new File(self, path)

    def copy_to(target: File) = {
      using(new FileOutputStream(target)) { os =>
        using(new FileInputStream(self)) { is =>
          IOSupport.copy(is, os)
        }
      }
    }

    def recursive_list: List[File] = {
      if (self.isDirectory) {
        self :: self.listFiles.toList.flatten(_.recursive_list)
      } else {
        self :: Nil
      }
    }

    def recursive_delete: Unit = {
      if (self.exists) {
        if (self.isDirectory) {
          self.listFiles.foreach(_.recursive_delete)
        }
        self.delete
      }
    }

    def recursive_copy_to(target: File): Unit = {
      if (self.isDirectory) {
        target.mkdirs
        self.listFiles.foreach(file => file.recursive_copy_to(target / file.getName))
      } else {
        self.copy_to(target)
      }
    }

    def read_bytes: Array[Byte] = {
      using(new FileInputStream(self)) { in =>
        IOSupport.read_bytes(in)
      }
    }

    def read_text(charset: String = "UTF-8"): String = new String(this.read_bytes, charset)

    def write_bytes(data:Array[Byte]):Unit = {
      using(new FileOutputStream(self)) { out =>
        IOSupport.write_bytes(out, data)
      }
    }

    def write_text(data:String, charset:String="UTF-8"):Unit = {
      using(new FileOutputStream(self)) { out =>
        IOSupport.write_text(out, data, charset)
      }
    }

  }

}


object FilterSupport {

  private val pattern: Pattern = Pattern.compile("\\$\\{([^\\}]+)\\}")

  implicit def asScalaMap(props:Properties):Map[String,String] = {
    import collection.JavaConversions._
    Map[String,String](collectionAsScalaIterable(props.entrySet).toSeq.map(x=>(x.getKey.toString, x.getValue.toString)):_*)
  }

  def translate(value: String, translations:Map[String,String]): String = {
    var rc = new StringBuilder
    var remaining = value
    while( !remaining.isEmpty ) {
      if( !translations.find{ case (key,value) =>
        if( remaining.startsWith(key) ) {
          rc.append(value)
          remaining = remaining.stripPrefix(key)
          true
        } else {
          false
        }
      }.isDefined ) {
        rc.append(remaining.charAt(0))
        remaining = remaining.substring(1)
      }
    }
    rc.toString
  }

  def filter(value: String, props:Map[String,String]): String = {
    var rc = value
    var start: Int = 0
    var done = false
    while (!done) {
      var matcher: Matcher = pattern.matcher(rc)
      if( matcher.find(start) ) {
        var group = matcher.group(1)
        props.get(group) match {
          case Some(property)=>
            rc = matcher.replaceFirst(Matcher.quoteReplacement(property))
          case None =>
            start = matcher.end
        }
      } else {
        done = true
      }
    }
    rc
  }
}