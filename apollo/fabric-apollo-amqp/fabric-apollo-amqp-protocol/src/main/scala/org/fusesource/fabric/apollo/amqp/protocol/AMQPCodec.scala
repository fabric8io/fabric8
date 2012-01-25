/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import java.nio.channels._
import java.nio.ByteBuffer
import org.fusesource.hawtbuf.{DataByteArrayOutputStream, Buffer}
import java.io.{DataInputStream, EOFException}
import org.apache.activemq.apollo.broker.Sizer
import org.apache.activemq.apollo.util.Logging
import java.net.SocketException
import org.fusesource.fabric.apollo.amqp.codec._
import marshaller.{BitUtils, AMQPProtocolHeaderCodec}
import org.fusesource.hawtdispatch.transport._
import ProtocolCodec.BufferState
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import types.{AMQPTransportFrame, AMQPProtocolHeader}
import org.apache.activemq.apollo.broker.protocol.ProtocolCodecFactory

/*
*
*/
object AMQPConstants {
  val PROTOCOL = "amqp"
  val MAGIC = new Buffer(AMQPDefinitions.MAGIC)
  val MIN_MAX_FRAME_SIZE = AMQPDefinitions.MIN_MAX_FRAME_SIZE
}

import AMQPConstants._

/*
*
*/
class AMQPProtocolCodecFactory extends ProtocolCodecFactory.Provider {

  def id = PROTOCOL

  def createProtocolCodec = new AMQPCodec

  def isIdentifiable = true

  def maxIdentificaionLength = MAGIC.length

  def matchesIdentification(buffer: Buffer): Boolean = {
    buffer.startsWith(MAGIC)
  }
}

/*
*
*/
object AMQPCodec extends Sizer[AMQPFrame] {

  def size(value: AMQPFrame) = {
    value match {
      case x: AMQPProtocolHeader => AMQPProtocolHeaderCodec.INSTANCE.getFixedSize
      case x: AMQPTransportFrame => x.getFrameSize.toInt
    }
  }
}

/*
*
*/
class AMQPCodec extends ProtocolCodec with Logging {

  implicit def toBuffer(value: Array[Byte]): Buffer = new Buffer(value)

  def protocol = PROTOCOL

  var write_buffer_size = 1024 * 64;
  var write_counter = 0L
  var write_channel: WritableByteChannel = null

  var next_write_buffer = new DataByteArrayOutputStream(write_buffer_size)
  var write_buffer = ByteBuffer.allocate(0)

  def full = next_write_buffer.size() >= (write_buffer_size >> 1)

  def is_empty = write_buffer.remaining() == 0

  def setWritableByteChannel(channel: WritableByteChannel) = {
    this.write_channel = channel
    write_channel match {
      case s: SocketChannel =>
        try {
          s.socket().setSendBufferSize(write_buffer_size);
        } catch {
          case e: SocketException => warn("Unable to set write buffer size to " + write_buffer_size + " using " + s.socket().getSendBufferSize)
        }
        write_buffer_size = s.socket().getSendBufferSize
      case _ =>
    }
  }

  def getWriteCounter = write_counter

  def write(command: AnyRef): ProtocolCodec.BufferState = {
    if ( full ) {
      ProtocolCodec.BufferState.FULL
    } else {
      val was_empty = is_empty
      debug("Sending %s", command);
      command match {
        case frame: AMQPProtocolHeader =>
          AMQPProtocolHeaderCodec.INSTANCE.encode(frame, next_write_buffer)
        case frame: AMQPTransportFrame =>
          frame.write(next_write_buffer)
      }
      if ( was_empty ) {
        ProtocolCodec.BufferState.WAS_EMPTY
      } else {
        ProtocolCodec.BufferState.NOT_EMPTY
      }
    }
  }

  def flush(): ProtocolCodec.BufferState = {
    // if we have a pending write that is being sent over the socket...
    if ( write_buffer.remaining() != 0 ) {
      //trace("Remaining data in write buffer : %s bytes", write_buffer.remaining())
      val bytes_out = write_channel.write(write_buffer)
      //trace("Wrote %s bytes", bytes_out);
      write_counter += bytes_out
    }


    // if it is now empty try to refill...
    if ( is_empty && next_write_buffer.size() != 0 ) {
      // size of incoming buffer is based on how much was used in the outgoing buffer.
      val prev_size = (write_buffer.position() + 512).max(512).min(write_buffer_size)
      write_buffer = next_write_buffer.toBuffer().toByteBuffer()
      next_write_buffer = new DataByteArrayOutputStream(prev_size)
      //trace("Current write buffer size is %s bytes, incoming write buffer size is %s bytes", write_buffer.remaining, prev_size)
    }

    if ( is_empty ) {
      ProtocolCodec.BufferState.EMPTY
    } else {
      ProtocolCodec.BufferState.NOT_EMPTY
    }
  }

