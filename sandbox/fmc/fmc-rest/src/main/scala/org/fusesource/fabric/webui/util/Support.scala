/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.webui.util

import java.io._
import java.util.concurrent.{ThreadFactory, LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}
import java.util.Properties
import java.util.regex.{Matcher, Pattern}
import java.lang.reflect.{Method, Field}
import collection.mutable.ListBuffer
import java.security.AccessControlException
import java.beans.{PropertyEditor, PropertyEditorManager}

object IOSupport {

  def read_bytes(in: InputStream) = {
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

  def write_text(out: OutputStream, value: String, charset: String = "UTF-8"): Unit = {
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

  def fix_file_separator(command: String) = command.replaceAll( """/|\\""", Matcher.quoteReplacement(file_separator))

  case class RichFile(self: File) {

    def /(path: String) = new File(self, path)

    def copy_to(target: File) = {
      using(new FileOutputStream(target)) {
        os =>
          using(new FileInputStream(self)) {
            is =>
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
      using(new FileInputStream(self)) {
        in =>
          IOSupport.read_bytes(in)
      }
    }

    def read_text(charset: String = "UTF-8"): String = new String(this.read_bytes, charset)

    def write_bytes(data: Array[Byte]): Unit = {
      using(new FileOutputStream(self)) {
        out =>
          IOSupport.write_bytes(out, data)
      }
    }

    def write_text(data: String, charset: String = "UTF-8"): Unit = {
      using(new FileOutputStream(self)) {
        out =>
          IOSupport.write_text(out, data, charset)
      }
    }
  }

}
