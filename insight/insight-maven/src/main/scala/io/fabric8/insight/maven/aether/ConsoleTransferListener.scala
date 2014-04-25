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
package io.fabric8.insight.maven.aether

import java.io.PrintStream
import java.text.{DecimalFormatSymbols, DecimalFormat}
import java.util.Locale

import collection.JavaConversions._
import collection.mutable.HashMap

import org.sonatype.aether.transfer.AbstractTransferListener
import org.sonatype.aether.transfer.TransferEvent
import org.sonatype.aether.transfer.TransferResource

class ConsoleTransferListener(out: PrintStream) extends AbstractTransferListener {

  var lastLength: Int = 0
  var downloads = new HashMap[TransferResource, Long]()

  override def transferSucceeded(event: TransferEvent) = {
    transferCompleted(event)

    val resource = event.getResource()
    val contentLength = event.getTransferredBytes()
    if (contentLength >= 0) {
      val message = if (event.getRequestType == TransferEvent.RequestType.PUT) "Uploaded" else "Downloaded"

      val len = if (contentLength >= 1024) {
        toKB(contentLength) + " KB"
      } else {
        contentLength + " B"
      }

      var throughput = ""
      val duration = System.currentTimeMillis() - resource.getTransferStartTime()
      if (duration > 0) {
        val format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH))
        val kbPerSec = (contentLength / 1024.0) / (duration / 1000.0)
        throughput = " at " + format.format(kbPerSec) + " KB/sec"
      }

      out.println(message + ": " + resource.getRepositoryUrl() + resource.getResourceName() +
              " (" + len + throughput + ")")
    }
  }

  override def transferProgressed(event: TransferEvent) = {
    val resource = event.getResource()
    downloads += (resource -> event.getTransferredBytes())
    val buffer = new StringBuilder(64)
    for ((key, value) <- downloads) {
      val total = key.getContentLength
      val complete = value.longValue
      buffer.append(getStatus(complete, total)).append("  ")
    }

    val size = lastLength - buffer.length
    lastLength = buffer.length
    pad(buffer, size)
    buffer.append('\r')

    out.print(buffer)
  }

  override def transferInitiated(event: TransferEvent) = {
    val message = if (event.getRequestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"

    out.println(message + ": " + event.getResource.getRepositoryUrl + event.getResource.getResourceName())
  }

  override def transferFailed(event: TransferEvent) = {
    transferCompleted(event)

    event.getException().printStackTrace(out)
  }

  override def transferCorrupted(event: TransferEvent) = {
    event.getException.printStackTrace(out)
  }

  protected def getStatus(complete: Long, total: Long) = {
    if (total >= 1024) {
      toKB(complete) + "/" + toKB(total) + " KB "
    }
    else if (total >= 0) {
      complete + "/" + total + " B "
    }
    else if (complete >= 1024) {
      toKB(complete) + " KB "
    }
    else {
      complete + " B "
    }
  }

  protected def pad(buffer: StringBuilder, size: Int): Unit = {
    var block: String = "                                        "
    var spaces = size
    while (spaces > 0) {
      var n: Int = Math.min(spaces, block.length)
      buffer.append(block.substring(0, n))
      spaces -= n
    }
  }

  protected def toKB(bytes: Long): Long = (bytes + 1023) / 1024

  private def transferCompleted(event: TransferEvent) = {
    downloads.remove(event.getResource())

    val buffer = new StringBuilder(64)
    pad(buffer, lastLength)
    buffer.append('\r')
    out.print(buffer)
  }
}
