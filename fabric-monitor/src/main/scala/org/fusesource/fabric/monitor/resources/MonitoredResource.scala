package org.fusesource.fabric.monitor.resources

import javax.ws.rs._
import core.MediaType
import org.fusesource.fabric.monitor.internal.DefaultMonitor
import javax.ws.rs._
import core.Response
import Response._
import Response.Status._
import org.rrd4j.ConsolFun
import org.fusesource.fabric.monitor.api.{DataSourceViewDTO, MonitoredViewDTO}
import org.rrd4j.core.{Util, FetchData, FetchRequest, RrdDb}

/**
 * Bundle resource
 */
class MonitoredResource(monitor:DefaultMonitor) {

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML))
  @Path("{id}")
  def get(
    @PathParam("id") id:String,
    @QueryParam("start")start:Long,
    @QueryParam("end")end:Long,
    @QueryParam("step")step:Long):MonitoredViewDTO = {

    println(List(id, start, end, step))

    val monitored_set = monitor.current_monitored_sets.get(id).getOrElse(result(NOT_FOUND))
    val rrd_db = new RrdDb(monitored_set.rrd_file_name, true, monitor.rrd_backend);
    try {

//      if ( filter !=null && !filter.isEmpty ) {
//        request.setFilter(filter:_*)
//      }

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
        rc.start = rc.end - (rc.step*60)
      }
      monitored_set.rrd_archive_funcs.foreach { consol_fun =>
        val request = rrd_db.createFetchRequest(consol_fun, rc.start, rc.end, rc.step)
        val data = request.fetchData()
        for( name <- data.getDsNames ) {
          val t = new DataSourceViewDTO
          t.id = name
          t.consolidation = consol_fun.toString
          // t.label = monitored_set.data_source_dto(name).map(_.name).getOrElse("")
          t.data = data.getValues(name)
          rc.data_sources.add(t)
        }

      }

      rc

    } finally {
      rrd_db.close()
    }
  }

  private def result[T](value:Status, message:Any=null):T = {
    val response = Response.status(value)
    if( message!=null ) {
      response.entity(message)
    }
    throw new WebApplicationException(response.build)
  }

}