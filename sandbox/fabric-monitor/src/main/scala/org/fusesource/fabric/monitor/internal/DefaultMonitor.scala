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

import scala.collection.mutable.HashMap
import org.rrd4j.ConsolFun._
import org.rrd4j.core._
import org.rrd4j.DsType._
import java.util.concurrent.atomic.AtomicBoolean
import org.rrd4j.core.Util
import org.linkedin.util.clock.Timespan
import java.io.File
import java.{util => ju}
import FileSupport._
import collection.JavaConversions._
import org.fusesource.scalate.util.Log
import io.fabric8.monitor.api._
import scala.Some

object DefaultMonitor {
  val log = Log(classOf[DefaultMonitor])
}
import DefaultMonitor._

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DefaultMonitor (
   val rrd_file_prefix:String="",
   val rrd_backend:RrdBackendFactory = new RrdNioBackendFactory()
   ) extends Monitor {

  var step_duration = 1000L

  def path_to_rrd_file(name:String) = rrd_file_prefix + name +".rrd"

  case class MonitoredSet(dto:MonitoredSetDTO) {

    import dto._
    import collection.JavaConversions._

    val sample_span = Timespan.parse(dto.step)

    val sources = {
      var source_counter = 0 ;
      (dto.data_sources.map { source =>

        // rrd ids are limitied to 20 chars...  lets do the best we can:
        var rrd_id = "%x:".format(source_counter)
        // Use the right most part of the id as it's usually the most specific
        rrd_id += source.id.takeRight(20-rrd_id.length)

        source_counter += 1
        (rrd_id, source)
      }).toMap
    }

    def file_base_name: String = {
      rrd_file_prefix + name
    }

    val rrd_file_name = path_to_rrd_file(name)

    val rrd_archive_funcs = dto.archives.map(_.consolidation toUpperCase match {
      case "AVERAGE" => AVERAGE
      case "MIN" => MIN
      case "MAX" => MAX
      case "LAST" => LAST
      case "FIRST" => FIRST
      case "TOTAL" => TOTAL
    }).toSet

    val rrd_def = {
      log.info("Creating RRD file to: " + rrd_file_name)
      val rc = new RrdDef(rrd_file_name, sample_span.getDurationInSeconds)

      sources.foreach { case (rrd_id, source) =>
        import source._
        val steps = Option(heartbeat).map( x =>
          Timespan.parse(x).getDuration(Timespan.TimeUnit.SECOND)
        ).getOrElse(2 * sample_span.getDuration(Timespan.TimeUnit.SECOND))

        rc.addDatasource(rrd_id, kind.toUpperCase match {
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

        val consolFun = consolidation.toUpperCase match {
          case "AVERAGE" => AVERAGE
          case "MIN" => MIN
          case "MAX" => MAX
          case "LAST" => LAST
          case "FIRST" => FIRST
          case "TOTAL" => TOTAL
        }

        rc.addArchive(consolFun, xff, steps, rows);
      }
      rc
    }

    var pollers = List[(String, Poller)]()
    var thread:Thread = _
    var active = new AtomicBoolean()

    def start = {
      if( active.compareAndSet(false, true) ) {

        new File(file_base_name+".json").write_bytes(JsonCodec.encode(dto))

        thread = new Thread("Monitoring: "+name) {
          setDaemon(true)
          override def run: Unit = {

            val sources_by_factory = HashMap[PollerFactory, List[(String, DataSourceDTO)]]()
            sources.foreach { case (rrd_id, source) =>
              poller_factories.find(_.accepts(source)).foreach { factory =>
                val sources = sources_by_factory.getOrElseUpdate(factory, Nil)
                sources_by_factory.put(factory, (rrd_id, source)::sources)
              }
            }

            pollers = {
              sources_by_factory.flatMap{ case (factory, sources)=>
                sources.map{ case (rrd_id, source) => (rrd_id, factory.create(source)) }
              }
            }.toList

            val rrd_db = new RrdDb(rrd_def, rrd_backend);
            try {
              while(active.get) {

                val sample = rrd_db.createSample()
                sample.setTime(Util.getTime())

                val start = System.currentTimeMillis()

//                log.info("Collecting samples from %d pollers.".format(pollers.size))
                pollers.foreach { case (rrd_id, poller) =>
                  val result = poller.poll
                  sample.setValue(rrd_id, result)
                }
//                log.info("Collected sample: "+sample.dump)

                sample.update();

                // Sleep until we need to poll again.
                def remaining = (start + (step_duration * sample_span.getDurationInSeconds)) - System.currentTimeMillis()
                var r = remaining
                while( r > 0 ) {
                  Thread.sleep( r )
                  r = remaining
                }


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

  def configure(value: Traversable[MonitoredSetDTO]): Unit = this.synchronized {

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

  def fetch(fetch: FetchMonitoredViewDTO):Option[MonitoredViewDTO] = this.synchronized {
    val monitored_set_id = fetch.monitored_set
    val ids = fetch.data_sources
    val consolidations = fetch.consolidations
    val start = fetch.start
    val end = fetch.end
    val step = fetch.step

    val monitored_set = current_monitored_sets.get(monitored_set_id) match {
      case Some(x) => x
      case None => return None
    }

    val rrd_db = new RrdDb(monitored_set.rrd_file_name, true, rrd_backend);
    try {

      val rc = new MonitoredViewDTO
      rc.start = start
      rc.end = end
      rc.step = step

      if( rc.step == 0 ) {
        rc.step = 1
      }
      if( rc.end == 0 ) {
        rc.end = Util.getTime-1
      }
      if( rc.start == 0 ) {
        rc.start = rc.end - (rc.step*60*5)
      }

      monitored_set.rrd_archive_funcs.foreach { consol_fun =>
        if( consolidations == null || consolidations.size == 0 || consolidations.contains(consol_fun) ) {
          val request = rrd_db.createFetchRequest(consol_fun, rc.start, rc.end, rc.step)

          if ( ids !=null && !ids.isEmpty ) {
            // Map DS ids to rrd_ids so that we only fetch the requested data...
            val filter: ju.Set[String] = setAsJavaSet(monitored_set.sources.flatMap { case (rrd_id, source) =>
              if (ids.contains(source.id)) {
                Some(rrd_id)
              } else {
                None
              }
            }.toSet)
            request.setFilter(filter)
          }

          val data = request.fetchData();

          for( rrd_id <- data.getDsNames ) {
            val t = new DataSourceViewDTO

            t.id = rrd_id
            t.label = rrd_id
            t.description = ""

            // we can probably get better values from
            // the data source dto
            for( dto <- monitored_set.sources.get(rrd_id) ) {
              t.id = dto.id
              t.label = Option(dto.name)getOrElse(t.id)
              t.description = Option(dto.description).getOrElse("")
            }

            rc.data_sources.add(t)

            t.consolidation = consol_fun.toString
            t.data = data.getValues(rrd_id)
          }

          // lets reorder the data so it matches the order it was
          // requested in..
          if ( ids !=null && !ids.isEmpty ) {
            val sources = rc.data_sources.map( x=> (x.id, x) ).toMap
            rc.data_sources = ids.flatMap(id => sources.get(id))
          }

        }
      }

      Some(rc)

    } finally {
      rrd_db.close()
    }

  }

  def list: Array[MonitoredSetDTO] = this.synchronized {
    current_monitored_sets.values.map(_.dto).toArray
  }
}