  var read_counter = 0L
  var read_buffer_size = 1024 * 64
  var read_channel: ReadableByteChannel = null

  var next_action: () => AnyRef = read_protocol_header
  var read_buffer: ByteBuffer = ByteBuffer.allocate(AMQPProtocolHeaderCodec.INSTANCE.getFixedSize)
  var read_waiting_on = AMQPProtocolHeaderCodec.INSTANCE.getFixedSize


  def setReadableByteChannel(channel: ReadableByteChannel) = {
    this.read_channel = channel
    read_channel match {
      case s: SocketChannel =>
        val sock = s.socket();
        try {
          sock.setReceiveBufferSize(read_buffer_size);
        } catch {
          case e: SocketException => warn("Unable to set receive buffer size to " + read_buffer_size + " using " + sock.getReceiveBufferSize)
        }
        read_buffer_size = sock.getReceiveBufferSize

      case _ =>
    }
  }

  def unread(buffer: Array[Byte]) = {
    assert(read_counter == 0)
    read_buffer = ByteBuffer.allocate(buffer.length.max(read_waiting_on))
    read_buffer.put(buffer)
    read_counter += buffer.length
    read_waiting_on -= buffer.length
    if ( read_waiting_on <= 0 ) {
      read_buffer.flip
    }
  }

  def getReadCounter = read_counter

  override def read(): Object = {

    var command: Object = null
    while (command == null) {
      // do we need to read in more data???
      if ( read_waiting_on > 0 ) {
        assert(read_buffer.remaining >= read_waiting_on, "read_buffer too small")

        // Try to fill the buffer with data from the socket..
        var count = read_channel.read(read_buffer)
        //trace("Read in %s bytes", count)
        if ( count == -1 ) {
          throw new EOFException("Peer disconnected")
        } else if ( count == 0 ) {
          return null
        }
        read_counter += count
        read_waiting_on -= count

        if ( read_waiting_on <= 0 ) {
          read_buffer.flip
        }

      } else {
        command = next_action()
        if ( read_waiting_on > read_buffer.remaining ) {
          val next_buffer = try {
            ByteBuffer.allocate((read_buffer.limit - read_buffer.position) + read_waiting_on)
          } catch {
            case o: OutOfMemoryError =>
              error("Caught OOM allocating read buffer %s", o)
              throw o
            case t: Throwable =>
              error("Caught exception allocating read buffer %s", t)
              throw t
          }
          // Move any unread bytes into the incoming buffer.. (don't think we ever have any)
          next_buffer.put(read_buffer)
          read_buffer = next_buffer
        }
      }
    }
    debug("Received %s", command)
    return command
  }

  def read_protocol_header: () => AnyRef = () => {
    val protocol_header = AMQPProtocolHeaderCodec.INSTANCE.decode(new DataInputStream(read_buffer.array.in))
    val new_pos = read_buffer.position + AMQPProtocolHeaderCodec.INSTANCE.getFixedSize
    read_buffer.position(new_pos)
    //trace("Read protocol header, read_buffer position : %s", read_buffer.position)

    read_waiting_on += 8
    next_action = read_frame
    protocol_header
  }

  def read_frame: () => AnyRef = () => {

    val header = new Buffer(8)
    read_buffer.get(header.data)

    val size = BitUtils.getUInt(header.data, 0).asInstanceOf[Int]
    val rc = new AMQPTransportFrame(header, new Buffer(read_buffer.array, read_buffer.position, size))

    read_buffer.position(read_buffer.position + size)

    //trace("Read frame, read buffer position : %s", read_buffer.position)

    read_waiting_on += 8
    next_action = read_frame
    rc
  }

  def getLastWriteSize = 0

  def getLastReadSize = 0

  def getReadBufferSize = read_buffer_size

  def getWriteBufferSize = write_buffer_size
}

