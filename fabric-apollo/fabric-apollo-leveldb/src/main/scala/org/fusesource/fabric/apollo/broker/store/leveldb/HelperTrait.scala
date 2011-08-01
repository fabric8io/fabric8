/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.apollo.broker.store.leveldb

import org.fusesource.hawtbuf._
import org.fusesource.leveldbjni._

object HelperTrait {

  final val messages_db_byte = 'm'.toByte
  final val message_refs_db_byte = 'r'.toByte
  final val queues_db_byte = 'q'.toByte
  final val entries_db_byte = 'e'.toByte

  final val messages_db = Array(messages_db_byte)
  final val message_refs_db = Array(message_refs_db_byte)
  final val queues_db = Array(queues_db_byte)
  final val entries_db = Array(entries_db_byte)

  def encode(a1:Long):Array[Byte] = {
    val out = new DataByteArrayOutputStream(8)
    out.writeLong(a1)
    out.getData
  }

  def encode(a1:Byte, a2:Long):Array[Byte] = {
    val out = new DataByteArrayOutputStream(9)
    out.writeByte(a1.toInt)
    out.writeLong(a2)
    out.getData
  }

  def decode_long_key(bytes:Array[Byte]):(Byte, Long) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readByte(), in.readLong())
  }

  def encode(a1:Byte, a2:Long, a3:Long):Array[Byte] = {
    val out = new DataByteArrayOutputStream(17)
    out.writeByte(a1)
    out.writeLong(a2)
    out.writeLong(a3)
    out.toBuffer.data
  }

  def decode_long_long_key(bytes:Array[Byte]):(Byte,Long,Long) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readByte(), in.readLong(), in.readLong())
  }

  def encode(a1:Byte, a2:Int):Array[Byte] = {
    val out = new DataByteArrayOutputStream(5)
    out.writeByte(a1)
    out.writeInt(a2)
    out.toBuffer.data
  }

  def decode_int_key(bytes:Array[Byte]):(Byte,Int) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readByte(), in.readInt())
  }

  final class RichDB(val db: DB) {


    def get(key:Array[Byte], ro:ReadOptions=new ReadOptions):Option[Array[Byte]] = {
      Option(db.get(ro, key))
    }

    def delete = db.delete()

    def delete(key:Array[Byte], wo:WriteOptions=new WriteOptions):Unit = {
      db.delete(wo, key)
    }

    def put(key:Array[Byte], value:Array[Byte], wo:WriteOptions=new WriteOptions):Unit = {
      db.put(wo, key, value)
    }

    def write[T](wo:WriteOptions=new WriteOptions)(func: WriteBatch=>T):T = {
      val updates = new WriteBatch()
      try {
        val rc=Some(func(updates))
        db.write(wo, updates)
        return rc.get
      } finally {
        updates.delete();
      }
    }

    def snapshot[T](func: Snapshot=>T):T = {
      val snapshot = db.getSnapshot
      try {
        func(snapshot)
      } finally {
        db.releaseSnapshot(snapshot)
      }
    }

    def cursor_keys(ro:ReadOptions=new ReadOptions)(func: Array[Byte] => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seekToFirst();
      try {
        while( iterator.isValid && func(iterator.key()) ) {
          iterator.next()
        }
      } finally {
        iterator.delete();
      }
    }

    def cursor_keys_prefixed(prefix:Array[Byte], ro:ReadOptions=new ReadOptions)(func: Array[Byte] => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(prefix);
      try {
        def check(key:Array[Byte]) = {
          key.startsWith(prefix) && func(key)
        }
        while( iterator.isValid && check(iterator.key()) ) {
          iterator.next()
        }
      } finally {
        iterator.delete();
      }
    }

    def cursor_prefixed(prefix:Array[Byte], ro:ReadOptions=new ReadOptions)(func: (Array[Byte],Array[Byte]) => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(prefix);
      try {
        def check(key:Array[Byte]) = {
          key.startsWith(prefix) && func(key, iterator.value())
        }
        while( iterator.isValid && check(iterator.key()) ) {
          iterator.next()
        }
      } finally {
        iterator.delete();
      }
    }

    def compare(a1:Array[Byte], a2:Array[Byte]):Int = {
      new Buffer(a1).compareTo(new Buffer(a2))
    }

    def cursor_range_keys(start_included:Array[Byte], end_excluded:Array[Byte], ro:ReadOptions=new ReadOptions)(func: Array[Byte] => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(start_included);
      try {
        def check(key:Array[Byte]) = {
          (compare(key,end_excluded) < 0) && func(key)
        }
        while( iterator.isValid && check(iterator.key()) ) {
          iterator.next()
        }
      } finally {
        iterator.delete();
      }
    }

    def cursor_range(start_included:Array[Byte], end_excluded:Array[Byte], ro:ReadOptions=new ReadOptions)(func: (Array[Byte],Array[Byte]) => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(start_included);
      try {
        def check(key:Array[Byte]) = {
          (compare(key,end_excluded) < 0) && func(key, iterator.value())
        }
        while( iterator.isValid && check(iterator.key()) ) {
          iterator.next()
        }
      } finally {
        iterator.delete();
      }
    }

    def last_key(prefix:Array[Byte], ro:ReadOptions=new ReadOptions): Option[Array[Byte]] = {
      val copy = new Buffer(prefix).deepCopy().data
      if ( copy.length > 0 ) {
        val pos = copy.length-1
        copy(pos) = (copy(pos)+1).toByte
      }
      val iterator = db.iterator(ro)
      try {
        iterator.seek(copy);
        if ( iterator.isValid ) {
          iterator.prev()
        } else {
          iterator.seekToLast()
        }
        
        if ( iterator.isValid ) {
          val key = iterator.key()
          if(key.startsWith(prefix)) {
            Some(key)
          } else {
            None
          } 
        } else {
          None
        }
      } finally {
        iterator.delete();
      }
    }
  }

}
