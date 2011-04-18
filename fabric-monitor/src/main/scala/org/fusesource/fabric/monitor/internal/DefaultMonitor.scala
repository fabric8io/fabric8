/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.internal

import scala.collection.mutable.HashMap
import org.rrd4j.ConsolFun._
import org.rrd4j.core._
import org.rrd4j.DsType._
import org.fusesource.fabric.monitor.api._
import java.util.concurrent.atomic.AtomicBoolean
import org.rrd4j.core.Util
import org.linkedin.util.clock.Timespan
import java.util.concurrent.TimeUnit

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DefaultMonitor(

   val rrd_file_prefix:String="",
   val rrd_backend:RrdBackendFactory = new RrdNioBackendFactory()
   ) extends Monitor {

  var step_duration = 1000L

  case class MonitoredSet(dto:MonitoredSetDTO) {

    import dto._
    import collection.JavaConversions._

    val sample_span = Timespan.parse(dto.step)

    def rrd_def = {
      val rc = new RrdDef(rrd_file_prefix+name+".rrd", sample_span.getDurationInSeconds)
      data_sources.foreach { source =>
        import source._

        val steps = Option(heartbeat).map( x =>
          Timespan.parse(x).getDuration(Timespan.TimeUnit.SECOND)
        ).getOrElse(1L)

        rc.addDatasource(source.id, kind.toUpperCase match {
          case "GAUGE"  => GAUGE
          case "COUNTER"  => COUNTER
          case "DERIVE"  => DERIVE
          case "ABSOLUTE"  => ABSOLUTE
        }, steps, min, max);

      }
      archives.foreach { archive =>
        import archive._

        val archive_span = Option(step).map(Timespan.parse(_)).getOrElse(sample_span)
        val steps = (archive_span.getDurationInMilliseconds / sample_span.getDurationInMilliseconds).toInt

        val total_span = Timespan.parse(window)
        val rows = (total_span.getDurationInMilliseconds / archive_span.getDurationInMilliseconds).toInt

        rc.addArchive(consolidation.toUpperCase match {
          case "AVERAGE"  => AVERAGE
          case "MIN"  => MIN
          case "MAX"  => MAX
          case "LAST"  => LAST
          case "FIRST"  => FIRST
          case "TOTAL"  => TOTAL
        }, xff, steps, rows);
      }
      rc
    }

    var pollers = List[Poller]()
    var thread:Thread = _
    var active = new AtomicBoolean()

    def start = {
      if( active.compareAndSet(false, true) ) {

        thread = new Thread("Monitoring: "+name) {
          setDaemon(true)
          override def run: Unit = {
            val rrd_db = new RrdDb(rrd_def, rrd_backend);
            try {

              val sources_by_factory = HashMap[PollerFactory, List[DataSourceDTO]]()
              data_sources.foreach { source =>
                poller_factories.find(_.accepts(source)).foreach { factory =>
                  val sources = sources_by_factory.getOrElseUpdate(factory, Nil)
                  sources_by_factory.put(factory, source::sources)
                }
              }

              pollers = (sources_by_factory.map{case (factory, sources)=> factory.create(sources.toArray) }).toList

              while(active.get) {

                val sample = rrd_db.createSample()
                sample.setTime(Util.getTime)

                println("Collecting samples from %d pollers.".format(pollers.size))
                pollers.foreach { case poller =>
                  val sources = poller.sources
                  val results = poller.poll

                  sources.zip(results).foreach { case (dto, result) =>
                    sample.setValue(dto.id, result);
                  }
                }
                println("Collected sample: "+sample.dump)
                sample.update();

                Thread.sleep( step_duration * sample_span.getDurationInSeconds)

              }


            } finally {
              rrd_db.close
            }
          }
        }
        thread.start()
      }
    }

    def stop = {
      if( active.compareAndSet(true, false) ) {
        thread.join
        thread = null
      }
    }
  }


  val current_monitored_sets = HashMap [String, MonitoredSet]()


  // TODO: if the poller_factories gets changed, we should
  // recreate the current_monitored_sets as there may be more or less
  // data sources that we can gather data for.
  var poller_factories:Seq[PollerFactory] = Nil

  def configure(value: Traversable[MonitoredSetDTO]): Unit = {

    import collection.JavaConversions._
    val next_services = Map[String, MonitoredSet]( value.map { dto=>
      dto.name -> MonitoredSet(dto)
    }.toSeq : _*)


    // Figure out which services are being added, removed, or updated.
    val existing_keys = current_monitored_sets.keys.toSet
    val next_service_keys = next_services.keys.toSet
    val updating = existing_keys.intersect(next_service_keys)
    val adding = next_service_keys -- updating
    val removing = existing_keys -- next_service_keys

    adding.foreach { id =>
      val next = next_services.get(id).get
      next.start
      current_monitored_sets += id -> next
    }

    updating.foreach { id =>
      val next = next_services.get(id).get
      val prev = current_monitored_sets.get(id).get

      // did the service configuration change?
      if( next != prev ) {
        prev.stop
        next.start
        current_monitored_sets.put(id, next)
      }
    }

    removing.foreach{ id =>
      val prev = current_monitored_sets.remove(id).get
      prev.stop
    }

  }

  def close: Unit = {

  }
}