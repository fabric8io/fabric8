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

import java.util.ArrayList
import java.util.Iterator
import java.util.List
import java.util.NoSuchElementException
import org.apache.activemq.apollo.util.TreeMap

object Interval {
  def apply[N](start:N)(implicit numeric: scala.math.Numeric[N]):Interval[N] = {
    import numeric._
    Interval(start, start+one)
  }
}

case class Interval[N](start: N, limit: N)(implicit numeric: scala.math.Numeric[N]) {
  import numeric._

  def size = limit - start
  def end = limit - one

  def start(value: N):Interval[N] = Interval(value, limit)
  def limit(value: N):Interval[N] = Interval(start, value)
  
  override def toString = {
    if (start == end) {
      start.toString
    } else {
      start.toString + "-" + end
    }
  }

  def contains(value: N): Boolean = {
    return start <= value && value < limit
  }
}

/**
 * Tracks numeric ranges.  Handy for keeping track of things like allocation or free lists.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class IntervalSet[N](implicit numeric: scala.math.Numeric[N]) extends java.lang.Iterable[Interval[N]] {
  import numeric._
  import collection.JavaConversions._
  private final val ranges = new TreeMap[N, Interval[N]]

  def copy = {
    val rc = new IntervalSet[N]
    for (r <- iterator) {
      rc.ranges.put(r.start, Interval(r.start, r.limit))
    }
    rc
  }
  def add(r:N):Unit = add(Interval(r))
  def add(r:Interval[N]): Unit = {
    var start = r.start
    var limit = r.limit
    
    var entry = ranges.floorEntry(limit)
    while (entry != null) {
      var curr = entry
      var range = curr.getValue
      entry = entry.previous
      if (range.limit < start) {
        entry = null
      } else {
        if (limit < range.limit) {
          limit = range.limit
        }
        if (start < range.start) {
          ranges.removeEntry(curr)
        } else {
          curr.setValue(range.limit(limit))
          return
        }
      }
    }
    ranges.put(start, Interval(start, limit))
  }

  def remove(r:N):Unit = remove(Interval(r))
  def remove(r:Interval[N]): Unit = {
    val start = r.start 
    var limit = r.limit
    var entry = ranges.lowerEntry(limit)
    while (entry != null) {
      
      var curr = entry
      var range = curr.getValue
      entry = entry.previous
      
      if (range.limit <= start) {
        entry = null
      } else {
        if (limit < range.limit) {
          ranges.put(limit, Interval(limit, range.limit))
        }
        if (start <= range.start) {
          ranges.removeEntry(curr)
        } else {
          curr.setValue(range.limit(start))
          entry = null
        }
      }
    }
  }

  def contains(value: N) = {
    var entry = ranges.floorEntry(value)
    if (entry == null) {
      false
    } else {
      entry.getValue.contains(value)
    }
  }

  def clear: Unit = ranges.clear

  def copy(source: IntervalSet[N]): Unit = {
    ranges.clear
    for (entry <- source.ranges.entrySet) {
      ranges.put(entry.getKey, entry.getValue)
    }
  }

  def size = {
    var rc = 0
    var entry = ranges.firstEntry
    while (entry != null) {
      rc += entry.getValue.size.toInt()
      entry = entry.next
    }
    rc
  }

  def toArrayList = {
    new ArrayList(ranges.values)
  }

  override def toString = {
    "[ " + ranges.values().mkString(", ")+" ]"
  }

  def iterator: Iterator[Interval[N]] = {
    return ranges.values.iterator
  }

  def values: List[N] = {
    var rc = new ArrayList[N]
    for (i <- new ValueIterator(iterator)) {
      rc.add(i)
    }
    return rc
  }

  def valueIterator: Iterator[N] = new ValueIterator(iterator)

  def valuesIteratorNotInInterval(r: Interval[N]): Iterator[N] = new ValueIterator(iteratorNotInInterval(r))

  def isEmpty = ranges.isEmpty

  def iteratorNotInInterval(mask: Interval[N]): java.util.Iterator[Interval[N]] = {
    return new Iterator[Interval[N]] {
      private var iter = ranges.values.iterator
      private var last = new Interval(mask.start, mask.start)
      private var _next: Interval[N] = null

      def hasNext: Boolean = {
        while (next==null && last.limit < mask.limit && iter.hasNext) {
          var r = iter.next
          if (r.limit >= last.limit) {
            if (r.start < last.limit) {
              last = new Interval(last.start, r.limit)
            } else {
              if (r.start < mask.limit) {
                _next = new Interval(last.limit, r.start)
              } else {
                _next = new Interval(last.limit, mask.limit)
              }
            }
          }
        }
        return next != null
      }

      def next: Interval[N] = {
        if (!hasNext) {
          throw new NoSuchElementException
        }
        last = next
        _next = null
        return last
      }

      def remove: Unit = {
        throw new UnsupportedOperationException
      }
    }
  }

  private final class ValueIterator(val ranges:Iterator[Interval[N]]) extends java.util.Iterator[N] {

    private var range: Interval[N] = null
    private var _next: Option[N] = None
    private var last: N = zero

    def hasNext: Boolean = {
      if (_next == None) {
        if (Interval == null) {
          if (ranges.hasNext) {
            range = ranges.next
            _next = Some(range.start)
          } else {
            return false
          }
        } else {
          _next = Some(last + one)
        }
        if (_next.get == (range.limit - one)) {
          range = null
        }
      }
      return _next.isDefined
    }

    def next: N = {
      if (!hasNext) {
        throw new NoSuchElementException
      }
      last = _next.get
      _next = None
      return last
    }

    def remove: Unit = throw new UnsupportedOperationException

  }

}